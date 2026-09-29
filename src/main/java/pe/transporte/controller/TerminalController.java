package pe.transporte.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.transporte.model.Terminal;
import pe.transporte.repository.TerminalRepository;

import java.util.List;

@RestController
@RequestMapping("/api/terminales")
public class TerminalController {

    @Autowired
    private TerminalRepository terminalRepository;

    // Endpoint al que llamará tu JavaScript: http://localhost:8080/api/terminales
    @GetMapping
    public List<Terminal> listarTerminales() {
        return terminalRepository.findAll();
    }
}