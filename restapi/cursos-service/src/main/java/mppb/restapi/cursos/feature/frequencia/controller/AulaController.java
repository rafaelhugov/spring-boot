package mppb.restapi.cursos.feature.frequencia.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mppb.restapi.cursos.feature.frequencia.converter.AulaConverter;
import mppb.restapi.cursos.feature.frequencia.dto.AulaDto;
import mppb.restapi.cursos.feature.frequencia.entity.Aula;
import mppb.restapi.cursos.feature.frequencia.service.AulaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cursos/{cursoId}/aulas")
@Tag(name = "Aulas", description = "Endpoints para gerenciamento de aulas dos cursos")
public class AulaController {

    private final AulaService aulaService;
    private final AulaConverter aulaConverter;

    public AulaController(AulaService aulaService, AulaConverter aulaConverter) {
        this.aulaService = aulaService;
        this.aulaConverter = aulaConverter;
    }

    @GetMapping
    @Operation(summary = "Listar aulas de um curso", description = "Retorna todas as aulas cadastradas para o curso informado.")
    public ResponseEntity<List<AulaDto>> listarAulas(@PathVariable Long cursoId) {
        return ResponseEntity.ok(aulaService.findAllByCursoId(cursoId));
    }

    @PostMapping
    @Operation(summary = "Criar nova aula", description = "Cria uma nova aula para o curso e recalcula a carga horária e o status do curso.")
    public ResponseEntity<AulaDto> criarAula(@PathVariable Long cursoId, @Valid @RequestBody AulaDto aulaDto) {
        Aula aula = aulaConverter.toEntity(aulaDto);
        Aula novaAula = aulaService.createAula(cursoId, aula);
        return new ResponseEntity<>(aulaConverter.toDto(novaAula), HttpStatus.CREATED);
    }

    @PutMapping("/{aulaId}")
    @Operation(summary = "Atualizar aula", description = "Atualiza os dados de uma aula existente e recalcula a carga horária e o status do curso.")
    public ResponseEntity<AulaDto> atualizarAula(@PathVariable Long cursoId, @PathVariable Long aulaId, @Valid @RequestBody AulaDto aulaDto) {
        Aula aulaAtualizada = aulaConverter.toEntity(aulaDto);
        Aula aulaSalva = aulaService.updateAula(cursoId, aulaId, aulaAtualizada);
        return ResponseEntity.ok(aulaConverter.toDto(aulaSalva));
    }

    @DeleteMapping("/{aulaId}")
    @Operation(summary = "Excluir aula", description = "Exclui uma aula e recalcula a carga horária e o status do curso.")
    public ResponseEntity<Void> excluirAula(@PathVariable Long cursoId, @PathVariable Long aulaId) {
        aulaService.deleteAula(cursoId, aulaId);
        return ResponseEntity.noContent().build();
    }
}
