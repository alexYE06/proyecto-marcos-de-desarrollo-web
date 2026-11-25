package pe.transporte.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pe.transporte.model.Terminal;
import pe.transporte.model.Viaje;
import pe.transporte.repository.BusRepository;
import pe.transporte.service.CatalogoViajeService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador MVC con motor de plantillas Thymeleaf (SSR) para el portal principal
 * y la Cartelera Digital de Salidas en Vivo del Terrapuerto.
 */
@Controller
public class PortalThymeleafController {

    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CatalogoViajeService catalogoViajeService;
    private final BusRepository busRepository;

    public PortalThymeleafController(CatalogoViajeService catalogoViajeService,
                                     BusRepository busRepository) {
        this.catalogoViajeService = catalogoViajeService;
        this.busRepository = busRepository;
    }

    @GetMapping({"/", "/inicio", "/index.html"})
    public String mostrarPortadaPrincipal(Model model) {
        List<Terminal> terminales = catalogoViajeService.listarTerminalesActivos();
        Map<String, Terminal> porCiudad = new LinkedHashMap<>();
        for (Terminal t : terminales) {
            porCiudad.putIfAbsent(t.getCiudad(), t);
        }

        List<Viaje> proximos = catalogoViajeService.buscarViajes(null, null, null, false);
        long busesActivos = busRepository.findAll().stream()
                .filter(b -> "Activo".equalsIgnoreCase(b.getEstado()))
                .count();

        model.addAttribute("terminalesUnicos", porCiudad.values());
        model.addAttribute("totalTerminales", terminales.size());
        model.addAttribute("totalBusesActivos", busesActivos);
        model.addAttribute("totalSalidasProgramadas", proximos.size());
        model.addAttribute("horaServidor", LocalDateTime.now().format(FMT_HORA));
        return "index";
    }

    @GetMapping("/cartelera")
    public String mostrarCarteleraEnVivo(
            @RequestParam(required = false) String origen,
            Model model) {
        List<Terminal> terminales = catalogoViajeService.listarTerminalesActivos();
        List<String> ciudades = terminales.stream().map(Terminal::getCiudad).distinct().toList();
        List<Viaje> salidas = catalogoViajeService.buscarViajes(origen, null, null, true);

        model.addAttribute("ciudades", ciudades);
        model.addAttribute("origenSeleccionado", origen != null ? origen : "");
        model.addAttribute("salidas", salidas);
        model.addAttribute("actualizadoEn", LocalDateTime.now().format(FMT_HORA));
        return "cartelera";
    }
}
