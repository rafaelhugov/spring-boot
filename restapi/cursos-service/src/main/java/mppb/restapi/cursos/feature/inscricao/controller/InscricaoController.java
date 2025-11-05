package mppb.restapi.cursos.feature.inscricao.controller;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.converter.DtoConverter;
import mppb.restapi.cursos.feature.curso.dto.CursoDto;
import mppb.restapi.cursos.feature.curso.dto.CursoView;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.inscricao.dto.AtualizarSituacaoDto;
import mppb.restapi.cursos.feature.inscricao.dto.InscricaoDto;
import mppb.restapi.cursos.feature.inscricao.entity.Inscricao;
import mppb.restapi.cursos.feature.inscricao.service.InscricaoService;
import mppb.restapi.cursos.feature.participante.dto.CriarInscricaoDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cursos/{cursoId}/inscricoes")
@RequiredArgsConstructor
@Slf4j
// TODO mover metodos relativos a cursos para o cursoController
// TODO nome dessa classe está confundindo com InscricaoAutenticadaController, ao mover os métodos, considerar unifcá-las
@PreAuthorize("hasAuthority('cursos.manter.cursos')")
public class InscricaoController {

    private final InscricaoService inscricaoService;


    @Operation(summary = "Obtém inscricoes", description = "Retorna inscricoes paginados.")
    @GetMapping(value="", produces =  MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<InscricaoDto>> findAll(@PathVariable Long cursoId, Pageable pageable) {
        Page<Inscricao> inscricaoPage = inscricaoService.findAllByCursoId(cursoId, pageable);
        return ResponseEntity.ok(inscricaoPage.map(inscricaoService::toDto));
    }

    @PostMapping
    public ResponseEntity<InscricaoDto> inscreverParticipante(@PathVariable Long cursoId,
                                                              @Valid @RequestBody CriarInscricaoDto dto) {
        InscricaoDto inscricaoCriadaDto = inscricaoService.inscrever(cursoId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(inscricaoCriadaDto);
    }

    /**
     * Atualiza a situação de uma inscrição específica.
     * PATCH pois é uma atualização parcial do recurso.
     */
    @PatchMapping("/{inscricaoId}")
    public ResponseEntity<InscricaoDto> atualizarSituacao(@PathVariable Long cursoId,
                                                          @PathVariable Long inscricaoId,
                                                          @Valid @RequestBody AtualizarSituacaoDto dto) {
        InscricaoDto inscricaoAtualizada = inscricaoService.atualizarSituacao(cursoId, inscricaoId, dto.getSituacao());
        return ResponseEntity.ok(inscricaoAtualizada);
    }

    @DeleteMapping("/{inscricaoId}")
    public ResponseEntity<Void> removerInscricao(@PathVariable Long cursoId,
                                                 @PathVariable Long inscricaoId) {
        inscricaoService.remover(cursoId, inscricaoId);
        return ResponseEntity.noContent().build();
    }
}

