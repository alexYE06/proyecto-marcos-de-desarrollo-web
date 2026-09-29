package pe.transporte.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import pe.transporte.model.Viaje;
import pe.transporte.repository.ViajeRepository;

import java.util.List;

@RestController
@RequestMapping("/api/viajes")
public class ViajeController {

    @Autowired
    private ViajeRepository viajeRepository;

    @GetMapping("/buscar")
    public List<Viaje> buscarViajes(@RequestParam Integer origen, @RequestParam Integer destino) {
        return viajeRepository.buscarViajesPorRuta(origen, destino);
    }
}