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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Inicializa cuentas de demostración por cargo y datos operativos base si la BD está vacía.
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

    public DataInitializer(UsuarioRepository usuarioRepository,
                           TerminalRepository terminalRepository,
                           RutaRepository rutaRepository,
                           BusRepository busRepository,
                           EmpleadoRepository empleadoRepository,
                           ViajeRepository viajeRepository,
                           CuponRepository cuponRepository) {
        this.usuarioRepository = usuarioRepository;
        this.terminalRepository = terminalRepository;
        this.rutaRepository = rutaRepository;
        this.busRepository = busRepository;
        this.empleadoRepository = empleadoRepository;
        this.viajeRepository = viajeRepository;
        this.cuponRepository = cuponRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        sembrarUsuariosPorRol();
        sembrarCatalogoOperativo();
        sembrarCupones();
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
}
