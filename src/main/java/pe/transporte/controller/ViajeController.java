package pe.transporte.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.transporte.model.Viaje;
import pe.transporte.repository.ViajeRepository;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/viajes")
@CrossOrigin(origins = "*")
public class ViajeController {

    @Autowired
    private ViajeRepository viajeRepository;

    @GetMapping
    public ResponseEntity<List<Viaje>> listarTodos() {
        return ResponseEntity.ok(viajeRepository.findAll());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<Viaje>> buscarViajes(
            @RequestParam(required = false) String origen,
            @RequestParam(required = false) String destino,
            @RequestParam(required = false) String fecha) {

        List<Viaje> todos = viajeRepository.findAll();

        if (origen == null && destino == null) {
            return ResponseEntity.ok(todos);
        }

        List<Viaje> filtrados = todos.stream().filter(v -> {
            boolean coincideOrigen = true;
            boolean coincideDestino = true;

            if (origen != null && !origen.trim().isEmpty() && v.getRuta() != null) {
                coincideOrigen = v.getRuta().toString().toLowerCase().contains(origen.toLowerCase());
            }
            if (destino != null && !destino.trim().isEmpty() && v.getRuta() != null) {
                coincideDestino = v.getRuta().toString().toLowerCase().contains(destino.toLowerCase());
            }

            return coincideOrigen && coincideDestino;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(filtrados);
    }
}