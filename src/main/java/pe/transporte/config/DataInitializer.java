package pe.transporte.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.transporte.model.*;
import pe.transporte.repository.*;
import pe.transporte.util.EncriptadorPassword;
import pe.transporte.util.GeneradorQR;
import pe.transporte.util.TarifasAsientos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Inicializa cuentas de demostración por cargo, flota, salidas y ventas/boletos de muestra
 * para que el Manifiesto del Bus, Dashboard y Bitácora de Auditoría tengan datos reales desde el inicio.
 */
@Component
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final TerminalRepository terminalRepository;
    private final RutaRepository rutaRepository;
    private final BusRepository busRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ViajeRepository viajeRepository;
    private final CuponRepository cuponRepository;
    private final VentaRepository ventaRepository;
    private final AuditoriaLogRepository auditoriaLogRepository;

    public DataInitializer(UsuarioRepository usuarioRepository,
                           TerminalRepository terminalRepository,
                           RutaRepository rutaRepository,
                           BusRepository busRepository,
                           EmpleadoRepository empleadoRepository,
                           ViajeRepository viajeRepository,
                           CuponRepository cuponRepository,
                           VentaRepository ventaRepository,
                           AuditoriaLogRepository auditoriaLogRepository) {
        this.usuarioRepository = usuarioRepository;
        this.terminalRepository = terminalRepository;
        this.rutaRepository = rutaRepository;
        this.busRepository = busRepository;
        this.empleadoRepository = empleadoRepository;
        this.viajeRepository = viajeRepository;
        this.cuponRepository = cuponRepository;
        this.ventaRepository = ventaRepository;
        this.auditoriaLogRepository = auditoriaLogRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        sembrarUsuariosPorRol();
        sembrarCatalogoOperativo();
        sembrarCupones();
        sembrarVentasManifiestoYAuditoria();
    }

    private void sembrarUsuariosPorRol() {
        asegurarUsuario("admin@urbanride.pe", "Admin123!", "Carlos", "Mendoza", "41002001", "999111001", "ADMIN");
        asegurarUsuario("vendedor@urbanride.pe", "Vendedor123!", "Lucía", "Paredes", "45003002", "999111002", "VENDEDOR");
        asegurarUsuario("embarque@urbanride.pe", "Embarque123!", "Jorge", "Quispe", "47004003", "999111003", "EMBARQUE");
        asegurarUsuario("cliente@urbanride.pe", "Cliente123!", "Ana", "Torres", "72005004", "999111004", "CLIENTE");
    }

    private void asegurarUsuario(String correo, String passwordPlano, String nombre, String apellido,
                                 String documento, String telefono, String rol) {
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            return;
        }
        if (usuarioRepository.existsByNumeroDocumento(documento)) {
            documento = documento + rol.substring(0, 2);
        }
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setApellido(apellido);
        u.setTipoDocumento("DNI");
        u.setNumeroDocumento(documento);
        u.setCorreo(correo);
        u.setTelefono(telefono);
        u.setContrasenaHash(EncriptadorPassword.encriptar(passwordPlano));
        u.setRol(rol);
        u.setActivo(true);
        usuarioRepository.save(u);
        log.info("Cuenta inicial creada -> [{}] {}", rol, correo);
    }

    private void sembrarCatalogoOperativo() {
        if (terminalRepository.count() == 0) {
            Terminal lima = terminalRepository.save(new Terminal(null, "Terminal Javier Prado", "Lima", "Av. Javier Prado Este 1093, La Victoria"));
            Terminal ica = terminalRepository.save(new Terminal(null, "Terrapuerto Ica Sur", "Ica", "Av. Matías Manzanilla 420"));
            Terminal huancayo = terminalRepository.save(new Terminal(null, "Gran Terminal Los Andes", "Huancayo", "Vía Evitamiento Norte Km 4"));
            Terminal arequipa = terminalRepository.save(new Terminal(null, "Terrapuerto Arequipa", "Arequipa", "Av. Arturo Ibáñez s/n, Jacobo Hunter"));
            Terminal trujillo = terminalRepository.save(new Terminal(null, "Terrapuerto Trujillo", "Trujillo", "Panamericana Norte Km 558"));
            Terminal cusco = terminalRepository.save(new Terminal(null, "Terminal Terrestre Cusco", "Cusco", "Vía Evitamiento 429, Santiago"));

            crearRuta(lima, ica, 50.0, 4, 305);
            crearRuta(ica, lima, 50.0, 4, 305);
            crearRuta(lima, huancayo, 70.0, 7, 310);
            crearRuta(huancayo, lima, 70.0, 7, 310);
            crearRuta(lima, arequipa, 90.0, 16, 1015);
            crearRuta(arequipa, lima, 90.0, 16, 1015);
            crearRuta(lima, trujillo, 80.0, 10, 560);
            crearRuta(lima, cusco, 120.0, 21, 1105);
        }

        if (busRepository.count() == 0) {
            busRepository.save(new Bus(null, "B8T-951", "BUS-101", "Marcopolo Paradiso 1800 DD", "Scania", 2, 64, "Activo"));
            busRepository.save(new Bus(null, "C4K-962", "BUS-102", "Irizar i6S Double Decker", "Volvo", 2, 64, "Activo"));
            busRepository.save(new Bus(null, "D9M-958", "BUS-103", "Comil Campione Invictus DD", "Mercedes-Benz", 2, 64, "Activo"));
            busRepository.save(new Bus(null, "A2R-960", "BUS-104", "Marcopolo G8 1800 DD", "Scania", 2, 64, "Mantenimiento"));
        }

        if (empleadoRepository.count() == 0) {
            Empleado c1 = new Empleado(null, "40112233", "Roberto", "Huamán", "Conductor", "987654321", new BigDecimal("3800.00"));
            c1.setLicencia("Q40112233-A3C");
            Empleado c2 = new Empleado(null, "41223344", "Miguel", "Salazar", "Conductor", "987654322", new BigDecimal("3850.00"));
            c2.setLicencia("Q41223344-A3C");
            Empleado c3 = new Empleado(null, "42334455", "Héctor", "Cáceres", "Conductor", "987654323", new BigDecimal("3700.00"));
            c3.setLicencia("Q42334455-A3C");
            Empleado p1 = new Empleado(null, "44556677", "Diego", "Rojas", "Copiloto", "987654324", new BigDecimal("2900.00"));
            p1.setLicencia("Q44556677-A3C");
            Empleado p2 = new Empleado(null, "45667788", "Luis", "Valdivia", "Copiloto", "987654325", new BigDecimal("2900.00"));
            p2.setLicencia("Q45667788-A3C");
            empleadoRepository.saveAll(List.of(c1, c2, c3, p1, p2));
        }

        LocalDateTime inicioHoy = LocalDate.now().atStartOfDay();
        List<Viaje> viajesFuturos = viajeRepository.findByFechaHoraSalidaBetweenOrderByFechaHoraSalidaAsc(
                inicioHoy, inicioHoy.plusDays(30));

        if (viajesFuturos.isEmpty()) {
            List<Ruta> rutas = rutaRepository.findAllByOrderByIdRutaAsc();
            List<Bus> busesActivos = busRepository.findAllByOrderByIdBusAsc().stream()
                    .filter(b -> "Activo".equalsIgnoreCase(b.getEstado()))
                    .toList();
            List<Empleado> conductores = empleadoRepository.findAllByOrderByApellidoAscNombreAsc().stream()
                    .filter(e -> "Conductor".equalsIgnoreCase(e.getCargo()))
                    .toList();
            List<Empleado> copilotos = empleadoRepository.findAllByOrderByApellidoAscNombreAsc().stream()
                    .filter(e -> "Copiloto".equalsIgnoreCase(e.getCargo()))
                    .toList();

            if (!rutas.isEmpty()) {
                LocalTime[] horarios = {LocalTime.of(8, 0), LocalTime.of(14, 30), LocalTime.of(21, 30)};
                String[] servicios = {"Doble Piso VIP", "Crucero Confort 160°", "Suite Nocturna 180°"};
                int idx = 0;
                for (int diaOffset = 0; diaOffset <= 3; diaOffset++) {
                    LocalDate fecha = LocalDate.now().plusDays(diaOffset);
                    for (int r = 0; r < Math.min(rutas.size(), 6); r++) {
                        Ruta ruta = rutas.get(r);
                        for (int h = 0; h < (r < 3 ? 2 : 1); h++) {
                            Viaje v = new Viaje();
                            v.setRuta(ruta);
                            if (!busesActivos.isEmpty()) v.setBus(busesActivos.get(idx % busesActivos.size()));
                            if (!conductores.isEmpty()) v.setConductor(conductores.get(idx % conductores.size()));
                            if (!copilotos.isEmpty()) v.setCopiloto(copilotos.get(idx % copilotos.size()));
                            v.setFechaHoraSalida(LocalDateTime.of(fecha, horarios[(r + h) % horarios.length]));
                            v.setPrecio(ruta.getPrecioBase() != null ? ruta.getPrecioBase() : 90.0);
                            v.setAsientosDisponibles(64);
                            v.setServicio(servicios[(r + h) % servicios.length]);
                            v.setEstado("PROGRAMADO");
                            viajeRepository.save(v);
                            idx++;
                        }
                    }
                }
                log.info("Viajes programados generados automáticamente para los próximos días.");
            }
        }
    }

    private void crearRuta(Terminal orig, Terminal dest, double precio, int horas, int km) {
        Ruta r = new Ruta();
        r.setOrigen(orig);
        r.setDestino(dest);
        r.setPrecioBase(precio);
        r.setHorasEstimadas(horas);
        r.setDistanciaKm(km);
        r.setActivo(true);
        rutaRepository.save(r);
    }

    private void sembrarCupones() {
        if (cuponRepository.count() == 0) {
            Cupon c1 = new Cupon();
            c1.setCodigo("PROMO10");
            c1.setDescripcion("Descuento promocional web S/ 15.00");
            c1.setMontoDescuento(new BigDecimal("15.00"));
            c1.setUsosMaximos(200);
            c1.setFechaExpiracion(LocalDate.now().plusMonths(6));
            c1.setActivo(true);

            Cupon c2 = new Cupon();
            c2.setCodigo("URBAN2026");
            c2.setDescripcion("Campaña Viajero Frecuente UrbanRide S/ 20.00");
            c2.setMontoDescuento(new BigDecimal("20.00"));
            c2.setUsosMaximos(100);
            c2.setFechaExpiracion(LocalDate.now().plusMonths(6));
            c2.setActivo(true);

            cuponRepository.saveAll(List.of(c1, c2));
        }
    }

    private void sembrarVentasManifiestoYAuditoria() {
        if (ventaRepository.count() > 0) {
            return;
        }
        List<Viaje> viajes = viajeRepository.findAllByOrderByFechaHoraSalidaAsc();
        if (viajes.isEmpty()) {
            return;
        }

        Usuario cliente = usuarioRepository.findByCorreoIgnoreCase("cliente@urbanride.pe").orElse(null);
        Usuario vendedor = usuarioRepository.findByCorreoIgnoreCase("vendedor@urbanride.pe").orElse(null);
        Usuario inspector = usuarioRepository.findByCorreoIgnoreCase("embarque@urbanride.pe").orElse(null);
        Usuario admin = usuarioRepository.findByCorreoIgnoreCase("admin@urbanride.pe").orElse(null);

        Viaje primerViaje = viajes.get(0);
        Viaje segundoViaje = viajes.size() > 1 ? viajes.get(1) : primerViaje;

        // 1. Venta WEB (2 pasajeros en el primer viaje: 1 ya abordado, 1 pendiente)
        Venta vWeb = new Venta();
        vWeb.setCodigoReserva("UR-DEMO01");
        vWeb.setTipoComprobante("BOLETA");
        vWeb.setSerieNumero("B001-000101");
        vWeb.setCanalVenta("WEB");
        vWeb.setViaje(primerViaje);
        vWeb.setCliente(cliente);
        vWeb.setCompradorNombre("Ana Torres");
        vWeb.setCompradorTipoDoc("DNI");
        vWeb.setCompradorDocumento("72005004");
        vWeb.setCompradorCorreo("cliente@urbanride.pe");
        vWeb.setCompradorTelefono("999111004");
        vWeb.setMetodoPago("TARJETA");
        vWeb.setEstadoPago("PAGADO");
        vWeb.setFechaVenta(LocalDateTime.now().minusHours(2));

        Boleto b1 = crearBoletoDemo("TK-DEMO01", primerViaje, 1, "DNI", "72005004", "Ana", "Torres", "ABORDADO", inspector);
        Boleto b2 = crearBoletoDemo("TK-DEMO02", primerViaje, 2, "DNI", "72881122", "Mateo", "Torres", "PENDIENTE", null);
        vWeb.agregarBoleto(b1);
        vWeb.agregarBoleto(b2);
        BigDecimal totalWeb = b1.getPrecio().add(b2.getPrecio());
        vWeb.setSubtotal(totalWeb);
        vWeb.setDescuento(BigDecimal.ZERO);
        vWeb.setMontoTotal(totalWeb);
        ventaRepository.saveAndFlush(vWeb);

        // 2. Venta VENTANILLA POS (3 pasajeros en el primer viaje para llenar el Manifiesto del Bus)
        Venta vPos = new Venta();
        vPos.setCodigoReserva("POS-DEMO02");
        vPos.setTipoComprobante("BOLETA");
        vPos.setSerieNumero("B001-000102");
        vPos.setCanalVenta("VENTANILLA");
        vPos.setViaje(primerViaje);
        vPos.setVendedor(vendedor);
        vPos.setCompradorNombre("Sofía Ramírez");
        vPos.setCompradorTipoDoc("DNI");
        vPos.setCompradorDocumento("46119988");
        vPos.setCompradorTelefono("987112233");
        vPos.setMetodoPago("YAPE");
        vPos.setEstadoPago("PAGADO");
        vPos.setFechaVenta(LocalDateTime.now().minusMinutes(50));

        Boleto b3 = crearBoletoDemo("TK-DEMO03", primerViaje, 7, "DNI", "46119988", "Sofía", "Ramírez", "ABORDADO", inspector);
        Boleto b4 = crearBoletoDemo("TK-DEMO04", primerViaje, 8, "DNI", "48223344", "Pedro", "Castillo", "PENDIENTE", null);
        Boleto b5 = crearBoletoDemo("TK-DEMO05", primerViaje, 25, "DNI", "71998877", "Valeria", "Gutiérrez", "PENDIENTE", null);
        vPos.agregarBoleto(b3);
        vPos.agregarBoleto(b4);
        vPos.agregarBoleto(b5);
        BigDecimal totalPos = b3.getPrecio().add(b4.getPrecio()).add(b5.getPrecio());
        vPos.setSubtotal(totalPos);
        vPos.setDescuento(BigDecimal.ZERO);
        vPos.setMontoTotal(totalPos);
        ventaRepository.saveAndFlush(vPos);

        primerViaje.setAsientosDisponibles(59);
        primerViaje.setEstado("EMBARCANDO");
        viajeRepository.saveAndFlush(primerViaje);

        // 3. Venta VENTANILLA con Solicitud de Anulación pendiente para demostrar aprobación del ADMIN
        Venta vSol = new Venta();
        vSol.setCodigoReserva("POS-DEMO03");
        vSol.setTipoComprobante("FACTURA");
        vSol.setSerieNumero("F001-000103");
        vSol.setRucFactura("20601234567");
        vSol.setRazonSocialFactura("CONSULTORA ANDINA S.A.C.");
        vSol.setCanalVenta("VENTANILLA");
        vSol.setViaje(segundoViaje);
        vSol.setVendedor(vendedor);
        vSol.setCompradorNombre("CONSULTORA ANDINA S.A.C.");
        vSol.setCompradorTipoDoc("RUC");
        vSol.setCompradorDocumento("20601234567");
        vSol.setMetodoPago("EFECTIVO");
        vSol.setEstadoPago("ANULACION_SOLICITADA");
        vSol.setMotivoAnulacion("Cliente reprogramará su viaje para el turno nocturno");
        vSol.setSolicitanteAnulacion(vendedor);
        vSol.setFechaVenta(LocalDateTime.now().minusMinutes(25));

        Boleto b6 = crearBoletoDemo("TK-DEMO06", segundoViaje, 12, "DNI", "43210987", "Ricardo", "Chávez", "PENDIENTE", null);
        vSol.agregarBoleto(b6);
        vSol.setSubtotal(b6.getPrecio());
        vSol.setDescuento(BigDecimal.ZERO);
        vSol.setMontoTotal(b6.getPrecio());
        ventaRepository.saveAndFlush(vSol);

        segundoViaje.setAsientosDisponibles(Math.max(0, (segundoViaje.getAsientosDisponibles() != null ? segundoViaje.getAsientosDisponibles() : 64) - 1));
        viajeRepository.saveAndFlush(segundoViaje);

        // Eventos iniciales en Bitácora de Auditoría
        if (auditoriaLogRepository.count() == 0) {
            crearLogDemo(admin, "FLOTA", "CAMBIO_ESTADO", "Bus", "A2R-960",
                    "Retiró bus A2R-960 a estado Mantenimiento preventivo de frenos.", "Mantenimiento preventivo 50,000 km");
            crearLogDemo(inspector, "EMBARQUE", "ABORDAJE_QR", "Boleto", "TK-DEMO01",
                    "Validó abordaje QR de Ana Torres (Asiento 1, Viaje #" + primerViaje.getIdViaje() + ").", null);
            crearLogDemo(vendedor, "VENTAS", "SOLICITAR_ANULACION", "Venta", "POS-DEMO03",
                    "Solicitó anulación de Factura F001-000103 (S/ " + b6.getPrecio() + ") para aprobación de Gerencia.",
                    "Cliente reprogramará su viaje para el turno nocturno");
        }
        log.info("Ventas de demostración, Manifiesto del Bus (5 pasajeros) y Bitácora de Auditoría inicializados.");
    }

    private Boleto crearBoletoDemo(String codigo, Viaje viaje, int asiento, String tipoDoc, String numDoc,
                                   String nombre, String apellido, String estadoAbordaje, Usuario inspector) {
        Boleto b = new Boleto();
        b.setCodigoBoleto(codigo);
        b.setViaje(viaje);
        b.setNumeroAsiento(asiento);
        b.setPiso(TarifasAsientos.piso(asiento));
        b.setCategoriaAsiento(TarifasAsientos.categoria(asiento));
        b.setPasajeroTipoDoc(tipoDoc);
        b.setPasajeroDocumento(numDoc);
        b.setPasajeroNombre(nombre);
        b.setPasajeroApellido(apellido);
        b.setPrecio(TarifasAsientos.precio(asiento, viaje.getPrecio()));
        b.setCodigoQr(GeneradorQR.generarQRBoleto(codigo, viaje.getIdViaje(), asiento, numDoc));
        b.setEstadoAbordaje(estadoAbordaje);
        if ("ABORDADO".equals(estadoAbordaje)) {
            b.setFechaAbordaje(LocalDateTime.now().minusMinutes(15));
            b.setInspectorAbordaje(inspector);
        }
        return b;
    }

    private void crearLogDemo(Usuario actor, String modulo, String accion, String entidad,
                              String idEntidad, String descripcion, String motivo) {
        AuditoriaLog logEntry = new AuditoriaLog();
        logEntry.setFechaHora(LocalDateTime.now().minusMinutes(10));
        if (actor != null) {
            logEntry.setIdUsuarioActor(actor.getIdUsuario());
            logEntry.setActorNombre(actor.getNombreCompleto());
            logEntry.setActorRol(actor.getRol());
        } else {
            logEntry.setActorNombre("Sistema");
            logEntry.setActorRol("SISTEMA");
        }
        logEntry.setModulo(modulo);
        logEntry.setAccion(accion);
        logEntry.setEntidad(entidad);
        logEntry.setIdEntidad(idEntidad);
        logEntry.setDescripcion(descripcion);
        logEntry.setMotivo(motivo);
        auditoriaLogRepository.save(logEntry);
    }
}
