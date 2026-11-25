package pe.transporte.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.transporte.dto.AdminOperacionDtos.*;
import pe.transporte.dto.VentaDtos.MotivoAccionRequest;
import pe.transporte.dto.VentaDtos.VentaDetalleResponse;
import pe.transporte.model.*;
import pe.transporte.model.enums.RolUsuario;
import pe.transporte.security.AuthInterceptor;
import pe.transporte.security.RequiereRol;
import pe.transporte.security.SesionUsuario;
import pe.transporte.service.AdminGestionService;
import pe.transporte.service.AuditoriaService;
import pe.transporte.service.VentaPasajeService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiereRol(RolUsuario.ADMIN)
public class AdminGestionController {

    private final AdminGestionService adminGestionService;
    private final VentaPasajeService ventaPasajeService;
    private final AuditoriaService auditoriaService;

    public AdminGestionController(AdminGestionService adminGestionService,
                                  VentaPasajeService ventaPasajeService,
                                  AuditoriaService auditoriaService) {
        this.adminGestionService = adminGestionService;
        this.ventaPasajeService = ventaPasajeService;
        this.auditoriaService = auditoriaService;
    }

    // ==================== DASHBOARD Y BITÁCORA INMUTABLE DE AUDITORÍA ====================

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> obtenerDashboard() {
        return ResponseEntity.ok(adminGestionService.obtenerDashboardEjecutivo());
    }

    @GetMapping("/auditoria")
    public ResponseEntity<List<Map<String, Object>>> listarAuditoria(
            @RequestParam(required = false) String modulo,
            @RequestParam(required = false) String accion) {
        return ResponseEntity.ok(auditoriaService.listarEventos(modulo, accion));
    }

    // ==================== AUDITORÍA DE VENTAS Y ANULACIONES ====================

    @GetMapping("/ventas")
    public ResponseEntity<List<VentaDetalleResponse>> listarVentasGlobal(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String canal) {
        return ResponseEntity.ok(ventaPasajeService.listarTodasLasVentasAdmin(estado, canal));
    }

    @PostMapping("/ventas/{idVenta}/anular")
    public ResponseEntity<VentaDetalleResponse> anularVenta(
            @PathVariable Integer idVenta,
            @RequestBody(required = false) MotivoAccionRequest body,
            HttpServletRequest request) {
        SesionUsuario sesion = AuthInterceptor.sesionObligatoria(request);
        return ResponseEntity.ok(ventaPasajeService.aprobarOEjecutarAnulacionAdmin(
                idVenta, body != null ? body.motivo() : null, sesion));
    }

    @PostMapping("/ventas/{idVenta}/rechazar-anulacion")
    public ResponseEntity<VentaDetalleResponse> rechazarSolicitudAnulacion(
            @PathVariable Integer idVenta,
            @RequestBody(required = false) MotivoAccionRequest body,
            HttpServletRequest request) {
        SesionUsuario sesion = AuthInterceptor.sesionObligatoria(request);
        return ResponseEntity.ok(ventaPasajeService.rechazarSolicitudAnulacionAdmin(
                idVenta, body != null ? body.motivo() : null, sesion));
    }

    // ==================== USUARIOS Y ROLES ====================

    @GetMapping("/usuarios")
    public ResponseEntity<List<Usuario>> listarUsuarios(@RequestParam(required = false) String rol) {
        return ResponseEntity.ok(adminGestionService.listarUsuarios(rol));
    }

