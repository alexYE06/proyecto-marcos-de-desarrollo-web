package pe.transporte.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.transporte.dto.AuthDtos.*;
import pe.transporte.exception.NoAutorizadoException;
import pe.transporte.exception.RecursoNoEncontradoException;
import pe.transporte.exception.ReglaNegocioException;
import pe.transporte.model.Usuario;
import pe.transporte.model.enums.RolUsuario;
import pe.transporte.repository.UsuarioRepository;
import pe.transporte.security.SesionUsuario;
import pe.transporte.security.SessionTokenStore;
import pe.transporte.util.EncriptadorPassword;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final SessionTokenStore tokenStore;

    public AuthService(UsuarioRepository usuarioRepository, SessionTokenStore tokenStore) {
        this.usuarioRepository = usuarioRepository;
        this.tokenStore = tokenStore;
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        String mensajeGenerico = "Correo electrónico o contraseña incorrectos.";
        if (req == null || req.correo() == null || req.contrasena() == null
                || req.correo().isBlank() || req.contrasena().isBlank()) {
            throw new NoAutorizadoException(mensajeGenerico);
        }

        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(req.correo().trim())
                .orElseThrow(() -> new NoAutorizadoException(mensajeGenerico));

        if (!EncriptadorPassword.verificar(req.contrasena(), usuario.getContrasenaHash())) {
            throw new NoAutorizadoException(mensajeGenerico);
        }

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new NoAutorizadoException("Tu cuenta está desactivada. Contacta al administrador.");
        }

        // Migración transparente de SHA-256 antiguo a PBKDF2 con salt
        if (EncriptadorPassword.requiereActualizacion(usuario.getContrasenaHash())) {
            usuario.setContrasenaHash(EncriptadorPassword.encriptar(req.contrasena()));
            usuarioRepository.save(usuario);
        }

        SesionUsuario sesion = tokenStore.crearSesion(usuario);
        return mapearRespuesta("Inicio de sesión exitoso.", usuario, sesion.token());
    }

    @Transactional
    public AuthResponse registrarCliente(RegistroClienteRequest req) {
        if (req == null) {
            throw new ReglaNegocioException("Los datos de registro son obligatorios.");
        }
        String nombre = limpiar(req.nombre(), "El nombre es obligatorio.");
        String apellido = limpiar(req.apellido(), "El apellido es obligatorio.");
        String documento = limpiar(req.numeroDocumento(), "El número de documento es obligatorio.");
        String correo = limpiar(req.correo(), "El correo electrónico es obligatorio.").toLowerCase();
        String tipoDoc = (req.tipoDocumento() == null || req.tipoDocumento().isBlank())
                ? "DNI" : req.tipoDocumento().trim().toUpperCase();

        if (req.contrasena() == null || req.contrasena().length() < 6) {
            throw new ReglaNegocioException("La contraseña debe tener al menos 6 caracteres.");
        }
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ReglaNegocioException("El correo ya se encuentra registrado.");
        }
        if (usuarioRepository.existsByNumeroDocumento(documento)) {
            throw new ReglaNegocioException("El número de documento ya está registrado.");
        }

        Usuario nuevo = new Usuario();
        nuevo.setNombre(nombre);
        nuevo.setApellido(apellido);
        nuevo.setTipoDocumento(tipoDoc);
        nuevo.setNumeroDocumento(documento);
        nuevo.setCorreo(correo);
        nuevo.setTelefono(req.telefono() != null ? req.telefono().trim() : null);
        nuevo.setContrasenaHash(EncriptadorPassword.encriptar(req.contrasena()));
        nuevo.setRol(RolUsuario.CLIENTE.name());
        nuevo.setActivo(true);

        Usuario guardado = usuarioRepository.saveAndFlush(nuevo);
        SesionUsuario sesion = tokenStore.crearSesion(guardado);
        return mapearRespuesta("Cuenta creada exitosamente.", guardado, sesion.token());
    }

    public AuthResponse obtenerPerfilActual(SesionUsuario sesion) {
        Usuario u = usuarioRepository.findById(sesion.idUsuario())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));
        if (!Boolean.TRUE.equals(u.getActivo())) {
            tokenStore.revocarSesionesDeUsuario(u.getIdUsuario());
            throw new NoAutorizadoException("Tu cuenta fue desactivada.");
        }
        return mapearRespuesta("Sesión activa", u, sesion.token());
    }

    @Transactional
    public AuthResponse actualizarPerfil(SesionUsuario sesion, ActualizarPerfilRequest req) {
        Usuario u = usuarioRepository.findById(sesion.idUsuario())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));

        if (req.nombre() != null && !req.nombre().isBlank()) {
            u.setNombre(req.nombre().trim());
        }
        if (req.apellido() != null && !req.apellido().isBlank()) {
            u.setApellido(req.apellido().trim());
        }
        if (req.telefono() != null) {
            u.setTelefono(req.telefono().trim());
        }

        if (req.contrasenaNueva() != null && !req.contrasenaNueva().isBlank()) {
            if (req.contrasenaActual() == null || !EncriptadorPassword.verificar(req.contrasenaActual(), u.getContrasenaHash())) {
                throw new ReglaNegocioException("La contraseña actual ingresada no es correcta.");
            }
            if (req.contrasenaNueva().length() < 6) {
                throw new ReglaNegocioException("La nueva contraseña debe tener al menos 6 caracteres.");
            }
            u.setContrasenaHash(EncriptadorPassword.encriptar(req.contrasenaNueva()));
        }

        usuarioRepository.save(u);
        return mapearRespuesta("Perfil actualizado correctamente.", u, sesion.token());
    }

    public void logout(String token) {
        tokenStore.revocarToken(token);
    }

    public static String resolverPanelPorRol(RolUsuario rol) {
        return switch (rol) {
            case ADMIN -> "admin-panel.html";
            case VENDEDOR -> "vendedor-pos.html";
            case EMBARQUE -> "embarque.html";
            case CLIENTE -> "index.html";
        };
    }

    private AuthResponse mapearRespuesta(String mensaje, Usuario u, String token) {
        RolUsuario rol = RolUsuario.desdeTexto(u.getRol());
        return new AuthResponse(
                mensaje,
                token,
                u.getIdUsuario(),
                u.getNombre(),
                u.getApellido(),
                u.getNombreCompleto(),
                u.getTipoDocumento(),
                u.getNumeroDocumento(),
                u.getCorreo(),
                u.getTelefono(),
                rol.name(),
                resolverPanelPorRol(rol)
        );
    }

    private String limpiar(String valor, String mensajeError) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new ReglaNegocioException(mensajeError);
        }
        return valor.trim();
    }
}
