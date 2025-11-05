package br.gov.mppb.ceaf_backend.controller;

import br.gov.mppb.ceaf_backend.domain.EntidadeTutora;
import br.gov.mppb.ceaf_backend.service.EntidadeTutoraService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/entidades-tutoras")
public class EntidadeTutoraController {

    private final EntidadeTutoraService entidadeTutoraService;

    public EntidadeTutoraController(EntidadeTutoraService entidadeTutoraService) {
        this.entidadeTutoraService = entidadeTutoraService;
    }

    @GetMapping
    public List<EntidadeTutora> getAll() {
        return entidadeTutoraService.findAll();
    }

    @PostMapping
    public ResponseEntity<EntidadeTutora> create(@RequestBody EntidadeTutora entidadeTutora) {
        entidadeTutora.setId(null); // Garante a criação
        EntidadeTutora saved = entidadeTutoraService.save(entidadeTutora);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EntidadeTutora> update(@PathVariable Integer id, @RequestBody EntidadeTutora entidadeTutora) {
        entidadeTutora.setId(id); // Garante a atualização
        EntidadeTutora saved = entidadeTutoraService.save(entidadeTutora);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        entidadeTutoraService.delete(id);
        return ResponseEntity.noContent().build();
    }
}