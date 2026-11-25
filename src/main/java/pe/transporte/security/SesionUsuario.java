package pe.transporte.security;

import pe.transporte.model.enums.RolUsuario;
import java.time.Instant;

public record SesionUsuario(
        String token,
        Integer idUsuario,
        String nombreCompleto,
        String correo,
        RolUsuario rol,
        Instant expiraEn
) {
}
