package mppb.restapi.cursos.feature.curso.controller;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.controller.AbstractController;
import mppb.restapi.common.core.converter.DtoConverter;
import mppb.restapi.common.core.error.NotFoundException;
import mppb.restapi.common.core.validation.groups.Update;
import mppb.restapi.common.infrastructure.security.authorization.TokenAuthentication;
import mppb.restapi.cursos.feature.curso.dto.*;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.curso.service.CursoService;
import mppb.restapi.cursos.feature.inscricao.dto.AtualizarSituacaoDto;
import mppb.restapi.cursos.feature.inscricao.dto.InscricaoDto;
import mppb.restapi.cursos.feature.inscricao.entity.Inscricao;
import mppb.restapi.cursos.feature.inscricao.service.InscricaoService;
import mppb.restapi.cursos.feature.participante.dto.CriarInscricaoDto;
import mppb.restapi.cursos.infrastructure.util.Constants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Validated
@RestController
@RequestMapping(Constants.API_PREFIX + "/cursos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cursos", description = "Gerencia as operações relacionadas a cursos")
@PreAuthorize("hasAuthority('cursos.manter.cursos')")
public class CursoController extends AbstractController {

    private final CursoService cursoService;
    private final DtoConverter<CursoDto, Curso> cursoConverter;
    private final InscricaoService inscricaoService;

    // --- ENDPOINTS DE GESTÃO DE CURSOS ---
    @Operation(summary = "Lista os cursos", description = "Listamge dos cursos (paginados).")
    @GetMapping(value="", produces =  MediaType.APPLICATION_JSON_VALUE)
    @JsonView(CursoView.Listagem.class)
    public ResponseEntity<Page<CursoDto>> getPages(
                                                   @RequestParam(required = false) String titulo,
                                                   @RequestParam(required = false) Long modalidadeId,
                                                   @RequestParam(required = false) Long publicoAlvoId,
                                                   Pageable pageable) {
        return ResponseEntity.ok(cursoService.findAll(titulo, modalidadeId, publicoAlvoId, pageable).map(cursoConverter::toDto));
    }

