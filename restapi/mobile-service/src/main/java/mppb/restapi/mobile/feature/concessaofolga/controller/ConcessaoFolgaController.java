package mppb.restapi.mobile.feature.concessaofolga.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientResponseException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.infrastructure.client.authapi.AuthServiceClient;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoRequestDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoResponseDto;
import mppb.restapi.common.infrastructure.util.Constants;
import mppb.restapi.mobile.feature.concessaofolga.dto.AusenciaCalendarioDto;
import mppb.restapi.mobile.feature.concessaofolga.dto.ConcessaoFolgaDto;
import mppb.restapi.mobile.feature.concessaofolga.dto.OperacaoSolicitacaoRequestDto;
import mppb.restapi.mobile.feature.concessaofolga.dto.SolicitacaoMarcacaoFeriasRequestDto;
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

    @GetMapping(value = "/concessoes/{concessaoId}/sugestoes-datas-finais", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> sugestoesDatasFinais(
            @RequestParam("token") @NotNull String token,
            @PathVariable("concessaoId") Long concessaoId,
            @RequestParam("dataInicial") String dataInicial) {
        if (matriculaAutenticada(token) == null) {
            log.warn("Sugestoes de ferias negadas: usuario nao autenticado ou sem matricula");
            return ResponseEntity.status(403).build();
        }

        try {
            ResponseEntity<String> response = concessaoFolgaService.sugestoesDatasFinais(concessaoId, dataInicial, token);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (RestClientResponseException e) {
            log.warn("MPVirtual recusou sugestao de datas finais: status={}", e.getStatusCode().value());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Erro ao buscar sugestoes de datas finais", e);
            return ResponseEntity.status(502).build();
        }
    }

    @PostMapping(
            value = "/concessoes/{concessaoId}/solicitacoes",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> cadastrarSolicitacao(
            @RequestParam("token") @NotNull String token,
            @PathVariable("concessaoId") Long concessaoId,
            @Valid @RequestBody SolicitacaoMarcacaoFeriasRequestDto request) {
        if (matriculaAutenticada(token) == null) {
            log.warn("Marcacao de ferias negada: usuario nao autenticado ou sem matricula");
            return ResponseEntity.status(403).build();
        }

        try {
            ResponseEntity<String> response = concessaoFolgaService.cadastrarSolicitacao(concessaoId, request, token);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (RestClientResponseException e) {
            log.warn("MPVirtual recusou marcacao de ferias: status={}", e.getStatusCode().value());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Erro ao cadastrar marcacao de ferias", e);
            return ResponseEntity.status(502).build();
        }
    }

    @GetMapping(value = "/solicitacoes/{solicitacaoId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> consultarSolicitacao(
            @RequestParam("token") @NotNull String token,
            @PathVariable("solicitacaoId") Long solicitacaoId) {
        if (matriculaAutenticada(token) == null) {
            log.warn("Consulta de solicitacao negada: usuario nao autenticado ou sem matricula");
            return ResponseEntity.status(403).build();
        }

        return proxyMpVirtual(
                () -> concessaoFolgaService.consultarSolicitacao(solicitacaoId, token),
                "consultar solicitacao");
    }

    @GetMapping(value = "/solicitacoes/{solicitacaoId}/operacoes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> consultarOperacoesSolicitacao(
            @RequestParam("token") @NotNull String token,
            @PathVariable("solicitacaoId") Long solicitacaoId) {
        if (matriculaAutenticada(token) == null) {
            log.warn("Consulta de operacoes negada: usuario nao autenticado ou sem matricula");
            return ResponseEntity.status(403).build();
        }

        return proxyMpVirtual(
                () -> concessaoFolgaService.consultarOperacoesSolicitacao(solicitacaoId, token),
                "consultar operacoes da solicitacao");
    }

    @PostMapping(value = "/solicitacoes/{solicitacaoId}/cancelamentos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> cancelarSolicitacao(
            @RequestParam("token") @NotNull String token,
            @PathVariable("solicitacaoId") Long solicitacaoId) {
        if (matriculaAutenticada(token) == null) {
            log.warn("Cancelamento negado: usuario nao autenticado ou sem matricula");
            return ResponseEntity.status(403).build();
        }

        return proxyMpVirtual(
                () -> concessaoFolgaService.cancelarSolicitacao(solicitacaoId, token),
                "cancelar solicitacao");
    }

    @PostMapping(
            value = "/solicitacoes/{solicitacaoId}/adiamentos",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> adiarSolicitacao(
            @RequestParam("token") @NotNull String token,
            @PathVariable("solicitacaoId") Long solicitacaoId,
            @RequestBody OperacaoSolicitacaoRequestDto request) {
        if (matriculaAutenticada(token) == null) {
            log.warn("Adiamento negado: usuario nao autenticado ou sem matricula");
            return ResponseEntity.status(403).build();
        }

        return proxyMpVirtual(
                () -> concessaoFolgaService.adiarSolicitacao(solicitacaoId, request, token),
                "adiar solicitacao");
    }

    @PostMapping(
            value = "/solicitacoes/{solicitacaoId}/antecipacoes",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> anteciparSolicitacao(
            @RequestParam("token") @NotNull String token,
            @PathVariable("solicitacaoId") Long solicitacaoId,
            @RequestBody OperacaoSolicitacaoRequestDto request) {
        if (matriculaAutenticada(token) == null) {
            log.warn("Antecipacao negada: usuario nao autenticado ou sem matricula");
            return ResponseEntity.status(403).build();
        }

        return proxyMpVirtual(
                () -> concessaoFolgaService.anteciparSolicitacao(solicitacaoId, request, token),
                "antecipar solicitacao");
    }

    @PostMapping(
            value = "/solicitacoes/{solicitacaoId}/interrupcoes",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> interromperSolicitacao(
            @RequestParam("token") @NotNull String token,
            @PathVariable("solicitacaoId") Long solicitacaoId,
            @RequestBody OperacaoSolicitacaoRequestDto request) {
        if (matriculaAutenticada(token) == null) {
            log.warn("Interrupcao negada: usuario nao autenticado ou sem matricula");
            return ResponseEntity.status(403).build();
        }

        return proxyMpVirtual(
                () -> concessaoFolgaService.interromperSolicitacao(solicitacaoId, request, token),
                "interromper solicitacao");
    }

    @PostMapping(
            value = "/solicitacoes/{solicitacaoId}/suspensoes-integrais",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> suspenderIntegralmenteSolicitacao(
            @RequestParam("token") @NotNull String token,
            @PathVariable("solicitacaoId") Long solicitacaoId) {
        if (matriculaAutenticada(token) == null) {
            log.warn("Suspensao integral negada: usuario nao autenticado ou sem matricula");
            return ResponseEntity.status(403).build();
        }

        return proxyMpVirtual(
                () -> concessaoFolgaService.suspenderIntegralmenteSolicitacao(solicitacaoId, token),
                "suspender integralmente solicitacao");
    }

    @PostMapping(
            value = "/solicitacoes/{solicitacaoId}/suspensoes-parciais",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> suspenderParcialmenteSolicitacao(
            @RequestParam("token") @NotNull String token,
            @PathVariable("solicitacaoId") Long solicitacaoId,
            @RequestBody OperacaoSolicitacaoRequestDto request) {
        if (matriculaAutenticada(token) == null) {
            log.warn("Suspensao parcial negada: usuario nao autenticado ou sem matricula");
            return ResponseEntity.status(403).build();
        }

        return proxyMpVirtual(
                () -> concessaoFolgaService.suspenderParcialmenteSolicitacao(solicitacaoId, request, token),
                "suspender parcialmente solicitacao");
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

    private ResponseEntity<String> proxyMpVirtual(MpVirtualCall call, String action) {
        try {
            ResponseEntity<String> response = call.execute();
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (RestClientResponseException e) {
            log.warn("MPVirtual recusou {}: status={}", action, e.getStatusCode().value());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Erro ao {}", action, e);
            return ResponseEntity.status(502).build();
        }
    }

    @FunctionalInterface
    private interface MpVirtualCall {
        ResponseEntity<String> execute();
    }
}
