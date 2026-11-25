package pe.transporte.model.enums;

public enum RolUsuario {
    ADMIN,
    VENDEDOR,
    EMBARQUE,
    CLIENTE;

    public static RolUsuario desdeTexto(String valor) {
        if (valor == null || valor.isBlank()) {
            return CLIENTE;
        }
        return RolUsuario.valueOf(valor.trim().toUpperCase());
    }
}
