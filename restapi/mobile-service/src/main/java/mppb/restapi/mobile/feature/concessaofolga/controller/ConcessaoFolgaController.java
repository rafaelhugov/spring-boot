package mppb.restapi.mobile.feature.concessaofolga.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
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
import mppb.restapi.mobile.feature.concessaofolga.dto.AusenciaCalendarioDto;
import mppb.restapi.mobile.feature.concessaofolga.dto.ConcessaoFolgaDto;
import mppb.restapi.mobile.feature.concessaofolga.repository.AtendimentoUnavailableException;
import mppb.restapi.mobile.feature.concessaofolga.service.ConcessaoFolgaService;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(Constants.API_PREFIX + "/recursos-humanos")
@RequiredArgsConstructor
public class ConcessaoFolgaController {

    private final ConcessaoFolgaService concessaoFolgaService;
    private final AuthServiceClient authServiceClient;

    @GetMapping("/minhas-concessoes-folga")
    public ResponseEntity<List<ConcessaoFolgaDto>> minhasConcessoes(
            @RequestParam("token") @NotNull String token) {
        String matricula = matriculaAutenticada(token);
        if (matricula == null) {
            log.warn("Concessoes de folga negadas: usuario nao autenticado ou sem matricula");
            return ResponseEntity.status(403).build();
        }

        try {
            List<ConcessaoFolgaDto> concessoes = concessaoFolgaService.listarConcessoes(matricula);
            log.info("Concessoes de folga consultadas com sucesso: {} registro(s)", concessoes.size());
            return ResponseEntity.ok(concessoes);
        } catch (AtendimentoUnavailableException e) {
            log.warn("Fonte remota de concessoes indisponivel");
            return ResponseEntity.status(503).build();
        } catch (Exception e) {
            log.error("Erro ao buscar concessoes de folga", e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/minhas-ausencias")
    public ResponseEntity<List<AusenciaCalendarioDto>> minhasAusencias(
            @RequestParam("token") @NotNull String token,
            @RequestParam(value = "mes", required = false) Integer mes,
            @RequestParam(value = "ano", required = false) Integer ano) {
        String matricula = matriculaAutenticada(token);
        if (matricula == null) {
            log.warn("Ausencias negadas: usuario nao autenticado ou sem matricula");
            return ResponseEntity.status(403).build();
        }

        try {
            List<AusenciaCalendarioDto> ausencias = concessaoFolgaService.listarAusencias(matricula, mes, ano);
            log.info("Ausencias consultadas com sucesso: {} registro(s)", ausencias.size());
            return ResponseEntity.ok(ausencias);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (AtendimentoUnavailableException e) {
            log.warn("Fonte remota de ausencias indisponivel");
            return ResponseEntity.status(503).build();
        } catch (Exception e) {
            log.error("Erro ao buscar ausencias", e);
            return ResponseEntity.status(500).build();
        }
    }

    private String matriculaAutenticada(String token) {
        String login = extractLoginFromToken(token);
        if (login == null) {
            return null;
        }
        String matricula = concessaoFolgaService.getMatriculaByLogin(login);
        return matricula == null || matricula.isBlank() ? null : matricula;
    }

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
}
