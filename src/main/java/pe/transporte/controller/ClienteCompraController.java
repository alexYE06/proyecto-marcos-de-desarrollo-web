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
@RequestMapping("/api/cliente")
@RequiereRol({RolUsuario.CLIENTE, RolUsuario.VENDEDOR, RolUsuario.EMBARQUE, RolUsuario.ADMIN})
public class ClienteCompraController {

    private final VentaPasajeService ventaPasajeService;

    public ClienteCompraController(VentaPasajeService ventaPasajeService) {
        this.ventaPasajeService = ventaPasajeService;
    }

    @PostMapping("/reservas")
    public ResponseEntity<Map<String, Object>> reservarAsientosTemporalmente(
            HttpServletRequest request,
            @RequestBody ReservaTemporalRequest body) {
        SesionUsuario sesion = AuthInterceptor.sesionObligatoria(request);
        return ResponseEntity.ok(ventaPasajeService.reservarAsientosTemporalmente(body, sesion));
    }

    @PostMapping("/compras")
    public ResponseEntity<VentaDetalleResponse> comprarPasajesOnline(
            HttpServletRequest request,
            @RequestBody EmitirVentaRequest body) {
        SesionUsuario sesion = AuthInterceptor.sesionObligatoria(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ventaPasajeService.emitirVenta(body, CanalVenta.WEB, sesion));
    }

    @GetMapping("/mis-compras")
    public ResponseEntity<List<VentaDetalleResponse>> listarMisCompras(HttpServletRequest request) {
        SesionUsuario sesion = AuthInterceptor.sesionObligatoria(request);
        return ResponseEntity.ok(ventaPasajeService.listarComprasCliente(sesion.idUsuario()));
    }

    @GetMapping("/comprobantes/{idVenta}")
    public ResponseEntity<VentaDetalleResponse> obtenerComprobante(@PathVariable Integer idVenta,
                                                                   HttpServletRequest request) {
        SesionUsuario sesion = AuthInterceptor.sesionObligatoria(request);
        return ResponseEntity.ok(ventaPasajeService.obtenerComprobanteAutorizado(idVenta, sesion));
    }
}
