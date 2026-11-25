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
    private final AuditoriaService auditoriaService;

    public AdminGestionService(UsuarioRepository usuarioRepository,
                               EmpleadoRepository empleadoRepository,
                               BusRepository busRepository,
                               TerminalRepository terminalRepository,
                               RutaRepository rutaRepository,
                               ViajeRepository viajeRepository,
                               VentaRepository ventaRepository,
                               CuponRepository cuponRepository,
                               SessionTokenStore tokenStore,
                               AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.empleadoRepository = empleadoRepository;
        this.busRepository = busRepository;
        this.terminalRepository = terminalRepository;
        this.rutaRepository = rutaRepository;
        this.viajeRepository = viajeRepository;
        this.ventaRepository = ventaRepository;
        this.cuponRepository = cuponRepository;
        this.tokenStore = tokenStore;
        this.auditoriaService = auditoriaService;
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

    // ==================== 2. GESTIÓN DE USUARIOS Y ROLES (AUDITADA) ====================

    public List<Usuario> listarUsuarios(String rolFiltro) {
        if (rolFiltro != null && !rolFiltro.isBlank()) {
            return usuarioRepository.findByRolIgnoreCaseOrderByIdUsuarioDesc(rolFiltro.trim());
        }
        return usuarioRepository.findAllByOrderByIdUsuarioDesc();
    }

    @Transactional
    public Usuario crearUsuarioConRol(CrearUsuarioInternoRequest req, SesionUsuario adminActual) {
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
        Usuario guardado = usuarioRepository.saveAndFlush(u);

        auditar(adminActual, "USUARIOS", "CREAR", "Usuario", guardado.getIdUsuario(),
                "Creó cuenta " + guardado.getCorreo() + " (" + guardado.getNombreCompleto() + ") con cargo " + rol.name() + ".", null);
        return guardado;
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
                String rolAnterior = u.getRol();
                u.setRol(nuevoRol.name());
                revocarSesiones = true;
                auditar(adminActual, "USUARIOS", "CAMBIO_ROL", "Usuario", u.getIdUsuario(),
                        "Cambió cargo de " + u.getCorreo() + " de " + rolAnterior + " a " + nuevoRol.name() + ".", req.motivo());
            }
        }

        if (req.activo() != null) {
            if (esMismoAdmin && !req.activo()) {
                throw new ReglaNegocioException("No puedes desactivar tu propia cuenta activa de administrador.");
            }
            if (!Objects.equals(u.getActivo(), req.activo())) {
                if (!req.activo() && (req.motivo() == null || req.motivo().isBlank())) {
                    throw new ReglaNegocioException("Por política de auditoría, ingresa el motivo para desactivar o dar de baja a este usuario.");
                }
                u.setActivo(req.activo());
                revocarSesiones = true;
                auditar(adminActual, "USUARIOS", req.activo() ? "REACTIVAR" : "DESACTIVAR", "Usuario", u.getIdUsuario(),
                        (req.activo() ? "Reactivó acceso de " : "Bloqueó/desactivó acceso de ") + u.getCorreo() + " (" + u.getRol() + ").",
                        req.motivo());
            }
        }

        if (req.nuevaContrasena() != null && !req.nuevaContrasena().isBlank()) {
            if (req.nuevaContrasena().length() < 6) {
                throw new ReglaNegocioException("La nueva contraseña debe tener al menos 6 caracteres.");
            }
            u.setContrasenaHash(EncriptadorPassword.encriptar(req.nuevaContrasena()));
            revocarSesiones = !esMismoAdmin;
            auditar(adminActual, "USUARIOS", "RESET_CLAVE", "Usuario", u.getIdUsuario(),
                    "Restableció la contraseña de " + u.getCorreo() + " y revocó sesiones activas.", req.motivo());
        }

        Usuario guardado = usuarioRepository.saveAndFlush(u);
        if (revocarSesiones) {
            tokenStore.revocarSesionesDeUsuario(guardado.getIdUsuario());
        }
        return guardado;
    }

    // ==================== 3. EMPLEADOS / TRIPULACIÓN (AUDITADO) ====================

    public List<Empleado> listarEmpleados() {
        return empleadoRepository.findAllByOrderByApellidoAscNombreAsc();
    }

    @Transactional
    public Empleado guardarEmpleado(Integer idEmpleado, EmpleadoRequest req, SesionUsuario adminActual) {
        if (req == null) throw new ReglaNegocioException("Datos del empleado obligatorios.");
        boolean esNuevo = idEmpleado == null;
        Empleado emp = !esNuevo
                ? empleadoRepository.findById(idEmpleado).orElseThrow(() -> new RecursoNoEncontradoException("Empleado no encontrado."))
                : new Empleado();

        String dni = requerido(req.dniEmpleado(), "El DNI del empleado es obligatorio.");
        if (esNuevo && empleadoRepository.existsByDniEmpleado(dni)) {
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
        Empleado guardado = empleadoRepository.saveAndFlush(emp);

        auditar(adminActual, "FLOTA", esNuevo ? "CREAR" : "EDITAR", "Empleado", guardado.getIdEmpleado(),
                (esNuevo ? "Registró " : "Actualizó ") + guardado.getCargo() + " " + guardado.getNombreCompleto() + " (DNI " + guardado.getDniEmpleado() + ").",
                null);
        return guardado;
    }

    @Transactional
    public Empleado cambiarEstadoEmpleado(Integer idEmpleado, CambioEstadoAuditadoRequest req, SesionUsuario adminActual) {
        Empleado emp = empleadoRepository.findById(idEmpleado)
                .orElseThrow(() -> new RecursoNoEncontradoException("Empleado no encontrado."));
        boolean nuevoEstado = req != null && req.activo() != null ? req.activo() : !Boolean.TRUE.equals(emp.getActivo());
        if (!nuevoEstado) {
            requerido(req != null ? req.motivo() : null, "Ingresa un motivo obligatorio de auditoría para dar de baja al empleado.");
        }
        emp.setActivo(nuevoEstado);
        Empleado guardado = empleadoRepository.saveAndFlush(emp);
        auditar(adminActual, "FLOTA", nuevoEstado ? "REACTIVAR" : "BAJA_LOGICA", "Empleado", guardado.getIdEmpleado(),
                (nuevoEstado ? "Reactivó al empleado " : "Dio de baja lógica al empleado ") + guardado.getNombreCompleto() + " (" + guardado.getCargo() + ").",
                req != null ? req.motivo() : null);
        return guardado;
    }

    // ==================== 4. FLOTA DE BUSES (AUDITADA) ====================

    public List<Bus> listarBuses() {
        return busRepository.findAllByOrderByIdBusAsc();
    }

    @Transactional
    public Bus guardarBus(Integer idBus, BusRequest req, SesionUsuario adminActual) {
        if (req == null) throw new ReglaNegocioException("Datos del bus obligatorios.");
        boolean esNuevo = idBus == null;
        Bus bus = !esNuevo
                ? busRepository.findById(idBus).orElseThrow(() -> new RecursoNoEncontradoException("Bus no encontrado."))
                : new Bus();

        String estadoAnterior = bus.getEstado();
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
        Bus guardado = busRepository.saveAndFlush(bus);

        auditar(adminActual, "FLOTA", esNuevo ? "CREAR" : "CAMBIO_ESTADO", "Bus", guardado.getPlaca(),
                esNuevo ? "Incorporó bus placa " + guardado.getPlaca() + " (" + guardado.getMarca() + " " + guardado.getModelo() + ")."
                        : "Cambió estado del bus " + guardado.getPlaca() + " de " + estadoAnterior + " a " + guardado.getEstado() + ".",
                null);
        return guardado;
    }

    @Transactional
    public Bus cambiarEstadoBusAuditado(Integer idBus, CambioEstadoAuditadoRequest req, SesionUsuario adminActual) {
        Bus bus = busRepository.findById(idBus)
                .orElseThrow(() -> new RecursoNoEncontradoException("Bus no encontrado."));
        String nuevoEstado = requerido(req != null ? req.estado() : null, "Indica el nuevo estado del bus.");
        if (!"Activo".equalsIgnoreCase(nuevoEstado)) {
            requerido(req.motivo(), "Indica un motivo de auditoría para retirar el bus a " + nuevoEstado + ".");
        }
        String anterior = bus.getEstado();
        bus.setEstado(nuevoEstado);
        Bus guardado = busRepository.saveAndFlush(bus);
        auditar(adminActual, "FLOTA", "Inactivo".equalsIgnoreCase(nuevoEstado) ? "BAJA_LOGICA" : "CAMBIO_ESTADO",
                "Bus", guardado.getPlaca(),
                "Cambió estado del bus " + guardado.getPlaca() + " de " + anterior + " a " + nuevoEstado + ".",
                req.motivo());
        return guardado;
    }

    // ==================== 5. TERMINALES, RUTAS Y VIAJES (AUDITADOS) ====================

    public List<Terminal> listarTerminales() {
        return terminalRepository.findAllByOrderByCiudadAscNombreAsc();
    }

    @Transactional
    public Terminal guardarTerminal(Integer idTerminal, TerminalRequest req, SesionUsuario adminActual) {
        if (req == null) throw new ReglaNegocioException("Datos del terminal obligatorios.");
        boolean esNuevo = idTerminal == null;
        Terminal t = !esNuevo
                ? terminalRepository.findById(idTerminal).orElseThrow(() -> new RecursoNoEncontradoException("Terminal no encontrado."))
                : new Terminal();

        t.setNombre(requerido(req.nombre(), "El nombre del terminal es obligatorio."));
        t.setCiudad(requerido(req.ciudad(), "La ciudad es obligatoria."));
        t.setDireccion(req.direccion() != null ? req.direccion().trim() : null);
        t.setActivo(req.activo() == null || req.activo());
        Terminal guardado = terminalRepository.saveAndFlush(t);
        auditar(adminActual, "RUTAS", esNuevo ? "CREAR" : "EDITAR", "Terminal", guardado.getIdTerminal(),
                (esNuevo ? "Creó terminal " : "Editó terminal ") + guardado.getCiudad() + " — " + guardado.getNombre() + ".", null);
        return guardado;
    }

    @Transactional
    public Terminal cambiarEstadoTerminal(Integer idTerminal, CambioEstadoAuditadoRequest req, SesionUsuario adminActual) {
        Terminal t = terminalRepository.findById(idTerminal)
                .orElseThrow(() -> new RecursoNoEncontradoException("Terminal no encontrado."));
        boolean nuevo = req != null && req.activo() != null ? req.activo() : !Boolean.TRUE.equals(t.getActivo());
        if (!nuevo) {
            requerido(req != null ? req.motivo() : null, "Ingresa un motivo obligatorio de auditoría para desactivar el terminal.");
        }
        t.setActivo(nuevo);
        Terminal guardado = terminalRepository.saveAndFlush(t);
        auditar(adminActual, "RUTAS", nuevo ? "REACTIVAR" : "BAJA_LOGICA", "Terminal", guardado.getIdTerminal(),
                (nuevo ? "Reactivó terminal " : "Desactivó terminal ") + guardado.getCiudad() + " — " + guardado.getNombre() + ".",
                req != null ? req.motivo() : null);
        return guardado;
    }

    public List<Ruta> listarRutas() {
        return rutaRepository.findAllByOrderByIdRutaAsc();
    }

    @Transactional
    public Ruta guardarRuta(Integer idRuta, RutaRequest req, SesionUsuario adminActual) {
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

        boolean esNuevo = idRuta == null;
        Ruta r = !esNuevo
                ? rutaRepository.findById(idRuta).orElseThrow(() -> new RecursoNoEncontradoException("Ruta no encontrada."))
                : new Ruta();

        r.setOrigen(orig);
        r.setDestino(dest);
        r.setPrecioBase(req.precioBase());
        r.setHorasEstimadas(req.horasEstimadas());
        r.setDistanciaKm(req.distanciaKm());
        r.setActivo(req.activo() == null || req.activo());
        Ruta guardada = rutaRepository.saveAndFlush(r);
        auditar(adminActual, "RUTAS", esNuevo ? "CREAR" : "EDITAR", "Ruta", guardada.getIdRuta(),
                (esNuevo ? "Creó ruta " : "Editó ruta ") + orig.getCiudad() + " → " + dest.getCiudad() + " (Base S/ " + req.precioBase() + ").",
                null);
        return guardada;
    }

    @Transactional
    public Ruta cambiarEstadoRuta(Integer idRuta, CambioEstadoAuditadoRequest req, SesionUsuario adminActual) {
        Ruta r = rutaRepository.findById(idRuta)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ruta no encontrada."));
        boolean nuevo = req != null && req.activo() != null ? req.activo() : !Boolean.TRUE.equals(r.getActivo());
        if (!nuevo) {
            requerido(req != null ? req.motivo() : null, "Ingresa un motivo obligatorio de auditoría para desactivar la ruta.");
        }
        r.setActivo(nuevo);
        Ruta guardada = rutaRepository.saveAndFlush(r);
        auditar(adminActual, "RUTAS", nuevo ? "REACTIVAR" : "BAJA_LOGICA", "Ruta", guardada.getIdRuta(),
                (nuevo ? "Reactivó ruta " : "Desactivó ruta ") + r.toString() + ".",
                req != null ? req.motivo() : null);
        return guardada;
    }

    public List<Viaje> listarViajes() {
        return viajeRepository.findAllByOrderByFechaHoraSalidaAsc();
    }

    @Transactional
    public Viaje guardarViaje(Integer idViaje, ViajeRequest req, SesionUsuario adminActual) {
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

        boolean esNuevo = idViaje == null;
        Viaje v = !esNuevo
                ? viajeRepository.findById(idViaje).orElseThrow(() -> new RecursoNoEncontradoException("Viaje no encontrado."))
                : new Viaje();

        if (req.idBus() != null) {
            Bus bus = busRepository.findById(req.idBus())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Bus no encontrado."));
            if (!"Activo".equalsIgnoreCase(bus.getEstado())) {
                throw new ReglaNegocioException("No se puede asignar el bus " + bus.getPlaca() + " porque está en estado: " + bus.getEstado() + ".");
            }
            v.setBus(bus);
            if (esNuevo) {
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
        } else if (!esNuevo) {
            v.setConductor(null);
        }

        if (req.idCopiloto() != null) {
            Empleado cop = empleadoRepository.findById(req.idCopiloto())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Copiloto no encontrado."));
            v.setCopiloto(cop);
        } else if (!esNuevo) {
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
        Viaje guardado = viajeRepository.saveAndFlush(v);
        auditar(adminActual, "VIAJES", esNuevo ? "CREAR" : "EDITAR", "Viaje", guardado.getIdViaje(),
                (esNuevo ? "Programó viaje #" : "Modificó viaje #") + guardado.getIdViaje() + " (" + ruta.getOrigen().getCiudad()
                        + " → " + ruta.getDestino().getCiudad() + ", salida " + fechaSalida + ").", null);
        return guardado;
    }

    @Transactional
    public Viaje cancelarViajeAuditado(Integer idViaje, String motivo, SesionUsuario adminActual) {
        String motivoLimpio = requerido(motivo, "Por auditoría, debes ingresar un motivo obligatorio para cancelar esta salida.");
        Viaje v = viajeRepository.findById(idViaje)
                .orElseThrow(() -> new RecursoNoEncontradoException("Viaje no encontrado."));
        v.setEstado("CANCELADO");
        Viaje guardado = viajeRepository.saveAndFlush(v);
        auditar(adminActual, "VIAJES", "CANCELAR_VIAJE", "Viaje", guardado.getIdViaje(),
                "Canceló salida programada #" + guardado.getIdViaje() + " ("
                        + (guardado.getRuta() != null ? guardado.getRuta().toString() : "") + ").",
                motivoLimpio);
        return guardado;
    }

    // ==================== 6. CUPONES PROMOCIONALES (AUDITADOS) ====================

    public List<Cupon> listarCupones() {
        return cuponRepository.findAllByOrderByIdCuponDesc();
    }

    @Transactional
    public Cupon guardarCupon(Integer idCupon, CuponRequest req, SesionUsuario adminActual) {
        if (req == null) throw new ReglaNegocioException("Datos del cupón obligatorios.");
        String codigo = requerido(req.codigo(), "El código del cupón es obligatorio.").toUpperCase();
        if (req.montoDescuento() == null || req.montoDescuento().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException("El monto de descuento debe ser mayor a S/ 0.");
        }

        boolean esNuevo = idCupon == null;
        Cupon c = !esNuevo
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
        Cupon guardado = cuponRepository.saveAndFlush(c);
        auditar(adminActual, "CUPONES", esNuevo ? "CREAR" : "EDITAR", "Cupon", guardado.getCodigo(),
                (esNuevo ? "Creó cupón " : "Actualizó cupón ") + guardado.getCodigo() + " (- S/ " + guardado.getMontoDescuento() + ").",
                null);
        return guardado;
    }

    @Transactional
    public Cupon cambiarEstadoCupon(Integer idCupon, CambioEstadoAuditadoRequest req, SesionUsuario adminActual) {
        Cupon c = cuponRepository.findById(idCupon)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cupón no encontrado."));
        boolean nuevo = req != null && req.activo() != null ? req.activo() : !Boolean.TRUE.equals(c.getActivo());
        if (!nuevo) {
            requerido(req != null ? req.motivo() : null, "Ingresa el motivo de auditoría para desactivar el cupón.");
        }
        c.setActivo(nuevo);
        Cupon guardado = cuponRepository.saveAndFlush(c);
        auditar(adminActual, "CUPONES", nuevo ? "REACTIVAR" : "BAJA_LOGICA", "Cupon", guardado.getCodigo(),
                (nuevo ? "Reactivó cupón " : "Desactivó cupón ") + guardado.getCodigo() + ".",
                req != null ? req.motivo() : null);
        return guardado;
    }

    private void auditar(SesionUsuario actor, String modulo, String accion, String entidad, Object id, String desc, String motivo) {
        if (auditoriaService != null) {
            auditoriaService.registrar(actor, modulo, accion, entidad, id, desc, motivo);
        }
    }

    private String requerido(String valor, String mensaje) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new ReglaNegocioException(mensaje);
        }
        return valor.trim();
    }
}
