package mppb.restapi.mobile.feature.ferias.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.infrastructure.client.authapi.AuthServiceClient;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoRequestDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoResponseDto;
import mppb.restapi.common.infrastructure.util.Constants;
import mppb.restapi.mobile.feature.ferias.dto.EditalFeriasDto;
import mppb.restapi.mobile.feature.ferias.dto.NovaSolicitacaoFeriasDto;
import mppb.restapi.mobile.feature.ferias.dto.SolicitacaoFLADto;
import mppb.restapi.mobile.feature.ferias.service.FeriasService;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping({ "/ferias", "/" + Constants.API_PREFIX + "/ferias" })
@RequiredArgsConstructor
public class FeriasController {

    private final FeriasService feriasService;
    private final AuthServiceClient authServiceClient;

    private String extractLoginFromToken(String token) {
        ConsultaAutorizacaoRequestDto req = new ConsultaAutorizacaoRequestDto(token, "ANDROID");
        ConsultaAutorizacaoResponseDto autorizacao = null;
        try {
            autorizacao = authServiceClient.consultaAutorizacao(req);
        } catch (Exception e) {
            log.error("Erro ao consultar autorizacao no auth-service", e);
        }

        if (autorizacao == null || autorizacao.getUsuario() == null || autorizacao.getUsuario().getLogin() == null) {
            return null;
        }

        return autorizacao.getUsuario().getLogin();
    }

    @GetMapping("/minhas-solicitacoes")
    public ResponseEntity<List<SolicitacaoFLADto>> getMinhasSolicitacoes(@RequestParam("token") @NotNull String token) {
        String login = extractLoginFromToken(token);
        if (login == null) {
            return ResponseEntity.status(403).build();
        }

        try {
            Integer matricula = feriasService.getMatriculaByLogin(login);
            if (matricula == null) {
                return ResponseEntity.status(400).build();
            }

            List<SolicitacaoFLADto> solicitacoes = feriasService.listarMinhasSolicitacoes(matricula);
            return ResponseEntity.ok(solicitacoes);
        } catch (Exception e) {
            log.error("Erro ao buscar solicitacoes de ferias", e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/editais-abertos")
    public ResponseEntity<List<EditalFeriasDto>> getEditaisAbertos(@RequestParam("token") @NotNull String token) {
        String login = extractLoginFromToken(token);
        if (login == null) {
            return ResponseEntity.status(403).build();
        }

        try {
            List<EditalFeriasDto> editais = feriasService.listarEditaisAbertos();
            return ResponseEntity.ok(editais);
        } catch (Exception e) {
            log.error("Erro ao buscar editais abertos", e);
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/solicitar")
    public ResponseEntity<Void> solicitarFerias(
            @RequestParam("token") @NotNull String token,
            @RequestBody NovaSolicitacaoFeriasDto dto) {
            
        String login = extractLoginFromToken(token);
        if (login == null) {
            return ResponseEntity.status(403).build();
        }

        try {
            Integer matricula = feriasService.getMatriculaByLogin(login);
            if (matricula == null) {
                return ResponseEntity.status(400).build();
            }

            feriasService.solicitarFerias(dto, matricula, login);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("Erro de validacao ao solicitar ferias", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erro ao solicitar ferias", e);
            return ResponseEntity.status(500).build();
        }
    }
}
