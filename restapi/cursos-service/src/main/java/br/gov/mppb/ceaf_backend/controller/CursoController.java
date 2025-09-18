package br.gov.mppb.ceaf_backend.controller;

import br.gov.mppb.ceaf_backend.domain.Curso;
import br.gov.mppb.ceaf_backend.repository.CursoRepository;
import br.gov.mppb.ceaf_backend.service.CursoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cursos")
public class CursoController {

    private final CursoRepository cursoRepository;
    private final CursoService cursoService;

    public CursoController(CursoRepository cursoRepository,  CursoService cursoService) {
        this.cursoRepository = cursoRepository;
        this.cursoService = cursoService;
    }

    @GetMapping
    public List<Curso> listarCursos() {
        return cursoRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Curso> getCurso(@PathVariable Long id) {
        return cursoService.findCompletoById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Curso> criarCurso(@RequestBody Curso curso) {
        curso.setId(null);
        Curso novoCurso = cursoService.salvar(curso);
        return new ResponseEntity<>(novoCurso, HttpStatus.CREATED);
    }


    @PutMapping("/{id}")
    public ResponseEntity<Curso> atualizarCurso(@PathVariable Long id, @RequestBody Curso cursoDetails) {
        return cursoRepository.findById(id)
                .map(cursoExistente -> {
                    cursoDetails.setId(id); // Garante que o ID seja o da URL
                    Curso cursoAtualizado = cursoService.salvar(cursoDetails);
                    return ResponseEntity.ok(cursoAtualizado);
                }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarCurso(@PathVariable Long id) {
        if (!cursoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        cursoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}