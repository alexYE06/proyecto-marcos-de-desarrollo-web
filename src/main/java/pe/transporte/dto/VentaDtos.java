package pe.transporte.dto;

import java.math.BigDecimal;
import java.util.List;

public final class VentaDtos {

    private VentaDtos() {
    }

    public record PasajeroItemRequest(
            Integer numeroAsiento,
            String tipoDocumento,
            String numeroDocumento,
            String nombre,
            String apellido,
            String correo,
            String telefono
    ) {
    }

    public record ReservaTemporalRequest(
            Integer idViaje,
            List<Integer> asientos
    ) {
    }

    public record EmitirVentaRequest(
            Integer idReservaExistente,
            Integer idViaje,
            String tipoComprobante,
            String rucFactura,
            String razonSocialFactura,
            String compradorNombre,
            String compradorTipoDoc,
            String compradorDocumento,
            String compradorCorreo,
            String compradorTelefono,
            String metodoPago,
            String codigoCupon,
            List<PasajeroItemRequest> pasajeros
    ) {
    }

    public record MotivoAccionRequest(
            String motivo
    ) {
    }

    public record MapaAsientosResponse(
            Integer idViaje,
            String origenCiudad,
            String origenTerminal,
            String destinoCiudad,
            String destinoTerminal,
            String fechaSalida,
            String horaSalida,
            Integer horasEstimadas,
            String servicio,
            String estadoViaje,
            String placaBus,
            String modeloBus,
            Double precioBase,
            BigDecimal tarifaPiso2,
            BigDecimal tarifaPiso1Vip,
            BigDecimal tarifaPiso1Panoramico,
            int capacidadTotal,
            int asientosDisponibles,
            List<Integer> asientosOcupados,
            List<Integer> asientosReservadosPorMi
    ) {
    }

    public record BoletoDetalleResponse(
            Integer idBoleto,
            String codigoBoleto,
            Integer numeroAsiento,
            Integer piso,
            String categoriaAsiento,
            String pasajeroTipoDoc,
            String pasajeroDocumento,
            String pasajeroNombre,
            String pasajeroApellido,
            String pasajeroNombreCompleto,
            String pasajeroCorreo,
            String pasajeroTelefono,
            BigDecimal precio,
            String codigoQr,
            String estadoAbordaje,
            String fechaAbordaje,
            String inspectorNombre
    ) {
    }

    public record VentaDetalleResponse(
            Integer idVenta,
            String codigoReserva,
            String tipoComprobante,
            String serieNumero,
            String rucFactura,
            String razonSocialFactura,
            String canalVenta,
            Integer idViaje,
            String origenCiudad,
            String origenTerminal,
            String destinoCiudad,
            String destinoTerminal,
            String fechaHoraSalida,
            String servicioViaje,
            String placaBus,
            String compradorNombre,
            String compradorTipoDoc,
            String compradorDocumento,
            String compradorCorreo,
            String compradorTelefono,
            String vendedorNombre,
            String metodoPago,
            String codigoCupon,
            BigDecimal subtotal,
            BigDecimal descuento,
            BigDecimal montoTotal,
            String estadoPago,
            String fechaVenta,
            String expiraEn,
            String motivoAnulacion,
            String solicitanteAnulacionNombre,
            String autorizadorAnulacionNombre,
            String fechaAnulacion,
            List<BoletoDetalleResponse> boletos
    ) {
    }
}
