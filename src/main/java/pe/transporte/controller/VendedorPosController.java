package pe.transporte.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.transporte.dto.VentaDtos.*;
import pe.transporte.model.enums.CanalVenta;
import pe.transporte.model.enums.RolUsuario;
import pe.transporte.security.AuthInterceptor;
import pe.transporte.security.RequiereRol;
import pe.transporte.security.SesionUsuario;
import pe.transporte.service.VentaPasajeService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vendedor")
@RequiereRol({RolUsuario.VENDEDOR, RolUsuario.ADMIN})
public class VendedorPosController {

    private final VentaPasajeService ventaPasajeService;

    public VendedorPosController(VentaPasajeService ventaPasajeService) {
        this.ventaPasajeService = ventaPasajeService;
    }

    @PostMapping("/ventas")
    public ResponseEntity<VentaDetalleResponse> registrarVentaMostrador(
            HttpServletRequest request,
            @RequestBody EmitirVentaRequest body) {
        SesionUsuario sesion = AuthInterceptor.sesionObligatoria(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ventaPasajeService.emitirVenta(body, CanalVenta.VENTANILLA, sesion));
    }

    @GetMapping("/mis-ventas")
    public ResponseEntity<List<VentaDetalleResponse>> listarMisVentasMostrador(HttpServletRequest request) {
        SesionUsuario sesion = AuthInterceptor.sesionObligatoria(request);
        return ResponseEntity.ok(ventaPasajeService.listarVentasVendedor(sesion.idUsuario()));
    }

    @GetMapping("/arqueo")
    public ResponseEntity<Map<String, Object>> obtenerArqueoCaja(HttpServletRequest request) {
        SesionUsuario sesion = AuthInterceptor.sesionObligatoria(request);
        return ResponseEntity.ok(ventaPasajeService.obtenerArqueoCajaVendedor(sesion.idUsuario()));
    }

    @PostMapping("/ventas/{idVenta}/solicitar-anulacion")
    public ResponseEntity<VentaDetalleResponse> solicitarAnulacion(
            @PathVariable Integer idVenta,
            @RequestBody MotivoAccionRequest body,
            HttpServletRequest request) {
        SesionUsuario sesion = AuthInterceptor.sesionObligatoria(request);
        return ResponseEntity.ok(ventaPasajeService.solicitarAnulacionPorVendedor(
                idVenta, body != null ? body.motivo() : null, sesion));
    }
}
