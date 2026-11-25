package pe.transporte.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.transporte.dto.AuthDtos.*;
import pe.transporte.model.enums.RolUsuario;
import pe.transporte.security.AuthInterceptor;
import pe.transporte.security.RequiereRol;
import pe.transporte.security.SesionUsuario;
import pe.transporte.service.AuthService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registrarCliente(@RequestBody RegistroClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrarCliente(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        String token = AuthInterceptor.extraerToken(request);
        authService.logout(token);
        return ResponseEntity.ok(Map.of("mensaje", "Sesión cerrada correctamente."));
    }

    @GetMapping("/me")
    @RequiereRol({RolUsuario.ADMIN, RolUsuario.VENDEDOR, RolUsuario.EMBARQUE, RolUsuario.CLIENTE})
    public ResponseEntity<AuthResponse> obtenerSesionActual(HttpServletRequest request) {
        SesionUsuario sesion = AuthInterceptor.sesionObligatoria(request);
        return ResponseEntity.ok(authService.obtenerPerfilActual(sesion));
    }

    @PutMapping("/perfil")
    @RequiereRol({RolUsuario.ADMIN, RolUsuario.VENDEDOR, RolUsuario.EMBARQUE, RolUsuario.CLIENTE})
    public ResponseEntity<AuthResponse> actualizarPerfil(HttpServletRequest request,
                                                         @RequestBody ActualizarPerfilRequest body) {
        SesionUsuario sesion = AuthInterceptor.sesionObligatoria(request);
        return ResponseEntity.ok(authService.actualizarPerfil(sesion, body));
    }
}
