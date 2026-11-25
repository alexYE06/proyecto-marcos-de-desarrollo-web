package pe.transporte.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.transporte.dto.AdminOperacionDtos.CambiarEstadoViajeRequest;
import pe.transporte.dto.AdminOperacionDtos.ValidarAbordajeRequest;
import pe.transporte.model.enums.RolUsuario;
import pe.transporte.security.AuthInterceptor;
import pe.transporte.security.RequiereRol;
import pe.transporte.security.SesionUsuario;
import pe.transporte.service.EmbarqueService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/embarque")
@RequiereRol({RolUsuario.EMBARQUE, RolUsuario.ADMIN})
public class EmbarqueController {

    private final EmbarqueService embarqueService;

    public EmbarqueController(EmbarqueService embarqueService) {
        this.embarqueService = embarqueService;
    }

    @GetMapping("/viajes")
    public ResponseEntity<List<Map<String, Object>>> listarViajesOperativos(
            @RequestParam(required = false) String fecha) {
        return ResponseEntity.ok(embarqueService.listarViajesOperativos(fecha));
    }

    @GetMapping("/viajes/{idViaje}/manifiesto")
    public ResponseEntity<Map<String, Object>> obtenerManifiesto(@PathVariable Integer idViaje) {
        return ResponseEntity.ok(embarqueService.obtenerManifiestoViaje(idViaje));
    }

    @PostMapping("/abordaje/validar")
    public ResponseEntity<Map<String, Object>> validarYAbordar(
            @RequestBody ValidarAbordajeRequest body,
            HttpServletRequest request) {
        SesionUsuario sesion = AuthInterceptor.sesionObligatoria(request);
        return ResponseEntity.ok(embarqueService.validarYRegistrarAbordaje(body, sesion));
    }

    @PatchMapping("/viajes/{idViaje}/estado")
    public ResponseEntity<Map<String, Object>> cambiarEstadoViaje(
            @PathVariable Integer idViaje,
            @RequestBody CambiarEstadoViajeRequest body,
            HttpServletRequest request) {
        SesionUsuario sesion = AuthInterceptor.sesionObligatoria(request);
        return ResponseEntity.ok(embarqueService.cambiarEstadoOperativoViaje(
                idViaje, body != null ? body.estado() : null, sesion));
    }
}
