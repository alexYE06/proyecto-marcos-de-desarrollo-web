package pe.transporte.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.transporte.dto.AdminOperacionDtos.*;
import pe.transporte.exception.RecursoNoEncontradoException;
import pe.transporte.exception.ReglaNegocioException;
import pe.transporte.model.*;
import pe.transporte.model.enums.CanalVenta;
import pe.transporte.model.enums.EstadoVenta;
import pe.transporte.model.enums.RolUsuario;
import pe.transporte.repository.*;
import pe.transporte.security.SesionUsuario;
import pe.transporte.security.SessionTokenStore;
import pe.transporte.util.EncriptadorPassword;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AdminGestionService {

    private final UsuarioRepository usuarioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final BusRepository busRepository;
    private final TerminalRepository terminalRepository;
    private final RutaRepository rutaRepository;
    private final ViajeRepository viajeRepository;
    private final VentaRepository ventaRepository;
    private final CuponRepository cuponRepository;
    private final SessionTokenStore tokenStore;

    public AdminGestionService(UsuarioRepository usuarioRepository,
                               EmpleadoRepository empleadoRepository,
                               BusRepository busRepository,
                               TerminalRepository terminalRepository,
                               RutaRepository rutaRepository,
                               ViajeRepository viajeRepository,
                               VentaRepository ventaRepository,
                               CuponRepository cuponRepository,
                               SessionTokenStore tokenStore) {
        this.usuarioRepository = usuarioRepository;
        this.empleadoRepository = empleadoRepository;
        this.busRepository = busRepository;
        this.terminalRepository = terminalRepository;
        this.rutaRepository = rutaRepository;
        this.viajeRepository = viajeRepository;
        this.ventaRepository = ventaRepository;
        this.cuponRepository = cuponRepository;
        this.tokenStore = tokenStore;
    }

    // ==================== 1. DASHBOARD Y REPORTES GERENCIALES ====================

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerDashboardEjecutivo() {
        List<Venta> todas = ventaRepository.findAllByOrderByFechaVentaDesc();

        BigDecimal ingresosTotales = BigDecimal.ZERO;
        BigDecimal ingresosWeb = BigDecimal.ZERO;
        BigDecimal ingresosVentanilla = BigDecimal.ZERO;
        int ventasConfirmadas = 0;
        int boletosVendidos = 0;
        int solicitudesAnulacion = 0;
        int ventasAnuladas = 0;

        Map<String, BigDecimal> ingresosPorRuta = new LinkedHashMap<>();
        Map<String, BigDecimal> ingresosPorMetodo = new LinkedHashMap<>();
        Map<String, BigDecimal> ventasPorVendedor = new LinkedHashMap<>();

        for (Venta v : todas) {
            String estado = v.getEstadoPago();
            if (EstadoVenta.ANULACION_SOLICITADA.name().equalsIgnoreCase(estado)) {
                solicitudesAnulacion++;
            } else if (EstadoVenta.ANULADO.name().equalsIgnoreCase(estado)) {
                ventasAnuladas++;
                continue;
            } else if (!EstadoVenta.PAGADO.name().equalsIgnoreCase(estado)) {
                continue;
            }

            // Contabilizar PAGADO y ANULACION_SOLICITADA (aún vigente hasta que Admin decida)
            ingresosTotales = ingresosTotales.add(v.getMontoTotal());
            ventasConfirmadas++;
            boletosVendidos += v.getBoletos().size();

            if (CanalVenta.WEB.name().equalsIgnoreCase(v.getCanalVenta())) {
                ingresosWeb = ingresosWeb.add(v.getMontoTotal());
            } else {
                ingresosVentanilla = ingresosVentanilla.add(v.getMontoTotal());
            }

            if (v.getViaje() != null && v.getViaje().getRuta() != null) {
                Ruta r = v.getViaje().getRuta();
                String etiquetaRuta = (r.getOrigen() != null ? r.getOrigen().getCiudad() : "?")
                        + " → " + (r.getDestino() != null ? r.getDestino().getCiudad() : "?");
                ingresosPorRuta.merge(etiquetaRuta, v.getMontoTotal(), BigDecimal::add);
            }

            if (v.getMetodoPago() != null) {
                ingresosPorMetodo.merge(v.getMetodoPago(), v.getMontoTotal(), BigDecimal::add);
            }

            if (v.getVendedor() != null) {
                ventasPorVendedor.merge(v.getVendedor().getNombreCompleto(), v.getMontoTotal(), BigDecimal::add);
            }
        }

        long busesActivos = busRepository.findAll().stream()
                .filter(b -> "Activo".equalsIgnoreCase(b.getEstado()))
                .count();

        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("ingresosTotales", ingresosTotales.setScale(2, RoundingMode.HALF_UP));
        dashboard.put("ingresosWeb", ingresosWeb.setScale(2, RoundingMode.HALF_UP));
        dashboard.put("ingresosVentanilla", ingresosVentanilla.setScale(2, RoundingMode.HALF_UP));
        dashboard.put("ventasConfirmadas", ventasConfirmadas);
        dashboard.put("boletosVendidos", boletosVendidos);
        dashboard.put("solicitudesAnulacionPendientes", solicitudesAnulacion);
        dashboard.put("ventasAnuladas", ventasAnuladas);
        dashboard.put("totalUsuarios", usuarioRepository.count());
        dashboard.put("busesActivos", busesActivos);
        dashboard.put("totalBuses", busRepository.count());
        dashboard.put("totalRutas", rutaRepository.count());
        dashboard.put("totalViajes", viajeRepository.count());
        dashboard.put("ingresosPorRuta", ingresosPorRuta);
        dashboard.put("ingresosPorMetodo", ingresosPorMetodo);
        dashboard.put("rankingVendedores", ventasPorVendedor);
        return dashboard;
    }

    // ==================== 2. GESTIÓN DE USUARIOS Y ROLES ====================

    public List<Usuario> listarUsuarios(String rolFiltro) {
        if (rolFiltro != null && !rolFiltro.isBlank()) {
            return usuarioRepository.findByRolIgnoreCaseOrderByIdUsuarioDesc(rolFiltro.trim());
        }
        return usuarioRepository.findAllByOrderByIdUsuarioDesc();
    }

    @Transactional
    public Usuario crearUsuarioConRol(CrearUsuarioInternoRequest req) {
        if (req == null) throw new ReglaNegocioException("Datos obligatorios.");
        String nombre = requerido(req.nombre(), "El nombre es obligatorio.");
        String apellido = requerido(req.apellido(), "El apellido es obligatorio.");
        String doc = requerido(req.numeroDocumento(), "El número de documento es obligatorio.");
        String correo = requerido(req.correo(), "El correo es obligatorio.").toLowerCase();

        if (req.contrasena() == null || req.contrasena().length() < 6) {
            throw new ReglaNegocioException("La contraseña debe tener mínimo 6 caracteres.");
        }
        RolUsuario rol;
        try {
            rol = RolUsuario.desdeTexto(req.rol());
        } catch (Exception e) {
            throw new ReglaNegocioException("El rol especificado no es válido (ADMIN, VENDEDOR, EMBARQUE, CLIENTE).");
        }

        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ReglaNegocioException("Ya existe un usuario con ese correo electrónico.");
        }
        if (usuarioRepository.existsByNumeroDocumento(doc)) {
            throw new ReglaNegocioException("Ya existe un usuario con ese documento.");
        }

        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setApellido(apellido);
        u.setTipoDocumento(req.tipoDocumento() != null && !req.tipoDocumento().isBlank() ? req.tipoDocumento().trim().toUpperCase() : "DNI");
        u.setNumeroDocumento(doc);
        u.setCorreo(correo);
        u.setTelefono(req.telefono() != null ? req.telefono().trim() : null);
        u.setContrasenaHash(EncriptadorPassword.encriptar(req.contrasena()));
        u.setRol(rol.name());
        u.setActivo(true);
        return usuarioRepository.saveAndFlush(u);
    }

    @Transactional
    public Usuario actualizarUsuarioYRol(Integer idUsuario, ActualizarUsuarioAdminRequest req, SesionUsuario adminActual) {
        Usuario u = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));

        boolean esMismoAdmin = adminActual.idUsuario().equals(u.getIdUsuario());

        if (req.nombre() != null && !req.nombre().isBlank()) u.setNombre(req.nombre().trim());
        if (req.apellido() != null && !req.apellido().isBlank()) u.setApellido(req.apellido().trim());
        if (req.telefono() != null) u.setTelefono(req.telefono().trim());

        boolean revocarSesiones = false;

        if (req.rol() != null && !req.rol().isBlank()) {
            RolUsuario nuevoRol;
            try {
                nuevoRol = RolUsuario.desdeTexto(req.rol());
            } catch (Exception e) {
                throw new ReglaNegocioException("Rol inválido.");
            }
            if (esMismoAdmin && nuevoRol != RolUsuario.ADMIN) {
                throw new ReglaNegocioException("Por seguridad no puedes quitarte el rol ADMIN a ti mismo.");
            }
            if (!nuevoRol.name().equalsIgnoreCase(u.getRol())) {
                u.setRol(nuevoRol.name());
                revocarSesiones = true;
            }
        }

        if (req.activo() != null) {
            if (esMismoAdmin && !req.activo()) {
                throw new ReglaNegocioException("No puedes desactivar tu propia cuenta activa de administrador.");
            }
            if (!Objects.equals(u.getActivo(), req.activo())) {
                u.setActivo(req.activo());
                revocarSesiones = true;
            }
        }

        if (req.nuevaContrasena() != null && !req.nuevaContrasena().isBlank()) {
            if (req.nuevaContrasena().length() < 6) {
                throw new ReglaNegocioException("La nueva contraseña debe tener al menos 6 caracteres.");
            }
            u.setContrasenaHash(EncriptadorPassword.encriptar(req.nuevaContrasena()));
            revocarSesiones = !esMismoAdmin;
        }

        Usuario guardado = usuarioRepository.saveAndFlush(u);
        if (revocarSesiones) {
            tokenStore.revocarSesionesDeUsuario(guardado.getIdUsuario());
        }
        return guardado;
    }

    // ==================== 3. GESTIÓN DE EMPLEADOS / TRIPULACIÓN ====================

    public List<Empleado> listarEmpleados() {
        return empleadoRepository.findAllByOrderByApellidoAscNombreAsc();
    }

    @Transactional
    public Empleado guardarEmpleado(Integer idEmpleado, EmpleadoRequest req) {
        if (req == null) throw new ReglaNegocioException("Datos del empleado obligatorios.");
        Empleado emp = idEmpleado != null
                ? empleadoRepository.findById(idEmpleado).orElseThrow(() -> new RecursoNoEncontradoException("Empleado no encontrado."))
                : new Empleado();

        String dni = requerido(req.dniEmpleado(), "El DNI del empleado es obligatorio.");
        if (idEmpleado == null && empleadoRepository.existsByDniEmpleado(dni)) {
            throw new ReglaNegocioException("Ya existe un empleado registrado con ese DNI.");
        }

        emp.setDniEmpleado(dni);
        emp.setNombre(requerido(req.nombre(), "El nombre es obligatorio."));
        emp.setApellido(requerido(req.apellido(), "El apellido es obligatorio."));
        emp.setCargo(requerido(req.cargo(), "El cargo es obligatorio (Conductor, Copiloto, Terramoza)."));
        emp.setLicencia(req.licencia() != null ? req.licencia().trim() : null);
        emp.setTelefono(req.telefono() != null ? req.telefono().trim() : null);
        emp.setSueldo(req.sueldo() != null ? req.sueldo() : BigDecimal.ZERO);
        emp.setActivo(req.activo() == null || req.activo());
        return empleadoRepository.saveAndFlush(emp);
    }

    // ==================== 4. GESTIÓN DE FLOTA DE BUSES ====================

    public List<Bus> listarBuses() {
        return busRepository.findAllByOrderByIdBusAsc();
    }

    @Transactional
    public Bus guardarBus(Integer idBus, BusRequest req) {
        if (req == null) throw new ReglaNegocioException("Datos del bus obligatorios.");
        Bus bus = idBus != null
                ? busRepository.findById(idBus).orElseThrow(() -> new RecursoNoEncontradoException("Bus no encontrado."))
                : new Bus();

        String placa = requerido(req.placa(), "La placa del bus es obligatoria.").toUpperCase();
        busRepository.findByPlacaIgnoreCase(placa).ifPresent(existente -> {
            if (!existente.getIdBus().equals(idBus)) {
                throw new ReglaNegocioException("Ya existe un bus registrado con la placa " + placa + ".");
            }
        });

        bus.setPlaca(placa);
        bus.setNumeroInterno(req.numeroInterno() != null ? req.numeroInterno().trim() : null);
        bus.setMarca(req.marca() != null ? req.marca().trim() : "Scania");
        bus.setModelo(req.modelo() != null ? req.modelo().trim() : "Doble Piso VIP");
        bus.setPisos(req.pisos() != null && req.pisos() > 0 ? req.pisos() : 2);
        bus.setCapacidadAsientos(req.capacidadAsientos() != null && req.capacidadAsientos() > 0 ? req.capacidadAsientos() : 64);
        bus.setEstado(req.estado() != null && !req.estado().isBlank() ? req.estado().trim() : "Activo");
        return busRepository.saveAndFlush(bus);
    }

    // ==================== 5. TERMINALES, RUTAS Y PROGRAMACIÓN DE VIAJES ====================

    public List<Terminal> listarTerminales() {
        return terminalRepository.findAllByOrderByCiudadAscNombreAsc();
    }

    @Transactional
    public Terminal guardarTerminal(Integer idTerminal, TerminalRequest req) {
        if (req == null) throw new ReglaNegocioException("Datos del terminal obligatorios.");
        Terminal t = idTerminal != null
                ? terminalRepository.findById(idTerminal).orElseThrow(() -> new RecursoNoEncontradoException("Terminal no encontrado."))
                : new Terminal();

        t.setNombre(requerido(req.nombre(), "El nombre del terminal es obligatorio."));
        t.setCiudad(requerido(req.ciudad(), "La ciudad es obligatoria."));
        t.setDireccion(req.direccion() != null ? req.direccion().trim() : null);
        t.setActivo(req.activo() == null || req.activo());
        return terminalRepository.saveAndFlush(t);
    }

    public List<Ruta> listarRutas() {
        return rutaRepository.findAllByOrderByIdRutaAsc();
    }

    @Transactional
    public Ruta guardarRuta(Integer idRuta, RutaRequest req) {
        if (req == null || req.idTerminalOrigen() == null || req.idTerminalDestino() == null) {
            throw new ReglaNegocioException("Selecciona el terminal de origen y de destino.");
        }
        if (req.idTerminalOrigen().equals(req.idTerminalDestino())) {
            throw new ReglaNegocioException("El terminal de origen y destino no pueden ser el mismo.");
        }
        if (req.precioBase() == null || req.precioBase() <= 0) {
            throw new ReglaNegocioException("El precio base debe ser mayor a S/ 0.");
        }
        if (req.horasEstimadas() == null || req.horasEstimadas() <= 0) {
            throw new ReglaNegocioException("Las horas estimadas deben ser mayores a 0.");
        }

        Terminal orig = terminalRepository.findById(req.idTerminalOrigen())
                .orElseThrow(() -> new RecursoNoEncontradoException("Terminal de origen no existe."));
        Terminal dest = terminalRepository.findById(req.idTerminalDestino())
                .orElseThrow(() -> new RecursoNoEncontradoException("Terminal de destino no existe."));

        Ruta r = idRuta != null
                ? rutaRepository.findById(idRuta).orElseThrow(() -> new RecursoNoEncontradoException("Ruta no encontrada."))
                : new Ruta();

        r.setOrigen(orig);
        r.setDestino(dest);
        r.setPrecioBase(req.precioBase());
        r.setHorasEstimadas(req.horasEstimadas());
        r.setDistanciaKm(req.distanciaKm());
        r.setActivo(req.activo() == null || req.activo());
        return rutaRepository.saveAndFlush(r);
    }

    public List<Viaje> listarViajes() {
        return viajeRepository.findAllByOrderByFechaHoraSalidaAsc();
    }

    @Transactional
    public Viaje guardarViaje(Integer idViaje, ViajeRequest req) {
        if (req == null || req.idRuta() == null || req.fechaHoraSalida() == null || req.fechaHoraSalida().isBlank()) {
            throw new ReglaNegocioException("La ruta y la fecha/hora de salida son obligatorias.");
        }

        Ruta ruta = rutaRepository.findById(req.idRuta())
                .orElseThrow(() -> new RecursoNoEncontradoException("Ruta no encontrada."));

        LocalDateTime fechaSalida;
        try {
            fechaSalida = LocalDateTime.parse(req.fechaHoraSalida().trim());
        } catch (Exception e) {
            throw new ReglaNegocioException("Formato de fecha y hora inválido.");
        }

        Viaje v = idViaje != null
                ? viajeRepository.findById(idViaje).orElseThrow(() -> new RecursoNoEncontradoException("Viaje no encontrado."))
                : new Viaje();

        if (req.idBus() != null) {
            Bus bus = busRepository.findById(req.idBus())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Bus no encontrado."));
            if (!"Activo".equalsIgnoreCase(bus.getEstado())) {
                throw new ReglaNegocioException("No se puede asignar el bus " + bus.getPlaca() + " porque está en estado: " + bus.getEstado() + ".");
            }
            v.setBus(bus);
            if (idViaje == null) {
                v.setAsientosDisponibles(bus.getCapacidadAsientos() != null ? bus.getCapacidadAsientos() : 64);
            }
        }

        if (req.idConductor() != null) {
            Empleado cond = empleadoRepository.findById(req.idConductor())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Conductor no encontrado."));
            if (!Boolean.TRUE.equals(cond.getActivo())) {
                throw new ReglaNegocioException("El conductor seleccionado se encuentra inactivo.");
            }
            v.setConductor(cond);
        } else if (idViaje != null) {
            v.setConductor(null);
        }

        if (req.idCopiloto() != null) {
            Empleado cop = empleadoRepository.findById(req.idCopiloto())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Copiloto no encontrado."));
            v.setCopiloto(cop);
        } else if (idViaje != null) {
            v.setCopiloto(null);
        }

        v.setRuta(ruta);
        v.setFechaHoraSalida(fechaSalida);
        v.setPrecio(req.precio() != null && req.precio() > 0 ? req.precio() : (ruta.getPrecioBase() != null ? ruta.getPrecioBase() : 90.0));
        v.setServicio(req.servicio() != null && !req.servicio().isBlank() ? req.servicio().trim() : "Doble Piso VIP");
        v.setEstado(req.estado() != null && !req.estado().isBlank() ? req.estado().trim().toUpperCase() : "PROGRAMADO");
        if (v.getAsientosDisponibles() == null) {
            v.setAsientosDisponibles(64);
        }
        return viajeRepository.saveAndFlush(v);
    }

    // ==================== 6. CUPONES PROMOCIONALES ====================

    public List<Cupon> listarCupones() {
        return cuponRepository.findAllByOrderByIdCuponDesc();
    }

    @Transactional
    public Cupon guardarCupon(Integer idCupon, CuponRequest req) {
        if (req == null) throw new ReglaNegocioException("Datos del cupón obligatorios.");
        String codigo = requerido(req.codigo(), "El código del cupón es obligatorio.").toUpperCase();
        if (req.montoDescuento() == null || req.montoDescuento().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException("El monto de descuento debe ser mayor a S/ 0.");
        }

        Cupon c = idCupon != null
                ? cuponRepository.findById(idCupon).orElseThrow(() -> new RecursoNoEncontradoException("Cupón no encontrado."))
                : new Cupon();

        cuponRepository.findByCodigoIgnoreCase(codigo).ifPresent(existente -> {
            if (!existente.getIdCupon().equals(idCupon)) {
                throw new ReglaNegocioException("Ya existe un cupón con el código " + codigo + ".");
            }
        });

        c.setCodigo(codigo);
        c.setDescripcion(req.descripcion() != null ? req.descripcion().trim() : null);
        c.setMontoDescuento(req.montoDescuento());
        c.setUsosMaximos(req.usosMaximos() != null && req.usosMaximos() > 0 ? req.usosMaximos() : 100);
        c.setFechaExpiracion(req.fechaExpiracion());
        c.setActivo(req.activo() == null || req.activo());
        return cuponRepository.saveAndFlush(c);
    }

    private String requerido(String valor, String mensaje) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new ReglaNegocioException(mensaje);
        }
        return valor.trim();
    }
}
