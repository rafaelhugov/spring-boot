package mppb.restapi.mobile.feature.concessaofolga.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
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
import mppb.restapi.mobile.core.security.ResolvedorTokenAutenticacao;
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

    @Value("${mpvirtual.recursos-humanos.auth-mode:token}")
    private String mpVirtualAuthMode;

    @GetMapping("/minhas-concessoes-folga")
    public ResponseEntity<List<ConcessaoFolgaDto>> minhasConcessoes(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "token", required = false) String tokenLegado) {
        String token = tokenObrigatorio(authorization, tokenLegado);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        UsuarioAutenticado usuario = usuarioAutenticado(token);
        if (usuario == null) {
            log.warn("Concessoes de folga negadas: usuario nao autenticado ou sem matricula");
            return ResponseEntity.status(403).build();
        }

        try {
            List<ConcessaoFolgaDto> concessoes = concessaoFolgaService.listarConcessoes(usuario.matricula());
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
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "token", required = false) String tokenLegado,
            @RequestParam(value = "mes", required = false) Integer mes,
            @RequestParam(value = "ano", required = false) Integer ano) {
        String token = tokenObrigatorio(authorization, tokenLegado);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        UsuarioAutenticado usuario = usuarioAutenticado(token);
        if (usuario == null) {
            log.warn("Ausencias negadas: usuario nao autenticado ou sem matricula");
            return ResponseEntity.status(403).build();
        }

        try {
            List<AusenciaCalendarioDto> ausencias = concessaoFolgaService.listarAusencias(usuario.matricula(), mes, ano);
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
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "token", required = false) String tokenLegado,
            @PathVariable("concessaoId") Long concessaoId,
            @RequestParam("dataInicial") String dataInicial) {
        String token = tokenObrigatorio(authorization, tokenLegado);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        UsuarioAutenticado usuario = usuarioAutenticado(token);
        if (usuario == null) {
            log.warn("Sugestoes de ferias negadas: usuario nao autenticado ou sem matricula");
            return ResponseEntity.status(403).build();
        }
        String mpVirtualCredential = mpVirtualCredential(token, usuario);

        try {
            ResponseEntity<String> response = concessaoFolgaService.sugestoesDatasFinais(
                    concessaoId,
                    dataInicial,
                    mpVirtualCredential);
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
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "token", required = false) String tokenLegado,
            @PathVariable("concessaoId") Long concessaoId,
            @Valid @RequestBody SolicitacaoMarcacaoFeriasRequestDto request) {
        String token = tokenObrigatorio(authorization, tokenLegado);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        UsuarioAutenticado usuario = usuarioAutenticado(token);
        if (usuario == null) {
            log.warn("Marcacao de ferias negada: usuario nao autenticado ou sem matricula");
            return ResponseEntity.status(403).build();
        }
        String mpVirtualCredential = mpVirtualCredential(token, usuario);

        try {
            ResponseEntity<String> response = concessaoFolgaService.cadastrarSolicitacao(
                    concessaoId,
                    request,
                    mpVirtualCredential);
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
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "token", required = false) String tokenLegado,
            @PathVariable("solicitacaoId") Long solicitacaoId) {
        String token = tokenObrigatorio(authorization, tokenLegado);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        UsuarioAutenticado usuario = usuarioAutenticado(token);
        if (usuario == null) {
            log.warn("Consulta de solicitacao negada: usuario nao autenticado ou sem matricula");
            return ResponseEntity.status(403).build();
        }
        String mpVirtualCredential = mpVirtualCredential(token, usuario);

        return proxyMpVirtual(
                () -> concessaoFolgaService.consultarSolicitacao(solicitacaoId, mpVirtualCredential),
                "consultar solicitacao");
    }

    @GetMapping(value = "/solicitacoes/{solicitacaoId}/operacoes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> consultarOperacoesSolicitacao(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "token", required = false) String tokenLegado,
            @PathVariable("solicitacaoId") Long solicitacaoId) {
        String token = tokenObrigatorio(authorization, tokenLegado);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        UsuarioAutenticado usuario = usuarioAutenticado(token);
        if (usuario == null) {
            log.warn("Consulta de operacoes negada: usuario nao autenticado ou sem matricula");
            return ResponseEntity.status(403).build();
        }
        String mpVirtualCredential = mpVirtualCredential(token, usuario);

        return proxyMpVirtual(
                () -> concessaoFolgaService.consultarOperacoesSolicitacao(solicitacaoId, mpVirtualCredential),
                "consultar operacoes da solicitacao");
    }

    @PostMapping(value = "/solicitacoes/{solicitacaoId}/cancelamentos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> cancelarSolicitacao(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "token", required = false) String tokenLegado,
            @PathVariable("solicitacaoId") Long solicitacaoId) {
        String token = tokenObrigatorio(authorization, tokenLegado);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        UsuarioAutenticado usuario = usuarioAutenticado(token);
        if (usuario == null) {
            log.warn("Cancelamento negado: usuario nao autenticado ou sem matricula");
            return ResponseEntity.status(403).build();
        }
        String mpVirtualCredential = mpVirtualCredential(token, usuario);

        return proxyMpVirtual(
                () -> concessaoFolgaService.cancelarSolicitacao(solicitacaoId, mpVirtualCredential),
                "cancelar solicitacao");
    }

    @PostMapping(
            value = "/solicitacoes/{solicitacaoId}/adiamentos",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> adiarSolicitacao(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "token", required = false) String tokenLegado,
            @PathVariable("solicitacaoId") Long solicitacaoId,
            @RequestBody OperacaoSolicitacaoRequestDto request) {
        String token = tokenObrigatorio(authorization, tokenLegado);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        UsuarioAutenticado usuario = usuarioAutenticado(token);
        if (usuario == null) {
            log.warn("Adiamento negado: usuario nao autenticado ou sem matricula");
            return ResponseEntity.status(403).build();
        }
        String mpVirtualCredential = mpVirtualCredential(token, usuario);

        return proxyMpVirtual(
                () -> concessaoFolgaService.adiarSolicitacao(solicitacaoId, request, mpVirtualCredential),
                "adiar solicitacao");
    }

    @PostMapping(
            value = "/solicitacoes/{solicitacaoId}/antecipacoes",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> anteciparSolicitacao(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "token", required = false) String tokenLegado,
            @PathVariable("solicitacaoId") Long solicitacaoId,
            @RequestBody OperacaoSolicitacaoRequestDto request) {
        String token = tokenObrigatorio(authorization, tokenLegado);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        UsuarioAutenticado usuario = usuarioAutenticado(token);
        if (usuario == null) {
            log.warn("Antecipacao negada: usuario nao autenticado ou sem matricula");
            return ResponseEntity.status(403).build();
        }
        String mpVirtualCredential = mpVirtualCredential(token, usuario);

        return proxyMpVirtual(
                () -> concessaoFolgaService.anteciparSolicitacao(solicitacaoId, request, mpVirtualCredential),
                "antecipar solicitacao");
    }

    @PostMapping(
            value = "/solicitacoes/{solicitacaoId}/interrupcoes",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> interromperSolicitacao(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "token", required = false) String tokenLegado,
            @PathVariable("solicitacaoId") Long solicitacaoId,
            @RequestBody OperacaoSolicitacaoRequestDto request) {
        String token = tokenObrigatorio(authorization, tokenLegado);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        UsuarioAutenticado usuario = usuarioAutenticado(token);
        if (usuario == null) {
            log.warn("Interrupcao negada: usuario nao autenticado ou sem matricula");
            return ResponseEntity.status(403).build();
        }
        String mpVirtualCredential = mpVirtualCredential(token, usuario);

        return proxyMpVirtual(
                () -> concessaoFolgaService.interromperSolicitacao(solicitacaoId, request, mpVirtualCredential),
                "interromper solicitacao");
    }

    @PostMapping(
            value = "/solicitacoes/{solicitacaoId}/suspensoes-integrais",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> suspenderIntegralmenteSolicitacao(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "token", required = false) String tokenLegado,
            @PathVariable("solicitacaoId") Long solicitacaoId) {
        String token = tokenObrigatorio(authorization, tokenLegado);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        UsuarioAutenticado usuario = usuarioAutenticado(token);
        if (usuario == null) {
            log.warn("Suspensao integral negada: usuario nao autenticado ou sem matricula");
            return ResponseEntity.status(403).build();
        }
        String mpVirtualCredential = mpVirtualCredential(token, usuario);

        return proxyMpVirtual(
                () -> concessaoFolgaService.suspenderIntegralmenteSolicitacao(solicitacaoId, mpVirtualCredential),
                "suspender integralmente solicitacao");
    }

    @PostMapping(
            value = "/solicitacoes/{solicitacaoId}/suspensoes-parciais",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> suspenderParcialmenteSolicitacao(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "token", required = false) String tokenLegado,
            @PathVariable("solicitacaoId") Long solicitacaoId,
            @RequestBody OperacaoSolicitacaoRequestDto request) {
        String token = tokenObrigatorio(authorization, tokenLegado);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        UsuarioAutenticado usuario = usuarioAutenticado(token);
        if (usuario == null) {
            log.warn("Suspensao parcial negada: usuario nao autenticado ou sem matricula");
            return ResponseEntity.status(403).build();
        }
        String mpVirtualCredential = mpVirtualCredential(token, usuario);

        return proxyMpVirtual(
                () -> concessaoFolgaService.suspenderParcialmenteSolicitacao(solicitacaoId, request, mpVirtualCredential),
                "suspender parcialmente solicitacao");
    }

    private UsuarioAutenticado usuarioAutenticado(String token) {
        String login = extractLoginFromToken(token);
        if (login == null) {
            return null;
        }
        String matricula = concessaoFolgaService.getMatriculaByLogin(login);
        if (matricula == null || matricula.isBlank()) {
            return null;
        }
        return new UsuarioAutenticado(login, matricula);
    }

    private String tokenObrigatorio(String authorization, String tokenLegado) {
        return ResolvedorTokenAutenticacao.resolver(authorization, tokenLegado);
    }

    private String mpVirtualCredential(String token, UsuarioAutenticado usuario) {
        if ("login".equalsIgnoreCase(mpVirtualAuthMode)) {
            return usuario.login();
        }
        return token;
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
            log.warn("MPVirtual recusou {}: status={} body={}",
                    action,
                    e.getStatusCode().value(),
                    truncateLogBody(e.getResponseBodyAsString()));
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Erro ao {}", action, e);
            return ResponseEntity.status(502).build();
        }
    }

    private String truncateLogBody(String body) {
        if (body == null || body.isBlank()) {
            return "<empty>";
        }
        String normalized = body.replaceAll("[\\r\\n\\t]+", " ").trim();
        int maxLength = 1000;
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    @FunctionalInterface
    private interface MpVirtualCall {
        ResponseEntity<String> execute();
    }

    private record UsuarioAutenticado(String login, String matricula) {
    }
}
