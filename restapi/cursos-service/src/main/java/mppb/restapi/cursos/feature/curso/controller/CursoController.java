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
import mppb.restapi.common.core.validation.groups.Update;
import mppb.restapi.cursos.feature.curso.dto.*;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.curso.service.CursoService;
import mppb.restapi.cursos.feature.frequencia.dto.AulaDto;
import mppb.restapi.cursos.feature.frequencia.dto.ChamadaDto;
import mppb.restapi.cursos.feature.frequencia.dto.FrequenciaDto;
import mppb.restapi.cursos.feature.frequencia.entity.Aula;
import mppb.restapi.cursos.feature.frequencia.entity.Chamada;
import mppb.restapi.cursos.feature.frequencia.service.AulaService;
import mppb.restapi.cursos.feature.frequencia.service.FrequenciaService;
import mppb.restapi.cursos.feature.inscricao.dto.AtualizarSituacaoDto;
import mppb.restapi.cursos.feature.inscricao.dto.InscricaoDto;
import mppb.restapi.cursos.feature.inscricao.entity.Inscricao;
import mppb.restapi.cursos.feature.inscricao.service.InscricaoService;
import mppb.restapi.cursos.feature.participante.dto.CriarInscricaoDto;
import mppb.restapi.common.infrastructure.util.Constants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Validated
@RestController
@RequestMapping(Constants.API_PREFIX + "/cursos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cursos", description = "Gerencia as operações relacionadas a cursos")
@PreAuthorize("hasAuthority('cursos.manter.cursos')")
public class CursoController extends AbstractController {

    private final CursoService cursoService;
    private final InscricaoService inscricaoService;
    private final AulaService aulaService;
    private final FrequenciaService frequenciaService;
    private final mppb.restapi.cursos.feature.frequencia.service.ChamadaService chamadaService;

    private final DtoConverter<CursoDto, Curso> cursoConverter;
    private final DtoConverter<AulaDto, Aula> aulaConverter;
    private final DtoConverter<ChamadaDto, Chamada> chamadaConverter;
    private final mppb.restapi.cursos.feature.curso.service.CursoDocumentoService cursoDocumentoService;