    @Operation(summary = "Obtém curso", description = "Retorna informações do curso.")
    @GetMapping(value="/{id}", produces =  MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CursoDto> findDetalhesById(@PathVariable Long id) {
        CursoDto cursoDto = cursoService.findDetalhesById(id);
        return ResponseEntity.ok(cursoDto);
    }

    @Operation(summary = "Cadastra um curso", description = "Castrada um novo curso.")
    @PostMapping
    public ResponseEntity<CursoDto> create(@Validated({Default.class}) @RequestBody CursoDto cursoDto) {
        Curso createdEntity = cursoService.save(cursoConverter.toEntity(cursoDto));
        return new ResponseEntity<>(cursoConverter.toDto(createdEntity), HttpStatus.CREATED);
    }

    @Operation(summary = "Atualiza um curso", description = "Atualiza as informações de um curso.")
    @PutMapping("/{id}")
    public ResponseEntity<CursoDto> update(@PathVariable Long id, @Validated(Update.class) @RequestBody CursoDto cursoDto) {
        return cursoService.findById(id)
                .map(existing -> {
                    cursoDto.setId(id); // Garante que o ID do path é o mesmo do objeto
                    Curso updatedEntity = cursoService.save(cursoConverter.toEntity(cursoDto));
                    return ResponseEntity.ok(cursoConverter.toDto(updatedEntity));
                }).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Deleta um curso", description = "Deleta um curso (exclusão lógica).")
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable Long id) {
        return cursoService.findById(id)
                .map(curso -> {
                    cursoService.deleteById(id);
                    return ResponseEntity.noContent().build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // --- ENDPOINTS PARA GESTÃO DE MINISTRANTES DO CURSO ---

    @Operation(summary = "Lista os ministrantes de um curso", description = "Retorna a lista de ministrantes associados a um curso específico, com seus respectivos papéis.")
    @GetMapping(value = "/{cursoId}/ministrantes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CursoMinistranteDto>> getMinistrantesDoCurso(@PathVariable Long cursoId) {
        return ResponseEntity.ok(cursoService.findAllMinistrantesByCursoId(cursoId));
    }

    @Operation(summary = "Adiciona um ministrante a um curso", description = "Associa um ministrante existente a um curso, definindo seu papel.")
    @PostMapping(value = "/{cursoId}/ministrantes", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CursoMinistranteDto> addMinistranteAoCurso(@PathVariable Long cursoId, @Valid @RequestBody AssociarMinistranteDto request) { // <-- ATUALIZADO
        CursoMinistranteDto associacao = cursoService.addMinistranteToCurso(cursoId, request);
        return new ResponseEntity<>(associacao, HttpStatus.CREATED);
    }

    @Operation(summary = "Remove um ministrante de um curso", description = "Remove a associação de um ministrante com um curso.")
    @DeleteMapping("/{cursoId}/ministrantes/{associacaoId}")
    public ResponseEntity<Void> removeMinistranteDoCurso(@PathVariable Long cursoId, @PathVariable Long associacaoId) {
        cursoService.removeMinistranteFromCurso(cursoId, associacaoId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Atualiza o papel de um ministrante em uma associação específica com um curso.
     *
     * @param cursoId O ID do curso ao qual a associação pertence.
     * @param associacaoId O ID da associação (CursoMinistrante) a ser atualizada.
     * @param dto O corpo da requisição contendo o ID do novo papel.
     * @return Um ResponseEntity contendo o DTO da associação atualizada.
     */
    @Operation(summary = "Atualiza o papel de um ministrante em um curso", description = "Altera o papel de um ministrante em uma associação específica, retornando a associação atualizada.")
    @PutMapping("/{cursoId}/ministrantes/{associacaoId}")
    public ResponseEntity<CursoMinistranteDto> atualizarPapelDoMinistrante(
            @PathVariable Long cursoId,
            @PathVariable Long associacaoId,
            @RequestBody @Valid AtualizarPapelMinistranteDto dto) {

        CursoMinistranteDto associacaoAtualizada = cursoService.atualizarPapelMinistrante(cursoId, associacaoId, dto);
        return ResponseEntity.ok(associacaoAtualizada);
    }

    // --- ENDPOINTS PARA GESTÃO DE PARTICIPANTES DO CURSO ---

    @Operation(summary = "Obtém os participantes de um curso", description = "Retorna os participantes inscritos de forma paginada.")
    @GetMapping(value="/{cursoId}/participantes", produces =  MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<InscricaoDto>> findAll(@PathVariable Long cursoId, Pageable pageable) {
        Page<Inscricao> inscricaoPage = inscricaoService.findAllByCursoId(cursoId, pageable);
        return ResponseEntity.ok(inscricaoPage.map(inscricaoService::toDto));
    }

    @Operation(summary = "Inscreve um participante em um curso (Admin)", description = "Permite que um administrador inscreva manualmente um participante (interno ou externo) em um curso.")
    @PostMapping(value = "/{cursoId}/participantes")
    public ResponseEntity<InscricaoDto> inscreverParticipante(@PathVariable Long cursoId,
                                                              @Valid @RequestBody CriarInscricaoDto dto) {
        InscricaoDto inscricaoCriadaDto = inscricaoService.inscrever(cursoId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(inscricaoCriadaDto);
    }

    @Operation(summary = "Atualiza a situação de um participante", description = "Atualiza a situação de uma inscrição específica (ex: APROVADO, REPROVADO).")
    @PatchMapping("/{cursoId}/participantes/{inscricaoId}")
    public ResponseEntity<InscricaoDto> atualizarSituacao(@PathVariable Long cursoId,
                                                          @PathVariable Long inscricaoId,
                                                          @Valid @RequestBody AtualizarSituacaoDto dto) {
        InscricaoDto inscricaoAtualizada = inscricaoService.atualizarSituacao(cursoId, inscricaoId, dto.getSituacao());
        return ResponseEntity.ok(inscricaoAtualizada);
    }

    @Operation(summary = "Remove um participante de um curso", description = "Remove a inscrição de um participante do curso.")
    @DeleteMapping("/{cursoId}/participantes/{inscricaoId}")
    public ResponseEntity<Void> removerInscricao(@PathVariable Long cursoId,
                                                 @PathVariable Long inscricaoId) {
        inscricaoService.remover(cursoId, inscricaoId);
        return ResponseEntity.noContent().build();
    }
}
