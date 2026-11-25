package pe.transporte.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class AdminOperacionDtos {

    private AdminOperacionDtos() {
    }

    public record CrearUsuarioInternoRequest(
            String nombre,
            String apellido,
            String tipoDocumento,
            String numeroDocumento,
            String correo,
            String telefono,
            String contrasena,
            String rol
    ) {
    }

    public record ActualizarUsuarioAdminRequest(
            String nombre,
            String apellido,
            String telefono,
            String rol,
            Boolean activo,
            String nuevaContrasena
    ) {
    }

    public record TerminalRequest(
            String nombre,
            String ciudad,
            String direccion,
            Boolean activo
    ) {
    }

    public record RutaRequest(
            Integer idTerminalOrigen,
            Integer idTerminalDestino,
            Double precioBase,
            Integer horasEstimadas,
            Integer distanciaKm,
            Boolean activo
    ) {
    }

    public record BusRequest(
            String placa,
            String numeroInterno,
            String marca,
            String modelo,
            Integer pisos,
            Integer capacidadAsientos,
            String estado
    ) {
    }

    public record EmpleadoRequest(
            String dniEmpleado,
            String nombre,
            String apellido,
            String cargo,
            String licencia,
            String telefono,
            BigDecimal sueldo,
            Boolean activo
    ) {
    }

    public record ViajeRequest(
            Integer idRuta,
            Integer idBus,
            Integer idConductor,
            Integer idCopiloto,
            String fechaHoraSalida,
            Double precio,
            String servicio,
            String estado
    ) {
    }

    public record CuponRequest(
            String codigo,
            String descripcion,
            BigDecimal montoDescuento,
            Integer usosMaximos,
            LocalDate fechaExpiracion,
            Boolean activo
    ) {
    }

    public record ValidarAbordajeRequest(
            Integer idViaje,
            String codigoEntrada,
            String documentoPasajero
    ) {
    }

    public record CambiarEstadoViajeRequest(
            String estado
    ) {
    }
}
