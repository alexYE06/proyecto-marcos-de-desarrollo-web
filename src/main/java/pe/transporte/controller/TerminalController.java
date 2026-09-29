package pe.transporte.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.transporte.model.Terminal;
import pe.transporte.repository.TerminalRepository;

import java.util.List;

@RestController
@RequestMapping("/api/terminales")
@CrossOrigin(origins = "*")
public class TerminalController {

    @Autowired
    private TerminalRepository terminalRepository;

    @GetMapping
    public ResponseEntity<List<Terminal>> listarTerminales() {
        return ResponseEntity.ok(terminalRepository.findAll());
    }
}