package pe.transporte.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.transporte.dto.VentaDtos.*;
import pe.transporte.exception.AccesoDenegadoException;
import pe.transporte.exception.RecursoNoEncontradoException;
import pe.transporte.exception.ReglaNegocioException;
import pe.transporte.model.*;
import pe.transporte.model.enums.*;
import pe.transporte.repository.*;
import pe.transporte.security.SesionUsuario;
import pe.transporte.util.GeneradorQR;
import pe.transporte.util.TarifasAsientos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class VentaPasajeService {

    private static final int MINUTOS_RESERVA_WEB = 15;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter FMT_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final VentaRepository ventaRepository;
    private final BoletoRepository boletoRepository;
    private final ViajeRepository viajeRepository;
    private final UsuarioRepository usuarioRepository;
    private final CuponRepository cuponRepository;
    private final CatalogoViajeService catalogoViajeService;
    private final AuditoriaService auditoriaService;

    public VentaPasajeService(VentaRepository ventaRepository,
                              BoletoRepository boletoRepository,
                              ViajeRepository viajeRepository,
                              UsuarioRepository usuarioRepository,
                              CuponRepository cuponRepository,
                              CatalogoViajeService catalogoViajeService,
                              AuditoriaService auditoriaService) {
        this.ventaRepository = ventaRepository;
        this.boletoRepository = boletoRepository;
        this.viajeRepository = viajeRepository;
        this.usuarioRepository = usuarioRepository;
        this.cuponRepository = cuponRepository;
        this.catalogoViajeService = catalogoViajeService;
        this.auditoriaService = auditoriaService;
    }

    /**
     * Bloqueo temporal de 15 minutos en BD para el flujo de compra online del CLIENTE.
     */
    @Transactional
    public Map<String, Object> reservarAsientosTemporalmente(ReservaTemporalRequest req, SesionUsuario sesion) {
        catalogoViajeService.expirarReservasVencidas();
        if (req == null || req.idViaje() == null || req.asientos() == null || req.asientos().isEmpty()) {
            throw new ReglaNegocioException("Debes seleccionar un viaje y al menos un asiento.");
        }

        Viaje viaje = obtenerViajeVendible(req.idViaje());
        List<Integer> asientos = normalizarListaAsientos(req.asientos());
        Usuario cliente = usuarioRepository.findById(sesion.idUsuario())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));

        // Liberar cualquier reserva previa sin pagar que el mismo cliente tuviera en este viaje
        List<Venta> previas = ventaRepository.findByCliente_IdUsuarioOrderByFechaVentaDesc(cliente.getIdUsuario());
        for (Venta previa : previas) {
            if (EstadoVenta.RESERVADO.name().equalsIgnoreCase(previa.getEstadoPago())
                    && previa.getViaje() != null
                    && req.idViaje().equals(previa.getViaje().getIdViaje())) {
                previa.setEstadoPago(EstadoVenta.EXPIRADO.name());
                ventaRepository.saveAndFlush(previa);
            }
        }

        validarDisponibilidadAsientos(viaje, asientos, cliente.getIdUsuario());

        Venta reserva = new Venta();
        reserva.setCodigoReserva(generarCodigoUnico("UR-"));
        reserva.setTipoComprobante("BOLETA");
        reserva.setSerieNumero(generarSerieTemporal());
        reserva.setCanalVenta(CanalVenta.WEB.name());
        reserva.setViaje(viaje);
        reserva.setCliente(cliente);
        reserva.setCompradorNombre(cliente.getNombreCompleto());
        reserva.setCompradorTipoDoc(cliente.getTipoDocumento());
        reserva.setCompradorDocumento(cliente.getNumeroDocumento());
        reserva.setCompradorCorreo(cliente.getCorreo());
        reserva.setCompradorTelefono(cliente.getTelefono());
        reserva.setMetodoPago(MetodoPago.TARJETA.name());
        reserva.setEstadoPago(EstadoVenta.RESERVADO.name());
        LocalDateTime expira = LocalDateTime.now().plusMinutes(MINUTOS_RESERVA_WEB);
        reserva.setExpiraEn(expira);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (int numAsiento : asientos) {
            BigDecimal precio = TarifasAsientos.precio(numAsiento, viaje.getPrecio());
            subtotal = subtotal.add(precio);

            Boleto b = new Boleto();
            b.setCodigoBoleto(generarCodigoUnico("TK-"));
            b.setViaje(viaje);
            b.setNumeroAsiento(numAsiento);
            b.setPiso(TarifasAsientos.piso(numAsiento));
            b.setCategoriaAsiento(TarifasAsientos.categoria(numAsiento));
            b.setPasajeroTipoDoc(cliente.getTipoDocumento());
            b.setPasajeroDocumento(cliente.getNumeroDocumento());
            b.setPasajeroNombre(cliente.getNombre());
            b.setPasajeroApellido(cliente.getApellido());
            b.setPrecio(precio);
            b.setEstadoAbordaje(EstadoAbordaje.PENDIENTE.name());
            reserva.agregarBoleto(b);
        }

        reserva.setSubtotal(subtotal);
        reserva.setDescuento(BigDecimal.ZERO);
        reserva.setMontoTotal(subtotal);

        Venta guardada = ventaRepository.saveAndFlush(reserva);
        catalogoViajeService.sincronizarDisponibilidadViaje(viaje);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("idReserva", guardada.getIdVenta());
        resp.put("codigoReserva", guardada.getCodigoReserva());
        resp.put("expiraEnEpochMs", expira.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        resp.put("mensaje", "Asientos bloqueados temporalmente por 15 minutos.");
        return resp;
    }

    public Map<String, Object> validarCupon(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new ReglaNegocioException("Ingresa un código promocional.");
        }
        Cupon cupon = cuponRepository.findByCodigoIgnoreCase(codigo.trim())
                .orElseThrow(() -> new ReglaNegocioException("El código promocional no existe."));
        if (!cupon.estaVigente()) {
            throw new ReglaNegocioException("El cupón ingresó fuera de vigencia o agotó su stock.");
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("codigo", cupon.getCodigo().toUpperCase());
        resp.put("descripcion", cupon.getDescripcion());
        resp.put("montoDescuento", cupon.getMontoDescuento());
        return resp;
    }

    /**
     * Emisión transaccional definitiva para compra WEB (CLIENTE) y POS (VENDEDOR).
     */
    @Transactional
    public VentaDetalleResponse emitirVenta(EmitirVentaRequest req, CanalVenta canal, SesionUsuario actor) {
        catalogoViajeService.expirarReservasVencidas();
        if (req == null || req.idViaje() == null || req.pasajeros() == null || req.pasajeros().isEmpty()) {
            throw new ReglaNegocioException("Debes indicar el viaje y los datos de cada pasajero.");
        }

        Viaje viaje = obtenerViajeVendible(req.idViaje());
        Usuario actorUsuario = usuarioRepository.findById(actor.idUsuario())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario autenticado no encontrado."));

        // Validar asientos y pasajeros
        List<Integer> listaAsientos = req.pasajeros().stream()
                .map(PasajeroItemRequest::numeroAsiento)
                .toList();
        normalizarListaAsientos(listaAsientos);

        // Si había una reserva temporal del mismo cliente, expirarla antes de emitir la venta final
        if (canal == CanalVenta.WEB) {
            List<Venta> previas = ventaRepository.findByCliente_IdUsuarioOrderByFechaVentaDesc(actorUsuario.getIdUsuario());
            for (Venta previa : previas) {
                if (EstadoVenta.RESERVADO.name().equalsIgnoreCase(previa.getEstadoPago())
                        && previa.getViaje() != null
                        && req.idViaje().equals(previa.getViaje().getIdViaje())) {
                    previa.setEstadoPago(EstadoVenta.EXPIRADO.name());
                    ventaRepository.saveAndFlush(previa);
                }
            }
        }

        validarDisponibilidadAsientos(viaje, listaAsientos, canal == CanalVenta.WEB ? actorUsuario.getIdUsuario() : null);

        // Validar tipo de comprobante (BOLETA vs FACTURA)
        boolean esFactura = "FACTURA".equalsIgnoreCase(req.tipoComprobante());
        String ruc = null;
        String razonSocial = null;
        if (esFactura) {
            ruc = req.rucFactura() != null ? req.rucFactura().trim() : "";
            razonSocial = req.razonSocialFactura() != null ? req.razonSocialFactura().trim() : "";
            if (!ruc.matches("\\d{11}")) {
                throw new ReglaNegocioException("Para emitir Factura, el RUC debe tener exactamente 11 dígitos numéricos.");
            }
            if (razonSocial.isEmpty()) {
                throw new ReglaNegocioException("Para emitir Factura, la Razón Social es obligatoria.");
            }
        }

        // Validar método de pago
        MetodoPago metodoPago;
        try {
            metodoPago = MetodoPago.valueOf((req.metodoPago() != null ? req.metodoPago() : "EFECTIVO").trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ReglaNegocioException("Método de pago no válido.");
        }

        // Datos del comprador (si faltan, se toman del primer pasajero)
        PasajeroItemRequest primerPasajero = req.pasajeros().get(0);
        String compNombre = textoONull(req.compradorNombre());
        if (compNombre == null) {
            compNombre = (textoRequerido(primerPasajero.nombre(), "Nombre del pasajero obligatorio")
                    + " " + textoRequerido(primerPasajero.apellido(), "Apellido del pasajero obligatorio")).trim();
        }
        String compDoc = textoONull(req.compradorDocumento());
        if (compDoc == null) {
            compDoc = esFactura ? ruc : textoRequerido(primerPasajero.numeroDocumento(), "Documento del pasajero obligatorio");
        }
        String compTipoDoc = esFactura ? "RUC"
                : (textoONull(req.compradorTipoDoc()) != null ? req.compradorTipoDoc().trim().toUpperCase() : "DNI");

        Venta venta = new Venta();
        venta.setCodigoReserva(generarCodigoUnico(canal == CanalVenta.VENTANILLA ? "POS-" : "UR-"));
        venta.setTipoComprobante(esFactura ? "FACTURA" : "BOLETA");
        venta.setSerieNumero(generarSerieComprobante(esFactura));
        venta.setRucFactura(ruc);
        venta.setRazonSocialFactura(razonSocial);
        venta.setCanalVenta(canal.name());
        venta.setViaje(viaje);

        if (canal == CanalVenta.WEB) {
            venta.setCliente(actorUsuario);
        } else {
            venta.setVendedor(actorUsuario);
            // Si el comprador ya es cliente registrado por correo, asociarlo opcionalmente
            if (req.compradorCorreo() != null && !req.compradorCorreo().isBlank()) {
                usuarioRepository.findByCorreoIgnoreCase(req.compradorCorreo().trim())
                        .filter(u -> RolUsuario.CLIENTE.name().equalsIgnoreCase(u.getRol()))
                        .ifPresent(venta::setCliente);
            }
        }

        venta.setCompradorNombre(compNombre);
        venta.setCompradorTipoDoc(compTipoDoc);
        venta.setCompradorDocumento(compDoc);
        venta.setCompradorCorreo(textoONull(req.compradorCorreo()));
        venta.setCompradorTelefono(textoONull(req.compradorTelefono()));
        venta.setMetodoPago(metodoPago.name());
        venta.setEstadoPago(EstadoVenta.PAGADO.name());
        venta.setFechaVenta(LocalDateTime.now());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (PasajeroItemRequest pReq : req.pasajeros()) {
            int num = pReq.numeroAsiento();
            String pNom = textoRequerido(pReq.nombre(), "Ingresa el nombre del pasajero del asiento " + num + ".");
            String pApe = textoRequerido(pReq.apellido(), "Ingresa el apellido del pasajero del asiento " + num + ".");
            String pDoc = textoRequerido(pReq.numeroDocumento(), "Ingresa el documento del pasajero del asiento " + num + ".");
            String pTipo = textoONull(pReq.tipoDocumento()) != null ? pReq.tipoDocumento().trim().toUpperCase() : "DNI";

            BigDecimal precioAsiento = TarifasAsientos.precio(num, viaje.getPrecio());
            subtotal = subtotal.add(precioAsiento);

            String codigoBoleto = generarCodigoUnico("TK-");
            String qrBase64 = GeneradorQR.generarQRBoleto(codigoBoleto, viaje.getIdViaje(), num, pDoc);

            Boleto boleto = new Boleto();
            boleto.setCodigoBoleto(codigoBoleto);
            boleto.setViaje(viaje);
            boleto.setNumeroAsiento(num);
            boleto.setPiso(TarifasAsientos.piso(num));
            boleto.setCategoriaAsiento(TarifasAsientos.categoria(num));
            boleto.setPasajeroTipoDoc(pTipo);
            boleto.setPasajeroDocumento(pDoc);
            boleto.setPasajeroNombre(pNom);
            boleto.setPasajeroApellido(pApe);
            boleto.setPasajeroCorreo(textoONull(pReq.correo()));
            boleto.setPasajeroTelefono(textoONull(pReq.telefono()));
            boleto.setPrecio(precioAsiento);
            boleto.setCodigoQr(qrBase64);
            boleto.setEstadoAbordaje(EstadoAbordaje.PENDIENTE.name());
            venta.agregarBoleto(boleto);
        }

        // Aplicar cupón si corresponde
        BigDecimal descuento = BigDecimal.ZERO;
        if (req.codigoCupon() != null && !req.codigoCupon().isBlank()) {
            Cupon cupon = cuponRepository.findByCodigoIgnoreCase(req.codigoCupon().trim())
                    .orElseThrow(() -> new ReglaNegocioException("El cupón ingresado no existe."));
            if (!cupon.estaVigente()) {
                throw new ReglaNegocioException("El cupón ingresado ha expirado o superó su límite de usos.");
            }
            descuento = cupon.getMontoDescuento().min(subtotal);
            cupon.setUsosActuales(cupon.getUsosActuales() + 1);
            cuponRepository.save(cupon);
            venta.setCodigoCupon(cupon.getCodigo().toUpperCase());
        }

        BigDecimal totalFinal = subtotal.subtract(descuento).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        venta.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        venta.setDescuento(descuento.setScale(2, RoundingMode.HALF_UP));
        venta.setMontoTotal(totalFinal);

        Venta guardada = ventaRepository.saveAndFlush(venta);
        catalogoViajeService.sincronizarDisponibilidadViaje(viaje);

        return mapearVentaDetalle(guardada);
    }

    @Transactional(readOnly = true)
    public List<VentaDetalleResponse> listarComprasCliente(Integer idCliente) {
        return ventaRepository.findByCliente_IdUsuarioOrderByFechaVentaDesc(idCliente).stream()
                .filter(v -> !EstadoVenta.RESERVADO.name().equalsIgnoreCase(v.getEstadoPago())
                        && !EstadoVenta.EXPIRADO.name().equalsIgnoreCase(v.getEstadoPago()))
                .map(this::mapearVentaDetalle)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VentaDetalleResponse> listarVentasVendedor(Integer idVendedor) {
        return ventaRepository.findByVendedor_IdUsuarioOrderByFechaVentaDesc(idVendedor).stream()
                .map(this::mapearVentaDetalle)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerArqueoCajaVendedor(Integer idVendedor) {
        LocalDate hoy = LocalDate.now();
        List<Venta> ventasHoy = ventaRepository.findByVendedor_IdUsuarioOrderByFechaVentaDesc(idVendedor).stream()
                .filter(v -> v.getFechaVenta() != null && v.getFechaVenta().toLocalDate().equals(hoy))
                .toList();

        BigDecimal totalRecaudado = BigDecimal.ZERO;
        int operacionesPagadas = 0;
        int boletosEmitidos = 0;
        Map<String, BigDecimal> porMetodo = new LinkedHashMap<>();
        for (MetodoPago mp : MetodoPago.values()) {
            porMetodo.put(mp.name(), BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }

        for (Venta v : ventasHoy) {
            if (EstadoVenta.PAGADO.name().equalsIgnoreCase(v.getEstadoPago())
                    || EstadoVenta.ANULACION_SOLICITADA.name().equalsIgnoreCase(v.getEstadoPago())) {
                totalRecaudado = totalRecaudado.add(v.getMontoTotal());
                operacionesPagadas++;
                boletosEmitidos += v.getBoletos().size();
                porMetodo.computeIfPresent(v.getMetodoPago(), (k, val) -> val.add(v.getMontoTotal()));
            }
        }

        Map<String, Object> arqueo = new LinkedHashMap<>();
        arqueo.put("fecha", hoy.toString());
        arqueo.put("operacionesHoy", operacionesPagadas);
        arqueo.put("boletosHoy", boletosEmitidos);
        arqueo.put("totalRecaudadoHoy", totalRecaudado.setScale(2, RoundingMode.HALF_UP));
        arqueo.put("desglosePorMetodo", porMetodo);
        return arqueo;
    }

    @Transactional(readOnly = true)
    public VentaDetalleResponse obtenerComprobanteAutorizado(Integer idVenta, SesionUsuario sesion) {
        Venta venta = ventaRepository.findById(idVenta)
                .orElseThrow(() -> new RecursoNoEncontradoException("La venta solicitada no existe."));

        boolean esStaff = sesion.rol() == RolUsuario.ADMIN
                || sesion.rol() == RolUsuario.VENDEDOR
                || sesion.rol() == RolUsuario.EMBARQUE;
        boolean esClienteDueno = venta.getCliente() != null
                && sesion.idUsuario().equals(venta.getCliente().getIdUsuario());

        if (!esStaff && !esClienteDueno) {
            throw new AccesoDenegadoException("No tienes permiso para visualizar este comprobante.");
        }
        return mapearVentaDetalle(venta);
    }

    /**
     * El VENDEDOR no puede eliminar ni anular directamente: registra una solicitud motivada para el ADMIN.
     */
    @Transactional
    public VentaDetalleResponse solicitarAnulacionPorVendedor(Integer idVenta, String motivo, SesionUsuario sesionVendedor) {
        String motivoLimpio = textoRequerido(motivo, "Debes indicar el motivo de la solicitud de anulación.");
        Venta venta = ventaRepository.findById(idVenta)
                .orElseThrow(() -> new RecursoNoEncontradoException("La venta no existe."));

        if (!EstadoVenta.PAGADO.name().equalsIgnoreCase(venta.getEstadoPago())) {
            throw new ReglaNegocioException("Solo se puede solicitar anulación de ventas en estado PAGADO.");
        }
        if (sesionVendedor.rol() == RolUsuario.VENDEDOR
                && (venta.getVendedor() == null || !sesionVendedor.idUsuario().equals(venta.getVendedor().getIdUsuario()))) {
            throw new AccesoDenegadoException("Solo puedes solicitar la anulación de ventas registradas en tu propia caja.");
        }

        Usuario solicitante = usuarioRepository.findById(sesionVendedor.idUsuario())
                .orElseThrow(() -> new RecursoNoEncontradoException("Vendedor no encontrado."));

        venta.setEstadoPago(EstadoVenta.ANULACION_SOLICITADA.name());
        venta.setMotivoAnulacion(motivoLimpio);
        venta.setSolicitanteAnulacion(solicitante);
        Venta actualizada = ventaRepository.saveAndFlush(venta);
        if (auditoriaService != null) {
            auditoriaService.registrar(
                    sesionVendedor,
                    "VENTAS",
                    "SOLICITAR_ANULACION",
                    "Venta",
                    actualizada.getCodigoReserva(),
                    "Vendedor solicitó anular venta " + actualizada.getCodigoReserva() + " por S/ " + actualizada.getMontoTotal(),
                    motivoLimpio
            );
        }
        return mapearVentaDetalle(actualizada);
    }

    /**
     * Acción exclusiva del ADMIN: ejecuta la anulación auditada y libera los asientos inmediatamente.
     */
    @Transactional
    public VentaDetalleResponse aprobarOEjecutarAnulacionAdmin(Integer idVenta, String motivo, SesionUsuario sesionAdmin) {
        Venta venta = ventaRepository.findById(idVenta)
                .orElseThrow(() -> new RecursoNoEncontradoException("La venta no existe."));

        if (EstadoVenta.ANULADO.name().equalsIgnoreCase(venta.getEstadoPago())) {
            throw new ReglaNegocioException("Esta venta ya se encuentra anulada.");
        }

        boolean veniaDeSolicitud = EstadoVenta.ANULACION_SOLICITADA.name().equalsIgnoreCase(venta.getEstadoPago());
        String motivoFinal = textoONull(motivo);
        if (motivoFinal == null) {
            if (veniaDeSolicitud && venta.getMotivoAnulacion() != null && !venta.getMotivoAnulacion().isBlank()) {
                motivoFinal = venta.getMotivoAnulacion();
            } else {
                throw new ReglaNegocioException("Por política de auditoría, el administrador debe ingresar un motivo obligatorio para anular una venta.");
            }
        }

        Usuario admin = usuarioRepository.findById(sesionAdmin.idUsuario())
                .orElseThrow(() -> new RecursoNoEncontradoException("Administrador no encontrado."));

        venta.setEstadoPago(EstadoVenta.ANULADO.name());
        venta.setMotivoAnulacion(motivoFinal);
        venta.setAutorizadorAnulacion(admin);
        venta.setFechaAnulacion(LocalDateTime.now());
        Venta guardada = ventaRepository.saveAndFlush(venta);

        if (guardada.getViaje() != null) {
            catalogoViajeService.sincronizarDisponibilidadViaje(guardada.getViaje());
        }
        if (auditoriaService != null) {
            auditoriaService.registrar(
                    sesionAdmin,
                    "VENTAS",
                    "ANULAR_VENTA",
                    "Venta",
                    guardada.getCodigoReserva(),
                    (veniaDeSolicitud ? "Aprobó solicitud y anuló venta " : "Anuló directamente la venta ")
                            + guardada.getCodigoReserva() + " (S/ " + guardada.getMontoTotal() + "), liberando "
                            + guardada.getBoletos().size() + " asiento(s).",
                    motivoFinal
            );
        }
        return mapearVentaDetalle(guardada);
    }

    @Transactional
    public VentaDetalleResponse rechazarSolicitudAnulacionAdmin(Integer idVenta, String observacion, SesionUsuario sesionAdmin) {
        Venta venta = ventaRepository.findById(idVenta)
                .orElseThrow(() -> new RecursoNoEncontradoException("La venta no existe."));
        if (!EstadoVenta.ANULACION_SOLICITADA.name().equalsIgnoreCase(venta.getEstadoPago())) {
            throw new ReglaNegocioException("La venta no tiene una solicitud de anulación pendiente.");
        }
        String obsFinal = observacion != null && !observacion.isBlank()
                ? observacion.trim()
                : "Solicitud rechazada por Administración";
        venta.setEstadoPago(EstadoVenta.PAGADO.name());
        venta.setMotivoAnulacion("Solicitud rechazada: " + obsFinal);
        Venta actualizada = ventaRepository.saveAndFlush(venta);
        if (auditoriaService != null) {
            auditoriaService.registrar(
                    sesionAdmin,
                    "VENTAS",
                    "RECHAZAR_ANULACION",
                    "Venta",
                    actualizada.getCodigoReserva(),
                    "Rechazó solicitud de anulación de venta " + actualizada.getCodigoReserva() + " (vuelve a PAGADO).",
                    obsFinal
            );
        }
        return mapearVentaDetalle(actualizada);
    }

    @Transactional(readOnly = true)
    public List<VentaDetalleResponse> listarTodasLasVentasAdmin(String filtroEstado, String filtroCanal) {
        return ventaRepository.findAllByOrderByFechaVentaDesc().stream()
                .filter(v -> !EstadoVenta.RESERVADO.name().equalsIgnoreCase(v.getEstadoPago())
                        && !EstadoVenta.EXPIRADO.name().equalsIgnoreCase(v.getEstadoPago()))
                .filter(v -> filtroEstado == null || filtroEstado.isBlank()
                        || filtroEstado.equalsIgnoreCase(v.getEstadoPago()))
                .filter(v -> filtroCanal == null || filtroCanal.isBlank()
                        || filtroCanal.equalsIgnoreCase(v.getCanalVenta()))
                .map(this::mapearVentaDetalle)
                .toList();
    }

    public VentaDetalleResponse mapearVentaDetalle(Venta v) {
        Viaje viaje = v.getViaje();
        Ruta ruta = viaje != null ? viaje.getRuta() : null;

        List<BoletoDetalleResponse> items = v.getBoletos().stream()
                .sorted(Comparator.comparing(Boleto::getNumeroAsiento))
                .map(b -> new BoletoDetalleResponse(
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
                ))
                .toList();

        return new VentaDetalleResponse(
                v.getIdVenta(),
                v.getCodigoReserva(),
                "FACTURA".equalsIgnoreCase(v.getTipoComprobante()) ? "FACTURA ELECTRÓNICA" : "BOLETA DE VENTA ELECTRÓNICA",
                v.getSerieNumero(),
                v.getRucFactura(),
                v.getRazonSocialFactura(),
                v.getCanalVenta(),
                viaje != null ? viaje.getIdViaje() : null,
                ruta != null && ruta.getOrigen() != null ? ruta.getOrigen().getCiudad() : "-",
                ruta != null && ruta.getOrigen() != null ? ruta.getOrigen().getNombre() : "-",
                ruta != null && ruta.getDestino() != null ? ruta.getDestino().getCiudad() : "-",
                ruta != null && ruta.getDestino() != null ? ruta.getDestino().getNombre() : "-",
                viaje != null && viaje.getFechaHoraSalida() != null ? viaje.getFechaHoraSalida().format(FMT_FECHA_HORA) : "-",
                viaje != null ? viaje.getServicio() : "Doble Piso VIP",
                viaje != null && viaje.getBus() != null ? viaje.getBus().getPlaca() : "-",
                v.getCompradorNombre(),
                v.getCompradorTipoDoc(),
                v.getCompradorDocumento(),
                v.getCompradorCorreo(),
                v.getCompradorTelefono(),
                v.getVendedor() != null ? v.getVendedor().getNombreCompleto() : "Autoservicio Web",
                v.getMetodoPago(),
                v.getCodigoCupon(),
                v.getSubtotal(),
                v.getDescuento(),
                v.getMontoTotal(),
                v.getEstadoPago(),
                v.getFechaVenta() != null ? v.getFechaVenta().format(FMT_FECHA_HORA) : "-",
                v.getExpiraEn() != null ? v.getExpiraEn().format(FMT_FECHA_HORA) : null,
                v.getMotivoAnulacion(),
                v.getSolicitanteAnulacion() != null ? v.getSolicitanteAnulacion().getNombreCompleto() : null,
                v.getAutorizadorAnulacion() != null ? v.getAutorizadorAnulacion().getNombreCompleto() : null,
                v.getFechaAnulacion() != null ? v.getFechaAnulacion().format(FMT_FECHA_HORA) : null,
                items
        );
    }

    private Viaje obtenerViajeVendible(Integer idViaje) {
        Viaje viaje = viajeRepository.findById(idViaje)
                .orElseThrow(() -> new RecursoNoEncontradoException("El viaje seleccionado no existe."));
        if (EstadoViaje.CANCELADO.name().equalsIgnoreCase(viaje.getEstado())
                || EstadoViaje.FINALIZADO.name().equalsIgnoreCase(viaje.getEstado())
                || EstadoViaje.EN_RUTA.name().equalsIgnoreCase(viaje.getEstado())) {
            throw new ReglaNegocioException("Este viaje ya no admite venta de pasajes (estado: " + viaje.getEstado() + ").");
        }
        return viaje;
    }

    private List<Integer> normalizarListaAsientos(List<Integer> asientos) {
        if (asientos.isEmpty()) {
            throw new ReglaNegocioException("Selecciona al menos un asiento.");
        }
        if (asientos.size() > TarifasAsientos.MAX_POR_VENTA) {
            throw new ReglaNegocioException("Solo puedes adquirir hasta " + TarifasAsientos.MAX_POR_VENTA + " asientos por operación.");
        }
        Set<Integer> unicos = new TreeSet<>();
        for (Integer num : asientos) {
            if (num == null || num < 1 || num > TarifasAsientos.TOTAL_ASIENTOS) {
                throw new ReglaNegocioException("El número de asiento " + num + " no es válido.");
            }
            if (!unicos.add(num)) {
                throw new ReglaNegocioException("El asiento " + num + " está duplicado en la solicitud.");
            }
        }
        return new ArrayList<>(unicos);
    }

    private void validarDisponibilidadAsientos(Viaje viaje, List<Integer> solicitados, Integer idClienteIgnorarReserva) {
        Set<Integer> ocupados = new HashSet<>();
        for (Boleto b : boletoRepository.findByViaje_IdViajeOrderByNumeroAsientoAsc(viaje.getIdViaje())) {
            Venta v = b.getVenta();
            if (v == null) continue;
            String est = v.getEstadoPago();
            if (EstadoVenta.ANULADO.name().equalsIgnoreCase(est)
                    || EstadoVenta.EXPIRADO.name().equalsIgnoreCase(est)) {
                continue;
            }
            if (EstadoVenta.RESERVADO.name().equalsIgnoreCase(est)
                    && idClienteIgnorarReserva != null
                    && v.getCliente() != null
                    && idClienteIgnorarReserva.equals(v.getCliente().getIdUsuario())) {
                continue;
            }
            ocupados.add(b.getNumeroAsiento());
        }

        List<Integer> conflictos = solicitados.stream().filter(ocupados::contains).toList();
        if (!conflictos.isEmpty()) {
            throw new ReglaNegocioException("Los siguientes asientos ya fueron ocupados en este viaje: " + conflictos);
        }
    }

    private String generarCodigoUnico(String prefijo) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(prefijo);
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String generarSerieComprobante(boolean esFactura) {
        String serie = esFactura ? "F001-" : "B001-";
        long correlativo = ventaRepository.count() + 1 + RANDOM.nextInt(90);
        return serie + String.format("%06d", correlativo) + "-" + (10 + RANDOM.nextInt(89));
    }

    private String generarSerieTemporal() {
        return "TMP-" + System.currentTimeMillis() + "-" + RANDOM.nextInt(1000);
    }

    private String textoRequerido(String val, String error) {
        if (val == null || val.trim().isEmpty()) {
            throw new ReglaNegocioException(error);
        }
        return val.trim();
    }

    private String textoONull(String val) {
        return (val == null || val.trim().isEmpty()) ? null : val.trim();
    }
}
