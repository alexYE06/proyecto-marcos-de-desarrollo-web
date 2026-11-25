package pe.transporte.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.transporte.dto.AdminOperacionDtos.ValidarAbordajeRequest;
import pe.transporte.dto.VentaDtos.BoletoDetalleResponse;
import pe.transporte.exception.RecursoNoEncontradoException;
import pe.transporte.exception.ReglaNegocioException;
import pe.transporte.model.*;
import pe.transporte.model.enums.EstadoAbordaje;
import pe.transporte.model.enums.EstadoVenta;
import pe.transporte.model.enums.EstadoViaje;
import pe.transporte.repository.*;
import pe.transporte.security.SesionUsuario;
import pe.transporte.util.GeneradorQR;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class EmbarqueService {

    private static final DateTimeFormatter FMT_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ViajeRepository viajeRepository;
    private final BoletoRepository boletoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CatalogoViajeService catalogoViajeService;
    private final AuditoriaService auditoriaService;

    public EmbarqueService(ViajeRepository viajeRepository,
                           BoletoRepository boletoRepository,
                           UsuarioRepository usuarioRepository,
                           CatalogoViajeService catalogoViajeService,
                           AuditoriaService auditoriaService) {
        this.viajeRepository = viajeRepository;
        this.boletoRepository = boletoRepository;
        this.usuarioRepository = usuarioRepository;
        this.catalogoViajeService = catalogoViajeService;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public List<Map<String, Object>> listarViajesOperativos(String fechaOpcional) {
        catalogoViajeService.expirarReservasVencidas();
        LocalDate filtro = null;
        if (fechaOpcional != null && !fechaOpcional.isBlank()) {
            try {
                filtro = LocalDate.parse(fechaOpcional.trim());
            } catch (Exception ignored) {
            }
        }
        final LocalDate fechaBuscada = filtro;

        return viajeRepository.findAllByOrderByFechaHoraSalidaAsc().stream()
                .filter(v -> !EstadoViaje.CANCELADO.name().equalsIgnoreCase(v.getEstado()))
                .filter(v -> fechaBuscada == null
                        || (v.getFechaHoraSalida() != null && v.getFechaHoraSalida().toLocalDate().equals(fechaBuscada)))
                .map(this::resumirViajeOperativo)
                .toList();
    }

    @Transactional
    public Map<String, Object> obtenerManifiestoViaje(Integer idViaje) {
        catalogoViajeService.expirarReservasVencidas();
        Viaje viaje = viajeRepository.findById(idViaje)
                .orElseThrow(() -> new RecursoNoEncontradoException("El viaje no existe."));

        List<Boleto> boletosValidos = obtenerBoletosPagadosDelViaje(idViaje);
        long abordados = boletosValidos.stream()
                .filter(b -> EstadoAbordaje.ABORDADO.name().equalsIgnoreCase(b.getEstadoAbordaje()))
                .count();

        List<BoletoDetalleResponse> pasajeros = boletosValidos.stream()
                .map(this::mapearBoleto)
                .toList();

        Map<String, Object> resp = new LinkedHashMap<>(resumirViajeOperativo(viaje));
        resp.put("totalPasajeros", pasajeros.size());
        resp.put("totalAbordados", abordados);
        resp.put("pendientesAbordar", pasajeros.size() - abordados);
        resp.put("pasajeros", pasajeros);
        return resp;
    }

    @Transactional
    public Map<String, Object> validarYRegistrarAbordaje(ValidarAbordajeRequest req, SesionUsuario sesionInspector) {
        if (req == null) {
            throw new ReglaNegocioException("Ingresa el código de boleto, texto QR o documento del pasajero.");
        }

        Boleto boleto = localizarBoleto(req);
        Venta venta = boleto.getVenta();
        if (venta == null || !EstadoVenta.PAGADO.name().equalsIgnoreCase(venta.getEstadoPago())) {
            String est = venta != null ? venta.getEstadoPago() : "DESCONOCIDO";
            throw new ReglaNegocioException("ALERTA: Este boleto NO es válido para abordar (Estado de venta: " + est + ").");
        }

        if (req.idViaje() != null && !req.idViaje().equals(boleto.getViaje().getIdViaje())) {
            throw new ReglaNegocioException("Este boleto pertenece a otro viaje (#" + boleto.getViaje().getIdViaje() + "), no al bus actual.");
        }

        Viaje viaje = boleto.getViaje();
        if (EstadoViaje.CANCELADO.name().equalsIgnoreCase(viaje.getEstado())
                || EstadoViaje.FINALIZADO.name().equalsIgnoreCase(viaje.getEstado())) {
            throw new ReglaNegocioException("El viaje asociado se encuentra " + viaje.getEstado() + ".");
        }

        if (EstadoAbordaje.ABORDADO.name().equalsIgnoreCase(boleto.getEstadoAbordaje())) {
            String hora = boleto.getFechaAbordaje() != null ? boleto.getFechaAbordaje().format(FMT_FECHA_HORA) : "previamente";
            String insp = boleto.getInspectorAbordaje() != null ? boleto.getInspectorAbordaje().getNombreCompleto() : "Inspector";
            throw new ReglaNegocioException("BOLETO YA UTILIZADO: el pasajero ya abordó (" + hora + " · " + insp + ").");
        }

        Usuario inspector = usuarioRepository.findById(sesionInspector.idUsuario())
                .orElseThrow(() -> new RecursoNoEncontradoException("Inspector no encontrado."));

        boleto.setEstadoAbordaje(EstadoAbordaje.ABORDADO.name());
        boleto.setFechaAbordaje(LocalDateTime.now());
        boleto.setInspectorAbordaje(inspector);
        Boleto actualizado = boletoRepository.saveAndFlush(boleto);

        if (auditoriaService != null) {
            auditoriaService.registrar(
                    sesionInspector,
                    "EMBARQUE",
                    "ABORDAJE_QR",
                    "Boleto",
                    actualizado.getCodigoBoleto(),
                    "Validó abordaje de " + actualizado.getPasajeroNombreCompleto()
                            + " (Asiento " + actualizado.getNumeroAsiento() + ", Viaje #" + viaje.getIdViaje() + ").",
                    null
            );
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("mensaje", "Abordaje verificado y registrado exitosamente.");
        resp.put("boleto", mapearBoleto(actualizado));
        resp.put("origen", viaje.getRuta().getOrigen().getCiudad());
        resp.put("destino", viaje.getRuta().getDestino().getCiudad());
        resp.put("placaBus", viaje.getBus() != null ? viaje.getBus().getPlaca() : "-");
        return resp;
    }

    @Transactional
    public Map<String, Object> cambiarEstadoOperativoViaje(Integer idViaje, String nuevoEstadoTexto, SesionUsuario sesionActor) {
        Viaje viaje = viajeRepository.findById(idViaje)
                .orElseThrow(() -> new RecursoNoEncontradoException("El viaje no existe."));

        EstadoViaje nuevoEstado;
        try {
            nuevoEstado = EstadoViaje.valueOf(nuevoEstadoTexto.trim().toUpperCase());
        } catch (Exception e) {
            throw new ReglaNegocioException("Estado de viaje no válido.");
        }

        if (nuevoEstado == EstadoViaje.CANCELADO) {
            throw new ReglaNegocioException("Solo Administración General puede cancelar una salida programada.");
        }

        String anterior = viaje.getEstado();
        viaje.setEstado(nuevoEstado.name());
        viajeRepository.saveAndFlush(viaje);
        if (auditoriaService != null) {
            auditoriaService.registrar(
                    sesionActor,
                    "EMBARQUE",
                    "CAMBIO_ESTADO_VIAJE",
                    "Viaje",
                    idViaje,
                    "Cambió estado del viaje #" + idViaje + " de " + anterior + " a " + nuevoEstado.name() + ".",
                    null
            );
        }
        return resumirViajeOperativo(viaje);
    }

    private Boleto localizarBoleto(ValidarAbordajeRequest req) {
        if (req.codigoEntrada() != null && !req.codigoEntrada().isBlank()) {
            String codigoExtraido = GeneradorQR.extraerCodigoBoleto(req.codigoEntrada());
            return boletoRepository.findByCodigoBoletoIgnoreCase(codigoExtraido)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe ningún boleto con el código: " + codigoExtraido));
        }
        if (req.documentoPasajero() != null && !req.documentoPasajero().isBlank() && req.idViaje() != null) {
            List<Boleto> coincidencias = boletoRepository.findByViaje_IdViajeAndPasajeroDocumento(
                            req.idViaje(), req.documentoPasajero().trim()).stream()
                    .filter(b -> b.getVenta() != null && EstadoVenta.PAGADO.name().equalsIgnoreCase(b.getVenta().getEstadoPago()))
                    .toList();
            if (coincidencias.isEmpty()) {
                throw new RecursoNoEncontradoException("No se encontró boleto pagado para el documento " + req.documentoPasajero() + " en este viaje.");
            }
            // Priorizar el que aún esté pendiente de abordar
            return coincidencias.stream()
                    .filter(b -> EstadoAbordaje.PENDIENTE.name().equalsIgnoreCase(b.getEstadoAbordaje()))
                    .findFirst()
                    .orElse(coincidencias.get(0));
        }
        throw new ReglaNegocioException("Proporciona el código QR/Boleto o selecciona un viaje e ingresa el DNI del pasajero.");
    }

    private List<Boleto> obtenerBoletosPagadosDelViaje(Integer idViaje) {
        return boletoRepository.findByViaje_IdViajeOrderByNumeroAsientoAsc(idViaje).stream()
                .filter(b -> b.getVenta() != null)
                .filter(b -> EstadoVenta.PAGADO.name().equalsIgnoreCase(b.getVenta().getEstadoPago())
                        || EstadoVenta.ANULACION_SOLICITADA.name().equalsIgnoreCase(b.getVenta().getEstadoPago()))
                .toList();
    }

    private Map<String, Object> resumirViajeOperativo(Viaje v) {
        List<Boleto> pagados = obtenerBoletosPagadosDelViaje(v.getIdViaje());
        long abordados = pagados.stream()
                .filter(b -> EstadoAbordaje.ABORDADO.name().equalsIgnoreCase(b.getEstadoAbordaje()))
                .count();

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("idViaje", v.getIdViaje());
        item.put("origen", v.getRuta() != null && v.getRuta().getOrigen() != null ? v.getRuta().getOrigen().getCiudad() : "-");
        item.put("terminalOrigen", v.getRuta() != null && v.getRuta().getOrigen() != null ? v.getRuta().getOrigen().getNombre() : "-");
        item.put("destino", v.getRuta() != null && v.getRuta().getDestino() != null ? v.getRuta().getDestino().getCiudad() : "-");
        item.put("terminalDestino", v.getRuta() != null && v.getRuta().getDestino() != null ? v.getRuta().getDestino().getNombre() : "-");
        item.put("fechaSalida", v.getFechaHoraSalida() != null ? v.getFechaHoraSalida().toLocalDate().toString() : "-");
        item.put("horaSalida", v.getFechaHoraSalida() != null ? v.getFechaHoraSalida().format(DateTimeFormatter.ofPattern("HH:mm")) : "-");
        item.put("fechaHoraFormateada", v.getFechaHoraSalida() != null ? v.getFechaHoraSalida().format(FMT_FECHA_HORA) : "-");
        item.put("servicio", v.getServicio());
        item.put("estado", v.getEstado());
        item.put("placaBus", v.getBus() != null ? v.getBus().getPlaca() : "Por asignar");
        item.put("modeloBus", v.getBus() != null ? v.getBus().getModelo() : "-");
        item.put("conductor", v.getConductor() != null ? v.getConductor().getNombreCompleto() : "Por asignar");
        item.put("copiloto", v.getCopiloto() != null ? v.getCopiloto().getNombreCompleto() : "-");
        item.put("capacidad", v.getBus() != null && v.getBus().getCapacidadAsientos() != null ? v.getBus().getCapacidadAsientos() : 64);
        item.put("vendidos", pagados.size());
        item.put("abordados", abordados);
        return item;
    }

    private BoletoDetalleResponse mapearBoleto(Boleto b) {
        return new BoletoDetalleResponse(
                b.getIdBoleto(),
                b.getCodigoBoleto(),
                b.getNumeroAsiento(),
                b.getPiso(),
                b.getCategoriaAsiento(),
                b.getPasajeroTipoDoc(),
                b.getPasajeroDocumento(),
                b.getPasajeroNombre(),
                b.getPasajeroApellido(),
                b.getPasajeroNombreCompleto(),
                b.getPasajeroCorreo(),
                b.getPasajeroTelefono(),
                b.getPrecio(),
                b.getCodigoQr(),
                b.getEstadoAbordaje(),
                b.getFechaAbordaje() != null ? b.getFechaAbordaje().format(FMT_FECHA_HORA) : null,
                b.getInspectorAbordaje() != null ? b.getInspectorAbordaje().getNombreCompleto() : null
        );
    }
}
