package pe.transporte.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.transporte.dto.VentaDtos.MapaAsientosResponse;
import pe.transporte.model.Ruta;
import pe.transporte.model.Terminal;
import pe.transporte.model.Viaje;
import pe.transporte.security.AuthInterceptor;
import pe.transporte.security.SesionUsuario;
import pe.transporte.service.CatalogoViajeService;
import pe.transporte.service.VentaPasajeService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalogo")
public class CatalogoPublicoController {

    private final CatalogoViajeService catalogoViajeService;
    private final VentaPasajeService ventaPasajeService;

    public CatalogoPublicoController(CatalogoViajeService catalogoViajeService,
                                     VentaPasajeService ventaPasajeService) {
        this.catalogoViajeService = catalogoViajeService;
        this.ventaPasajeService = ventaPasajeService;
    }

    @GetMapping("/terminales")
    public ResponseEntity<List<Terminal>> listarTerminales() {
        return ResponseEntity.ok(catalogoViajeService.listarTerminalesActivos());
    }

    @GetMapping("/rutas")
    public ResponseEntity<List<Ruta>> listarRutas() {
        return ResponseEntity.ok(catalogoViajeService.listarRutasActivas());
    }

    @GetMapping("/viajes")
    public ResponseEntity<List<Viaje>> buscarViajes(
            @RequestParam(required = false) String origen,
            @RequestParam(required = false) String destino,
            @RequestParam(required = false) String fecha,
            @RequestParam(defaultValue = "false") boolean incluirHoyCompleto) {
        return ResponseEntity.ok(catalogoViajeService.buscarViajes(origen, destino, fecha, incluirHoyCompleto));
    }

    @GetMapping("/viajes/{idViaje}/asientos")
    public ResponseEntity<MapaAsientosResponse> obtenerMapaAsientos(@PathVariable Integer idViaje,
                                                                    HttpServletRequest request) {
        Integer idUsuario = AuthInterceptor.sesionOpcional(request)
                .map(SesionUsuario::idUsuario)
                .orElse(null);
        return ResponseEntity.ok(catalogoViajeService.obtenerMapaAsientos(idViaje, idUsuario));
    }

    @GetMapping("/cupones/validar")
    public ResponseEntity<Map<String, Object>> validarCupon(@RequestParam String codigo) {
        return ResponseEntity.ok(ventaPasajeService.validarCupon(codigo));
    }
}
