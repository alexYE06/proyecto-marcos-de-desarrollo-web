package pe.transporte.dto;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            String correo,
            String contrasena
    ) {
    }

    public record RegistroClienteRequest(
            String nombre,
            String apellido,
            String tipoDocumento,
            String numeroDocumento,
            String correo,
            String telefono,
            String contrasena
    ) {
    }

    public record ActualizarPerfilRequest(
            String nombre,
            String apellido,
            String telefono,
            String contrasenaActual,
            String contrasenaNueva
    ) {
    }

    public record AuthResponse(
            String mensaje,
            String token,
            Integer idUsuario,
            String nombre,
            String apellido,
            String nombreCompleto,
            String tipoDocumento,
            String numeroDocumento,
            String correo,
            String telefono,
            String rol,
            String panelUrl
    ) {
    }
}
