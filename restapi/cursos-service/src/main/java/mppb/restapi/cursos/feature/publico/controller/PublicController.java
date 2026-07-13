package mppb.restapi.cursos.feature.publico.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mppb.restapi.cursos.feature.curso.dto.CursoPublicoDto;
import mppb.restapi.cursos.feature.curso.service.CursoService;
import mppb.restapi.cursos.feature.frequencia.converter.AulaConverter;
import mppb.restapi.cursos.feature.frequencia.dto.*;
import mppb.restapi.cursos.feature.frequencia.service.ChamadaService;
import mppb.restapi.cursos.feature.frequencia.service.FrequenciaService;
import mppb.restapi.cursos.feature.common.service.ArquivoService;
import mppb.restapi.cursos.feature.inscricao.dto.InscricaoDto;
import mppb.restapi.cursos.feature.inscricao.service.InscricaoService;
import mppb.restapi.cursos.feature.participante.dto.InscricaoPublicaDto;
import mppb.restapi.cursos.feature.participante.dto.ParticipanteVerificacaoDto;
import mppb.restapi.common.infrastructure.util.Constants;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Validated
@RestController
@RequestMapping(Constants.API_PREFIX + "/public")
@RequiredArgsConstructor
@Tag(name = "Público", description = "Endpoints públicos")
public class PublicController {

    private final CursoService cursoService;
    private final InscricaoService inscricaoService;
    private final ChamadaService chamadaService;
    private final FrequenciaService frequenciaService;
    private final ArquivoService arquivoService;
    private final ModelMapper modelMapper;
    private final AulaConverter aulaConverter;

    /**
     * Busca os detalhes de um curso pelo seu hash de inscrição.
     * Essencial para a página de inscrição pública exibir as informações do curso.
     */
    @Operation(summary = "Obtém detalhes públicos de um curso", description = "Retorna os detalhes de um curso para a página de inscrição, usando o hash do link.")
    @GetMapping("/cursos/{hash}")
    public ResponseEntity<CursoPublicoDto> getCursoPublico(@PathVariable UUID hash) {
        CursoPublicoDto cursoDto = cursoService.findByHashInscricao(hash);
        return ResponseEntity.ok(cursoDto);
    }

    @Operation(summary = "Obtém um arquivo público", description = "Retorna os bytes de um arquivo público (ex: imagem de cabeçalho e certificados públicos).")
    @GetMapping("/arquivos/{id}")
    public ResponseEntity<byte[]> getArquivoPublico(@PathVariable Long id) {
        return arquivoService.buscarPorId(id)
                .map(arquivo -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(arquivo.getMimeType()))
                        .body(arquivo.getBytes()))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Realiza a inscrição de um participante externo", description = "Submete os dados de um participante externo para inscrição em um curso através do link público.")
    @PostMapping("/inscricoes/{hash}/externo")
    public ResponseEntity<InscricaoDto> inscreverExterno(@PathVariable UUID hash,
                                                  @Valid @RequestBody InscricaoPublicaDto dto) {
        InscricaoDto inscricaoSalva = inscricaoService.realizarInscricaoPublica(hash, dto);
        return new ResponseEntity<>(inscricaoSalva, HttpStatus.CREATED);
    }

    /**
     * Verifica a situação de um participante externo por CPF para um curso específico
     * @param cpf O CPF a ser verificado.
     * @return Um DTO com o ID do participante externo e o e-mail mascarado se encontrado, ou 404 Not Found caso contrário.
     */
    @Operation(summary = "Verifica a situação de um participante externo por CPF para um curso específico")
    @GetMapping("/cursos/{hash}/verificar-participante-externo")
    public ResponseEntity<ParticipanteVerificacaoDto> verificarParticipanteExterno(
            @PathVariable UUID hash,
            @RequestParam String cpf) {

        ParticipanteVerificacaoDto dto = inscricaoService.verificarParticipanteParaInscricao(hash, cpf.replaceAll("\\D", ""));
        return ResponseEntity.ok(dto);
    }

    /**
     * Confirma uma inscrição do participante externo a partir do token enviado por e-mail.
     * @param token O token de confirmação.
     * @return Uma resposta de sucesso se o token for válido.
     */
    @Operation(summary = "Confirma uma inscrição do participante externo por e-mail", description = "Valida o token recebido e, se for válido, efetiva a inscrição do participante externo.")
    @GetMapping("/inscricoes/confirmar/{token}")
    public ResponseEntity<Void> confirmarInscricaoExterno(@PathVariable String token) {
        inscricaoService.confirmarInscricao(token);
        // Retorna um 200 OK sem corpo, indicando que a operação foi bem-sucedida.
        // O frontend será responsável por mostrar a mensagem de sucesso ao utilizador.
        return ResponseEntity.ok().build();
    }

    /**
     * Obtém os dados da Aula pelo ID
     */
    @Operation(summary = "Obtém dados da chamada da aula", description = "Retorna os detalhes da aula associada ao registro de frequência.")
    @GetMapping("/frequencia/aula/{aulaId}")
    public ResponseEntity<ChamadaPublicaDto> getDadosChamada(@PathVariable Long aulaId) {
        return ResponseEntity.ok(chamadaService.getDadosChamadaPublica(aulaId));
    }

    /**
     * Registra a presença do aluno.
     */
    @Operation(summary = "Registra frequência do participante", description = "Valida o CPF, verifica a inscrição, horário da aula e registra a presença.")
    @PostMapping(value = "/frequencia/aula/{aulaId}/registrar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FrequenciaRegistroResponseDto> registrarFrequencia(@PathVariable Long aulaId,
                                                                             @Valid @RequestBody RegistroFrequenciaDto dto) {
        FrequenciaRegistroResponseDto responseDto = frequenciaService.registrarPresenca(aulaId, dto.getCpf());
        return ResponseEntity.ok(responseDto);
    }

}

