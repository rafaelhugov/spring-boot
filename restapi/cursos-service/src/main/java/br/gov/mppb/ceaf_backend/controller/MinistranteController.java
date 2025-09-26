package br.gov.mppb.ceaf_backend.controller;

import br.gov.mppb.ceaf_backend.service.MinistranteService;
import br.gov.mppb.ceaf_backend.service.dto.MinistranteAddDTO;
import br.gov.mppb.ceaf_backend.service.dto.MinistranteCursoDTO;
import br.gov.mppb.ceaf_backend.service.dto.MinistranteUpdateDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cursos/{cursoId}/ministrantes")
public class MinistranteController {

    @Autowired
    private MinistranteService ministranteService;

    @GetMapping
    public ResponseEntity<List<MinistranteCursoDTO>> getAllByCurso(@PathVariable Long cursoId) {
        List<MinistranteCursoDTO> ministrantes = ministranteService.getAllByCurso(cursoId);
        return ResponseEntity.ok(ministrantes);
    }

    @PostMapping
    public ResponseEntity<MinistranteCursoDTO> addMinistranteToCurso(@PathVariable Long cursoId, @RequestBody MinistranteAddDTO dto) {
        MinistranteCursoDTO novoMinistrante = ministranteService.addMinistranteToCurso(cursoId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoMinistrante);
    }

    @DeleteMapping("/{ministranteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMinistranteFromCurso(@PathVariable Long cursoId, @PathVariable Long ministranteId) {
        // O `ministranteId` na URL corresponde ao ID da associação (`CursoMinistrante.id`)
        ministranteService.removeMinistranteFromCurso(cursoId, ministranteId);
    }

    @PutMapping("/{ministranteId}")
    public ResponseEntity<MinistranteCursoDTO> updateMinistrante(
            @PathVariable Long cursoId,
            @PathVariable Long ministranteId,
            @RequestBody MinistranteUpdateDTO dto) {
        // O `ministranteId` na URL corresponde ao ID da associação (`CursoMinistrante.id`)
        MinistranteCursoDTO ministranteAtualizado = ministranteService.updateMinistrante(cursoId, ministranteId, dto);
        return ResponseEntity.ok(ministranteAtualizado);
    }
}