    @PostMapping("/usuarios")
    public ResponseEntity<Usuario> crearUsuario(@RequestBody CrearUsuarioInternoRequest body,
                                                HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminGestionService.crearUsuarioConRol(body, AuthInterceptor.sesionObligatoria(request)));
    }

    @PutMapping("/usuarios/{id}")
    public ResponseEntity<Usuario> actualizarUsuario(@PathVariable Integer id,
                                                     @RequestBody ActualizarUsuarioAdminRequest body,
                                                     HttpServletRequest request) {
        SesionUsuario sesion = AuthInterceptor.sesionObligatoria(request);
        return ResponseEntity.ok(adminGestionService.actualizarUsuarioYRol(id, body, sesion));
    }

    // ==================== EMPLEADOS / TRIPULACIÓN ====================

    @GetMapping("/empleados")
    public ResponseEntity<List<Empleado>> listarEmpleados() {
        return ResponseEntity.ok(adminGestionService.listarEmpleados());
    }

    @PostMapping("/empleados")
    public ResponseEntity<Empleado> crearEmpleado(@RequestBody EmpleadoRequest body, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminGestionService.guardarEmpleado(null, body, AuthInterceptor.sesionObligatoria(request)));
    }

    @PutMapping("/empleados/{id}")
    public ResponseEntity<Empleado> actualizarEmpleado(@PathVariable Integer id,
                                                       @RequestBody EmpleadoRequest body,
                                                       HttpServletRequest request) {
        return ResponseEntity.ok(adminGestionService.guardarEmpleado(id, body, AuthInterceptor.sesionObligatoria(request)));
    }

    @PatchMapping("/empleados/{id}/estado")
    public ResponseEntity<Empleado> cambiarEstadoEmpleado(@PathVariable Integer id,
                                                          @RequestBody CambioEstadoAuditadoRequest body,
                                                          HttpServletRequest request) {
        return ResponseEntity.ok(adminGestionService.cambiarEstadoEmpleado(id, body, AuthInterceptor.sesionObligatoria(request)));
    }

    // ==================== FLOTA DE BUSES ====================

    @GetMapping("/buses")
    public ResponseEntity<List<Bus>> listarBuses() {
        return ResponseEntity.ok(adminGestionService.listarBuses());
    }

    @PostMapping("/buses")
    public ResponseEntity<Bus> crearBus(@RequestBody BusRequest body, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminGestionService.guardarBus(null, body, AuthInterceptor.sesionObligatoria(request)));
    }

    @PutMapping("/buses/{id}")
    public ResponseEntity<Bus> actualizarBus(@PathVariable Integer id,
                                             @RequestBody BusRequest body,
                                             HttpServletRequest request) {
        return ResponseEntity.ok(adminGestionService.guardarBus(id, body, AuthInterceptor.sesionObligatoria(request)));
    }

    @PatchMapping("/buses/{id}/estado")
    public ResponseEntity<Bus> cambiarEstadoBus(@PathVariable Integer id,
                                                @RequestBody CambioEstadoAuditadoRequest body,
                                                HttpServletRequest request) {
        return ResponseEntity.ok(adminGestionService.cambiarEstadoBusAuditado(id, body, AuthInterceptor.sesionObligatoria(request)));
    }

    // ==================== TERMINALES, RUTAS Y VIAJES ====================

    @GetMapping("/terminales")
    public ResponseEntity<List<Terminal>> listarTerminales() {
        return ResponseEntity.ok(adminGestionService.listarTerminales());
    }

    @PostMapping("/terminales")
    public ResponseEntity<Terminal> crearTerminal(@RequestBody TerminalRequest body, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminGestionService.guardarTerminal(null, body, AuthInterceptor.sesionObligatoria(request)));
    }

    @PutMapping("/terminales/{id}")
    public ResponseEntity<Terminal> actualizarTerminal(@PathVariable Integer id,
                                                       @RequestBody TerminalRequest body,
                                                       HttpServletRequest request) {
        return ResponseEntity.ok(adminGestionService.guardarTerminal(id, body, AuthInterceptor.sesionObligatoria(request)));
    }

    @PatchMapping("/terminales/{id}/estado")
    public ResponseEntity<Terminal> cambiarEstadoTerminal(@PathVariable Integer id,
                                                          @RequestBody CambioEstadoAuditadoRequest body,
                                                          HttpServletRequest request) {
        return ResponseEntity.ok(adminGestionService.cambiarEstadoTerminal(id, body, AuthInterceptor.sesionObligatoria(request)));
    }

    @GetMapping("/rutas")
    public ResponseEntity<List<Ruta>> listarRutas() {
        return ResponseEntity.ok(adminGestionService.listarRutas());
    }

    @PostMapping("/rutas")
    public ResponseEntity<Ruta> crearRuta(@RequestBody RutaRequest body, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminGestionService.guardarRuta(null, body, AuthInterceptor.sesionObligatoria(request)));
    }

    @PutMapping("/rutas/{id}")
    public ResponseEntity<Ruta> actualizarRuta(@PathVariable Integer id,
                                               @RequestBody RutaRequest body,
                                               HttpServletRequest request) {
        return ResponseEntity.ok(adminGestionService.guardarRuta(id, body, AuthInterceptor.sesionObligatoria(request)));
    }

    @PatchMapping("/rutas/{id}/estado")
    public ResponseEntity<Ruta> cambiarEstadoRuta(@PathVariable Integer id,
                                                  @RequestBody CambioEstadoAuditadoRequest body,
                                                  HttpServletRequest request) {
        return ResponseEntity.ok(adminGestionService.cambiarEstadoRuta(id, body, AuthInterceptor.sesionObligatoria(request)));
    }

    @GetMapping("/viajes")
    public ResponseEntity<List<Viaje>> listarViajes() {
        return ResponseEntity.ok(adminGestionService.listarViajes());
    }

    @PostMapping("/viajes")
    public ResponseEntity<Viaje> crearViaje(@RequestBody ViajeRequest body, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminGestionService.guardarViaje(null, body, AuthInterceptor.sesionObligatoria(request)));
    }

    @PutMapping("/viajes/{id}")
    public ResponseEntity<Viaje> actualizarViaje(@PathVariable Integer id,
                                                 @RequestBody ViajeRequest body,
                                                 HttpServletRequest request) {
        return ResponseEntity.ok(adminGestionService.guardarViaje(id, body, AuthInterceptor.sesionObligatoria(request)));
    }

    @PostMapping("/viajes/{id}/cancelar")
    public ResponseEntity<Viaje> cancelarViaje(@PathVariable Integer id,
                                               @RequestBody MotivoAccionRequest body,
                                               HttpServletRequest request) {
        return ResponseEntity.ok(adminGestionService.cancelarViajeAuditado(
                id, body != null ? body.motivo() : null, AuthInterceptor.sesionObligatoria(request)));
    }

    // ==================== CUPONES PROMOCIONALES ====================

    @GetMapping("/cupones")
    public ResponseEntity<List<Cupon>> listarCupones() {
        return ResponseEntity.ok(adminGestionService.listarCupones());
    }

    @PostMapping("/cupones")
    public ResponseEntity<Cupon> crearCupon(@RequestBody CuponRequest body, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminGestionService.guardarCupon(null, body, AuthInterceptor.sesionObligatoria(request)));
    }

    @PutMapping("/cupones/{id}")
    public ResponseEntity<Cupon> actualizarCupon(@PathVariable Integer id,
                                                 @RequestBody CuponRequest body,
                                                 HttpServletRequest request) {
        return ResponseEntity.ok(adminGestionService.guardarCupon(id, body, AuthInterceptor.sesionObligatoria(request)));
    }

    @PatchMapping("/cupones/{id}/estado")
    public ResponseEntity<Cupon> cambiarEstadoCupon(@PathVariable Integer id,
                                                    @RequestBody CambioEstadoAuditadoRequest body,
                                                    HttpServletRequest request) {
        return ResponseEntity.ok(adminGestionService.cambiarEstadoCupon(id, body, AuthInterceptor.sesionObligatoria(request)));
    }
}
