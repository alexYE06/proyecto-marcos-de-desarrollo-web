package pe.transporte.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.transporte.model.VentaAdmin;
import pe.transporte.repository.VentaAdminRepository;
import pe.transporte.util.AdminTokenStore;
import pe.transporte.util.TarifasAsientos;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/ventas")
@CrossOrigin(origins = "*")
public class VentaAdminController {

    private static final Set<String> METODOS = Set.of("TARJETA", "PAYPAL", "YAPE", "EFECTIVO");

    @Autowired
    private VentaAdminRepository ventaAdminRepository;

    @Autowired
    private AdminTokenStore tokenStore;

    @GetMapping
    public ResponseEntity<?> listar(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        if (!tokenStore.esValido(token)) return noAutorizado();
        return ResponseEntity.ok(ventaAdminRepository.findAllByOrderByFechaDescIdVentaAdminDesc());
    }

    @PostMapping
    public ResponseEntity<?> registrar(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                       @RequestBody VentaAdmin venta) {
        if (!tokenStore.esValido(token)) return noAutorizado();

        Map<String, Object> respuesta = new HashMap<>();

        if (venta.getFecha() == null) {
            return error(respuesta, "La fecha es obligatoria.");
        }

        // 1. Validar y normalizar los asientos
        List<Integer> asientos;
        try {
            asientos = parsearAsientos(venta.getAsientos());
        } catch (IllegalArgumentException e) {
            return error(respuesta, e.getMessage());
        }

        if (asientos.isEmpty()) {
            return error(respuesta, "Selecciona al menos un asiento.");
        }
        if (asientos.size() > TarifasAsientos.MAX_POR_VENTA) {
            return error(respuesta, "Máximo " + TarifasAsientos.MAX_POR_VENTA + " asientos por venta.");
        }
        for (int num : asientos) {
            if (num < 1 || num > TarifasAsientos.TOTAL_ASIENTOS) {
                return error(respuesta, "El asiento " + num + " no existe.");
            }
            if (TarifasAsientos.OCUPADOS_BASE.contains(num)) {
                return error(respuesta, "El asiento " + num + " está ocupado.");
            }
        }

        // 2. Que ninguno ya se haya vendido para esa fecha
        Set<Integer> vendidos = new HashSet<>();
        for (VentaAdmin existente : ventaAdminRepository.findByFecha(venta.getFecha())) {
            try {
                vendidos.addAll(parsearAsientos(existente.getAsientos()));
            } catch (IllegalArgumentException ignorado) {
                // registro antiguo con formato inesperado: se omite
            }
        }
        List<Integer> repetidos = asientos.stream().filter(vendidos::contains).collect(Collectors.toList());
        if (!repetidos.isEmpty()) {
            return error(respuesta, "Ya se vendieron para esa fecha los asientos: " + repetidos);
        }

        // 3. Método de pago
        if (venta.getMetodoPago() == null || !METODOS.contains(venta.getMetodoPago())) {
            venta.setMetodoPago("EFECTIVO");
        }

        // 4. Cantidad y precio total los calcula el servidor (no se confía en el navegador)
        int total = asientos.stream().mapToInt(TarifasAsientos::precio).sum();
        venta.setAsientos(asientos.stream().map(String::valueOf).collect(Collectors.joining(",")));
        venta.setCantidad(asientos.size());
        venta.setPrecioTotal(BigDecimal.valueOf(total).setScale(2));
        venta.setIdVentaAdmin(null); // siempre es un registro nuevo

        try {
            VentaAdmin guardada = ventaAdminRepository.saveAndFlush(venta);
            respuesta.put("mensaje", "Venta registrada correctamente.");
            respuesta.put("idVentaAdmin", guardada.getIdVentaAdmin());
            return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
        } catch (Exception e) {
            e.printStackTrace();
            respuesta.put("mensaje", "Error al guardar en base de datos: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(respuesta);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                      @PathVariable Integer id) {
        if (!tokenStore.esValido(token)) return noAutorizado();

        Map<String, Object> respuesta = new HashMap<>();
        if (!ventaAdminRepository.existsById(id)) {
            respuesta.put("mensaje", "La venta no existe.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);
        }
        ventaAdminRepository.deleteById(id);
        respuesta.put("mensaje", "Venta eliminada.");
        return ResponseEntity.ok(respuesta);
    }

    // "3, 4,21" -> [3, 4, 21] (ordenado y sin repetidos). Lanza IllegalArgumentException si el formato es inválido.
    private List<Integer> parsearAsientos(String texto) {
        if (texto == null || texto.trim().isEmpty()) return new ArrayList<>();
        TreeSet<Integer> resultado = new TreeSet<>();
        for (String parte : texto.split(",")) {
            try {
                if (!resultado.add(Integer.parseInt(parte.trim()))) {
                    throw new IllegalArgumentException("Hay asientos repetidos en la venta.");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("La lista de asientos no es válida.");
            }
        }
        return new ArrayList<>(resultado);
    }

    private ResponseEntity<?> error(Map<String, Object> respuesta, String mensaje) {
        respuesta.put("mensaje", mensaje);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta);
    }

    private ResponseEntity<?> noAutorizado() {
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Sesión de administrador no válida o expirada.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(respuesta);
    }
}
