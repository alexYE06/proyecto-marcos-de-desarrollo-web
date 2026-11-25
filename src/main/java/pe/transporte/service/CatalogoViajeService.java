package pe.transporte.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.transporte.dto.VentaDtos.MapaAsientosResponse;
import pe.transporte.exception.RecursoNoEncontradoException;
import pe.transporte.model.*;
import pe.transporte.model.enums.EstadoVenta;
import pe.transporte.model.enums.EstadoViaje;
import pe.transporte.repository.*;
import pe.transporte.util.TarifasAsientos;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CatalogoViajeService {

    private final TerminalRepository terminalRepository;
    private final RutaRepository rutaRepository;
    private final ViajeRepository viajeRepository;
    private final VentaRepository ventaRepository;
    private final BoletoRepository boletoRepository;

    public CatalogoViajeService(TerminalRepository terminalRepository,
                                RutaRepository rutaRepository,
                                ViajeRepository viajeRepository,
                                VentaRepository ventaRepository,
                                BoletoRepository boletoRepository) {
        this.terminalRepository = terminalRepository;
        this.rutaRepository = rutaRepository;
        this.viajeRepository = viajeRepository;
        this.ventaRepository = ventaRepository;
        this.boletoRepository = boletoRepository;
    }

    public List<Terminal> listarTerminalesActivos() {
        return terminalRepository.findAllByOrderByCiudadAscNombreAsc().stream()
                .filter(t -> Boolean.TRUE.equals(t.getActivo()))
                .toList();
    }

    public List<Ruta> listarRutasActivas() {
        return rutaRepository.findAllByOrderByIdRutaAsc().stream()
                .filter(r -> Boolean.TRUE.equals(r.getActivo()))
                .toList();
    }

    @Transactional
    public List<Viaje> buscarViajes(String origen, String destino, String fechaTexto, boolean incluirPasadosHoy) {
        expirarReservasVencidas();

        LocalDate fechaFiltro = null;
        if (fechaTexto != null && !fechaTexto.isBlank()) {
            try {
                fechaFiltro = LocalDate.parse(fechaTexto.trim());
            } catch (Exception ignored) {
            }
        }

        final LocalDate fechaBuscada = fechaFiltro;
        final String origNorm = normalizar(origen);
        final String destNorm = normalizar(destino);

        List<Viaje> todos = viajeRepository.findAllByOrderByFechaHoraSalidaAsc();
        List<Viaje> resultado = todos.stream()
                .filter(v -> !EstadoViaje.CANCELADO.name().equalsIgnoreCase(v.getEstado()))
                .filter(v -> !EstadoViaje.FINALIZADO.name().equalsIgnoreCase(v.getEstado()))
                .filter(v -> {
                    if (v.getRuta() == null || v.getRuta().getOrigen() == null || v.getRuta().getDestino() == null) {
                        return false;
                    }
                    boolean okOrigen = origNorm.isEmpty()
                            || normalizar(v.getRuta().getOrigen().getCiudad()).contains(origNorm)
                            || normalizar(v.getRuta().getOrigen().getNombre()).contains(origNorm);
                    boolean okDestino = destNorm.isEmpty()
                            || normalizar(v.getRuta().getDestino().getCiudad()).contains(destNorm)
                            || normalizar(v.getRuta().getDestino().getNombre()).contains(destNorm);
                    boolean okFecha = fechaBuscada == null
                            || (v.getFechaHoraSalida() != null && v.getFechaHoraSalida().toLocalDate().equals(fechaBuscada));
                    if (!incluirPasadosHoy && fechaBuscada == null && v.getFechaHoraSalida() != null) {
                        return okOrigen && okDestino && v.getFechaHoraSalida().isAfter(LocalDateTime.now().minusHours(2));
                    }
                    return okOrigen && okDestino && okFecha;
                })
                .toList();

        // Sincronizar contador real de asientos disponibles en cada viaje devuelto
        for (Viaje v : resultado) {
            sincronizarDisponibilidadViaje(v);
        }
        return resultado;
    }

    @Transactional
    public MapaAsientosResponse obtenerMapaAsientos(Integer idViaje, Integer idUsuarioActual) {
        expirarReservasVencidas();
        Viaje viaje = viajeRepository.findById(idViaje)
                .orElseThrow(() -> new RecursoNoEncontradoException("El viaje solicitado no existe."));

        Set<Integer> ocupadosOtros = new TreeSet<>();
        Set<Integer> reservadosPorMi = new TreeSet<>();

        List<Boleto> boletos = boletoRepository.findByViaje_IdViajeOrderByNumeroAsientoAsc(idViaje);
        for (Boleto b : boletos) {
            Venta venta = b.getVenta();
            if (venta == null) continue;
            String estado = venta.getEstadoPago();
            if (EstadoVenta.ANULADO.name().equalsIgnoreCase(estado)
                    || EstadoVenta.EXPIRADO.name().equalsIgnoreCase(estado)) {
                continue;
            }
            if (EstadoVenta.RESERVADO.name().equalsIgnoreCase(estado)
                    && idUsuarioActual != null
                    && venta.getCliente() != null
                    && idUsuarioActual.equals(venta.getCliente().getIdUsuario())) {
                reservadosPorMi.add(b.getNumeroAsiento());
            } else {
                ocupadosOtros.add(b.getNumeroAsiento());
            }
        }

        int capacidad = viaje.getBus() != null && viaje.getBus().getCapacidadAsientos() != null
                ? viaje.getBus().getCapacidadAsientos() : TarifasAsientos.TOTAL_ASIENTOS;
        int disponibles = Math.max(0, capacidad - ocupadosOtros.size() - reservadosPorMi.size());
        viaje.setAsientosDisponibles(disponibles);
        viajeRepository.save(viaje);

        Ruta r = viaje.getRuta();
        Double precioBase = viaje.getPrecio() != null ? viaje.getPrecio() : TarifasAsientos.PRECIO_BASE_DEFECTO;

        return new MapaAsientosResponse(
                viaje.getIdViaje(),
                r != null && r.getOrigen() != null ? r.getOrigen().getCiudad() : "-",
                r != null && r.getOrigen() != null ? r.getOrigen().getNombre() : "-",
                r != null && r.getDestino() != null ? r.getDestino().getCiudad() : "-",
                r != null && r.getDestino() != null ? r.getDestino().getNombre() : "-",
                viaje.getFechaHoraSalida() != null ? viaje.getFechaHoraSalida().toLocalDate().toString() : "-",
                viaje.getFechaHoraSalida() != null ? viaje.getFechaHoraSalida().format(DateTimeFormatter.ofPattern("HH:mm")) : "-",
                r != null ? r.getHorasEstimadas() : null,
                viaje.getServicio(),
                viaje.getEstado(),
                viaje.getBus() != null ? viaje.getBus().getPlaca() : "Por asignar",
                viaje.getBus() != null ? viaje.getBus().getModelo() : "Doble Piso 64 asientos",
                precioBase,
                TarifasAsientos.precio(25, precioBase),
                TarifasAsientos.precio(10, precioBase),
                TarifasAsientos.precio(1, precioBase),
                capacidad,
                disponibles,
                new ArrayList<>(ocupadosOtros),
                new ArrayList<>(reservadosPorMi)
        );
    }

    @Transactional
    public void expirarReservasVencidas() {
        List<Venta> vencidas = ventaRepository.findByEstadoPagoAndExpiraEnBefore(
                EstadoVenta.RESERVADO.name(), LocalDateTime.now());
        for (Venta v : vencidas) {
            v.setEstadoPago(EstadoVenta.EXPIRADO.name());
            ventaRepository.save(v);
            if (v.getViaje() != null) {
                sincronizarDisponibilidadViaje(v.getViaje());
            }
        }
    }

    @Transactional
    public void sincronizarDisponibilidadViaje(Viaje viaje) {
        if (viaje == null || viaje.getIdViaje() == null) return;
        long ocupados = boletoRepository.findByViaje_IdViajeOrderByNumeroAsientoAsc(viaje.getIdViaje()).stream()
                .filter(b -> b.getVenta() != null)
                .filter(b -> {
                    String est = b.getVenta().getEstadoPago();
                    return EstadoVenta.PAGADO.name().equalsIgnoreCase(est)
                            || EstadoVenta.RESERVADO.name().equalsIgnoreCase(est)
                            || EstadoVenta.ANULACION_SOLICITADA.name().equalsIgnoreCase(est);
                })
                .count();
        int capacidad = viaje.getBus() != null && viaje.getBus().getCapacidadAsientos() != null
                ? viaje.getBus().getCapacidadAsientos() : TarifasAsientos.TOTAL_ASIENTOS;
        int disp = Math.max(0, capacidad - (int) ocupados);
        if (!Objects.equals(viaje.getAsientosDisponibles(), disp)) {
            viaje.setAsientosDisponibles(disp);
            viajeRepository.save(viaje);
        }
    }

    private String normalizar(String texto) {
        if (texto == null) return "";
        String nfd = Normalizer.normalize(texto.trim().toLowerCase(), Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
