package pe.transporte.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.transporte.model.Usuario;
import pe.transporte.repository.UsuarioRepository;
import pe.transporte.util.AdminTokenStore;
import pe.transporte.util.EncriptadorPassword;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/auth")
@CrossOrigin(origins = "*")
public class AdminAuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AdminTokenStore tokenStore;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credenciales) {
        Map<String, String> respuesta = new HashMap<>();
        String correo = credenciales.get("correo");
        String contrasena = credenciales.get("contrasena");

        // Mensaje único para cualquier fallo: no revela si el correo existe o no es admin
        String mensajeError = "Credenciales de administrador incorrectas.";

        if (correo == null || contrasena == null) {
            respuesta.put("mensaje", mensajeError);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(respuesta);
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(correo.trim());
        if (usuarioOpt.isEmpty()) {
            respuesta.put("mensaje", mensajeError);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(respuesta);
        }

        Usuario usuario = usuarioOpt.get();
        String hashIngresado = EncriptadorPassword.encriptar(contrasena);
        boolean passwordOk = usuario.getContrasenaHash() != null && MessageDigest.isEqual(
                usuario.getContrasenaHash().getBytes(StandardCharsets.UTF_8),
                hashIngresado.getBytes(StandardCharsets.UTF_8));
        boolean esAdmin = "ADMIN".equalsIgnoreCase(usuario.getRol());
        boolean activo = Boolean.TRUE.equals(usuario.getActivo());

        if (!passwordOk || !esAdmin || !activo) {
            respuesta.put("mensaje", mensajeError);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(respuesta);
        }

        respuesta.put("mensaje", "Acceso de administrador concedido");
        respuesta.put("nombre", usuario.getNombre() + " " + usuario.getApellido());
        respuesta.put("token", tokenStore.crear());
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        tokenStore.revocar(token);
        Map<String, String> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Sesión cerrada");
        return ResponseEntity.ok(respuesta);
    }
}
