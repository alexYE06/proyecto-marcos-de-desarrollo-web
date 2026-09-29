package pe.transporte.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pe.transporte.model.Usuario;
import pe.transporte.repository.UsuarioRepository;
import pe.transporte.util.EncriptadorPassword;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @PostMapping("/registro")
    @Transactional
    public ResponseEntity<?> registrarUsuario(@RequestBody Usuario usuario) {
        Map<String, String> respuesta = new HashMap<>();

        try {
            // 1. Validar correo duplicado usando existsByCorreo
            if (usuario.getCorreo() != null && usuarioRepository.existsByCorreo(usuario.getCorreo())) {
                respuesta.put("mensaje", "El correo ya se encuentra registrado.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta);
            }

            // 2. Validar documento
            if (usuario.getNumeroDocumento() == null || usuario.getNumeroDocumento().trim().isEmpty()) {
                respuesta.put("mensaje", "El número de documento es obligatorio.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta);
            }

            if (usuarioRepository.existsByNumeroDocumento(usuario.getNumeroDocumento())) {
                respuesta.put("mensaje", "El número de documento ya está registrado.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta);
            }

            // 3. Asignar valores por defecto para PostgreSQL
            if (usuario.getTipoDocumento() == null || usuario.getTipoDocumento().isEmpty()) {
                usuario.setTipoDocumento("DNI");
            }
            if (usuario.getRol() == null || usuario.getRol().isEmpty()) {
                usuario.setRol("CLIENTE");
            }
            usuario.setActivo(true);

            // 4. Encriptar contraseña
            if (usuario.getContrasenaHash() != null && !usuario.getContrasenaHash().isEmpty()) {
                usuario.setContrasenaHash(EncriptadorPassword.encriptar(usuario.getContrasenaHash()));
            }

            // 5. Guardar y confirmar inmediatamente en PostgreSQL
            Usuario guardado = usuarioRepository.saveAndFlush(usuario);

            respuesta.put("mensaje", "Usuario registrado exitosamente en la base de datos.");
            respuesta.put("idUsuario", String.valueOf(guardado.getIdUsuario()));
            return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);

        } catch (Exception e) {
            e.printStackTrace();
            respuesta.put("mensaje", "Error al guardar en base de datos: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(respuesta);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUsuario(@RequestBody Usuario credenciales) {
        Map<String, String> respuesta = new HashMap<>();

        try {
            Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(credenciales.getCorreo());

            if (usuarioOpt.isEmpty()) {
                respuesta.put("mensaje", "El correo ingresado no existe.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(respuesta);
            }

            Usuario usuario = usuarioOpt.get();
            String passHasheada = EncriptadorPassword.encriptar(credenciales.getContrasenaHash());

            if (!usuario.getContrasenaHash().equals(passHasheada)) {
                respuesta.put("mensaje", "Contraseña incorrecta.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(respuesta);
            }

            respuesta.put("mensaje", "Inicio de sesión exitoso");
            respuesta.put("nombre", usuario.getNombre() + " " + usuario.getApellido());
            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            e.printStackTrace();
            respuesta.put("mensaje", "Error al procesar el ingreso: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(respuesta);
        }
    }
}