    // --- ENDPOINTS DE GESTÃO DE CURSOS ---
    @Operation(summary = "Lista os cursos", description = "Listamge dos cursos (paginados).")
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    @JsonView(CursoView.Listagem.class)
    public ResponseEntity<Page<CursoDto>> getPages(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) Long modalidadeId,
            @RequestParam(required = false) Long publicoAlvoId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "false") Boolean historico,
            Pageable pageable) {
        return ResponseEntity.ok(cursoService.findAll(titulo, modalidadeId, publicoAlvoId, status, historico, pageable).map(cursoConverter::toDto));
    }

    @Operation(summary = "Obtém curso", description = "Retorna informações do curso.")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
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
        return ResponseEntity.ok(cursoService.update(id, cursoDto));
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
     * @param cursoId      O ID do curso ao qual a associação pertence.
     * @param associacaoId O ID da associação (CursoMinistrante) a ser atualizada.
     * @param dto          O corpo da requisição contendo o ID do novo papel.
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

    @Operation(summary = "Obtém os participantes de um curso", description = "Retorna os participantes inscritos de forma paginada. Suporta filtros por nome e situação.")
    @GetMapping(value = "/{cursoId}/participantes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<InscricaoDto>> findAll(
            @PathVariable Long cursoId,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String situacao,
            Pageable pageable) {
        Page<Inscricao> inscricaoPage = inscricaoService.findAllByCursoId(cursoId, nome, situacao, pageable);
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
        InscricaoDto inscricaoAtualizada = inscricaoService.atualizarSituacao(cursoId, inscricaoId, dto.getSituacao(), dto.getFrequencia());
        return ResponseEntity.ok(inscricaoAtualizada);
    }

    @Operation(summary = "Remove um participante de um curso", description = "Remove a inscrição de um participante do curso.")
    @DeleteMapping("/{cursoId}/participantes/{inscricaoId}")
    public ResponseEntity<Void> removerInscricao(@PathVariable Long cursoId,
                                                 @PathVariable Long inscricaoId) {
        inscricaoService.remover(cursoId, inscricaoId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Exporta os participantes para Excel", description = "Gera um arquivo .xlsx com a lista de participantes filtrada.")
    @GetMapping(value = "/{cursoId}/participantes/export")
    public ResponseEntity<Resource> exportarParaExcel(
            @PathVariable Long cursoId,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String situacao) throws IOException {

        ByteArrayInputStream in = inscricaoService.exportarParaExcel(cursoId, nome, situacao);

        CursoDto curso = cursoService.findDetalhesById(cursoId);
        // Remove caracteres especiais para evitar problemas no nome do arquivo
        String nomeCurso = curso.getTitulo().replaceAll("[^a-zA-Z0-9]", "_");
        String fileName = "participantes_" + nomeCurso + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=" + fileName);

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }


    // ENDPOINTS PARA GESTÃO DE AULAS (FREQUÊNCIA) ---

    @Operation(summary = "Lista as aulas do curso", description = "Retorna a lista de aulas cadastradas para um curso, ordenadas cronologicamente.")
    @GetMapping("/{cursoId}/aulas")
    public ResponseEntity<List<AulaDto>> listarAulas(@PathVariable Long cursoId) {
        List<AulaDto> aulas = aulaService.findAllByCursoId(cursoId);
        return ResponseEntity.ok(aulas);
    }

    @Operation(summary = "Cria uma nova aula para o curso", description = "Adiciona uma nova aula com datas e horários definidos.")
    @PostMapping("/{cursoId}/aulas")
    public ResponseEntity<AulaDto> criarAula(@PathVariable Long cursoId, @RequestBody @Valid AulaDto dto) {
        Aula aula = aulaConverter.toEntity(dto);
        Aula aulaSalva = aulaService.createAula(cursoId, aula);
        return ResponseEntity.status(HttpStatus.CREATED).body(aulaConverter.toDto(aulaSalva));
    }

    @Operation(summary = "Atualiza uma aula existente")
    @PutMapping("/{cursoId}/aulas/{aulaId}")
    public ResponseEntity<AulaDto> atualizarAula(@PathVariable Long cursoId, @PathVariable Long aulaId, @RequestBody AulaDto aulaDto) {
        return aulaService.findById(aulaId)
                .map(aula -> {
                    aulaDto.setId(aulaId); // Garante que o ID do path é o mesmo do objeto

                    Aula from = aulaConverter.toEntity(aulaDto);

                    aulaConverter.map(from, aula);

                    Aula updatedEntity = aulaService.save(aula);
                    return ResponseEntity.ok(aulaConverter.toDto(updatedEntity));
                }).orElse(ResponseEntity.notFound().build());
       // AulaDto updatedDto = aulaService.update(cursoId, aulaId, aulaDto);
      //  return ResponseEntity.ok(updatedDto);
    }

    @Operation(summary = "Remove uma aula existente")
    @DeleteMapping("/{cursoId}/aulas/{aulaId}")
    public ResponseEntity<Object> removerAula(@PathVariable Long cursoId, @PathVariable Long aulaId) {
        aulaService.deleteAula(cursoId, aulaId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Gera uma nova chamada (QR Code) para a aula", description = "Cria uma nova Chamada com um token único para registo de presença.")
    @PostMapping("/{cursoId}/aulas/{aulaId}/chamadas")
    public ResponseEntity<ChamadaDto> gerarChamada(@PathVariable Long cursoId,
                                                   @PathVariable Long aulaId,
                                                   @RequestParam String tipo) {
        Chamada chamada = aulaService.adicionarChamada(aulaId, tipo);
        return ResponseEntity.status(HttpStatus.CREATED).body(chamadaConverter.toDto(chamada));
    }

    @Operation(summary = "Encerra uma chamada ativa", description = "Impede novos registros de frequência para o token desta chamada.")
    @PostMapping("/{cursoId}/aulas/{aulaId}/chamadas/{chamadaId}/encerrar")
    public ResponseEntity<ChamadaDto> encerrarChamada(@PathVariable Long cursoId,
                                                      @PathVariable Long aulaId,
                                                      @PathVariable Long chamadaId) {
        Chamada chamadaFechada = chamadaService.encerrarChamada(chamadaId);
        return ResponseEntity.ok(chamadaConverter.toDto(chamadaFechada));
    }

    @Operation(summary = "Remove uma chamada", description = "Remove a chamada, seus registros de frequência e recalcula as inscrições do curso.")
    @DeleteMapping("/{cursoId}/aulas/{aulaId}/chamadas/{chamadaId}")
    public ResponseEntity<Void> removerChamada(@PathVariable Long cursoId,
                                               @PathVariable Long aulaId,
                                               @PathVariable Long chamadaId) {
        aulaService.removerChamada(cursoId, aulaId, chamadaId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Lista as frequências de um aula", description = "Busca as frequências de uma determinada aula do curso")
    @GetMapping("/{cursoId}/aulas/{aulaId}/frequencia")
    public ResponseEntity<List<FrequenciaDto>> getFrequencia(
            @PathVariable Long cursoId,
            @PathVariable Long aulaId) {

        List<FrequenciaDto> frequencias = frequenciaService.findAllByAulaId(aulaId)
                .stream()
                .map(frequenciaService::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(frequencias);
    }

    @Operation(summary = "Remove um registro de frequência", description = "Remove uma presença específica e recalcula a situação da inscrição.")
    @DeleteMapping("/{cursoId}/aulas/{aulaId}/frequencia/{frequenciaId}")
    public ResponseEntity<Void> removerFrequencia(
            @PathVariable Long cursoId,
            @PathVariable Long aulaId,
            @PathVariable Long frequenciaId) {

        frequenciaService.remover(cursoId, aulaId, frequenciaId);
        return ResponseEntity.noContent().build();
    }

    // --- ENDPOINTS PARA DOCUMENTOS ---

    @Operation(summary = "Exporta a lista de presença em PDF", description = "Gera a lista de presença para todos os participantes do curso.")
    @GetMapping(value = "/{cursoId}/documentos/presenca", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> gerarListaPresenca(@PathVariable Long cursoId) {
        byte[] pdf = cursoDocumentoService.gerarListaPresenca(cursoId);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=lista_presenca_" + cursoId + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    @Operation(summary = "Exporta o ofício da Corregedoria em PDF", description = "Gera ofício listando apenas os Membros.")
    @GetMapping(value = "/{cursoId}/documentos/corregedoria", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> gerarOficioCorregedoria(@PathVariable Long cursoId) {
        byte[] pdf = cursoDocumentoService.gerarOficioCorregedoria(cursoId);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=oficio_corregedoria_" + cursoId + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    @Operation(summary = "Exporta o ofício do DRH em PDF", description = "Gera ofício listando todos os participantes (Membros, Servidores e Assessores).")
    @GetMapping(value = "/{cursoId}/documentos/drh", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> gerarOficioDrh(@PathVariable Long cursoId) {
        byte[] pdf = cursoDocumentoService.gerarOficioDrh(cursoId);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=oficio_drh_" + cursoId + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
