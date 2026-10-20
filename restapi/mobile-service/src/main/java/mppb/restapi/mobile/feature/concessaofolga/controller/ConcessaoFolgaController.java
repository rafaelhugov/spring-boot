package mppb.restapi.mobile.feature.concessaofolga.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.infrastructure.util.Constants;
import mppb.restapi.mobile.core.security.AutenticacaoAtual;
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

    @Value("${mpvirtual.recursos-humanos.auth-mode:token}")
    private String mpVirtualAuthMode;

    @GetMapping("/minhas-concessoes-folga")
    public ResponseEntity<List<ConcessaoFolgaDto>> minhasConcessoes() {
        UsuarioAutenticado usuario = usuarioAutenticado();
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
            @RequestParam(value = "mes", required = false) Integer mes,
            @RequestParam(value = "ano", required = false) Integer ano) {
        UsuarioAutenticado usuario = usuarioAutenticado();
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
            @PathVariable("concessaoId") Long concessaoId,
            @RequestParam("dataInicial") String dataInicial) {
        String token = tokenAutenticado();
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        UsuarioAutenticado usuario = usuarioAutenticado();
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
            return tratarErroMpVirtual(e, "buscar sugestoes de datas finais");
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
            @PathVariable("concessaoId") Long concessaoId,
            @Valid @RequestBody SolicitacaoMarcacaoFeriasRequestDto request) {
        String token = tokenAutenticado();
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        UsuarioAutenticado usuario = usuarioAutenticado();
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
            return tratarErroMpVirtual(e, "cadastrar marcacao de ferias");
        } catch (Exception e) {
            log.error("Erro ao cadastrar marcacao de ferias", e);
            return ResponseEntity.status(502).build();
        }
    }

    @GetMapping(value = "/solicitacoes/{solicitacaoId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> consultarSolicitacao(
            @PathVariable("solicitacaoId") Long solicitacaoId) {
        String token = tokenAutenticado();
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        UsuarioAutenticado usuario = usuarioAutenticado();
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
            @PathVariable("solicitacaoId") Long solicitacaoId) {
        String token = tokenAutenticado();
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        UsuarioAutenticado usuario = usuarioAutenticado();
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
            @PathVariable("solicitacaoId") Long solicitacaoId) {
        String token = tokenAutenticado();
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        UsuarioAutenticado usuario = usuarioAutenticado();
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
            @PathVariable("solicitacaoId") Long solicitacaoId,
            @RequestBody OperacaoSolicitacaoRequestDto request) {
        String token = tokenAutenticado();
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        UsuarioAutenticado usuario = usuarioAutenticado();
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
            @PathVariable("solicitacaoId") Long solicitacaoId,
            @RequestBody OperacaoSolicitacaoRequestDto request) {
        String token = tokenAutenticado();
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        UsuarioAutenticado usuario = usuarioAutenticado();
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
            @PathVariable("solicitacaoId") Long solicitacaoId,
            @RequestBody OperacaoSolicitacaoRequestDto request) {
        String token = tokenAutenticado();
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        UsuarioAutenticado usuario = usuarioAutenticado();
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
            @PathVariable("solicitacaoId") Long solicitacaoId) {
        String token = tokenAutenticado();
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        UsuarioAutenticado usuario = usuarioAutenticado();
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
            @PathVariable("solicitacaoId") Long solicitacaoId,
            @RequestBody OperacaoSolicitacaoRequestDto request) {
        String token = tokenAutenticado();
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        UsuarioAutenticado usuario = usuarioAutenticado();
        if (usuario == null) {
            log.warn("Suspensao parcial negada: usuario nao autenticado ou sem matricula");
            return ResponseEntity.status(403).build();
        }
        String mpVirtualCredential = mpVirtualCredential(token, usuario);

        return proxyMpVirtual(
                () -> concessaoFolgaService.suspenderParcialmenteSolicitacao(solicitacaoId, request, mpVirtualCredential),
                "suspender parcialmente solicitacao");
    }

    private UsuarioAutenticado usuarioAutenticado() {
        String login = AutenticacaoAtual.login().orElse(null);
        if (login == null) {
            return null;
        }
        String matricula = concessaoFolgaService.getMatriculaByLogin(login);
        if (matricula == null || matricula.isBlank()) {
            return null;
        }
        return new UsuarioAutenticado(login, matricula);
    }

    private String tokenAutenticado() {
        return AutenticacaoAtual.token().orElse(null);
    }

    private String mpVirtualCredential(String token, UsuarioAutenticado usuario) {
        if ("login".equalsIgnoreCase(mpVirtualAuthMode)) {
            return usuario.login();
        }
        return token;
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
            return tratarErroMpVirtual(e, action);
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

    private ResponseEntity<String> tratarErroMpVirtual(RestClientResponseException e, String action) {
        String body = e.getResponseBodyAsString();
        String bodyLower = body != null ? body.toLowerCase() : "";

        if (bodyLower.contains("menos de 1 minuto")
                || bodyLower.contains("fragmentofla_prevent_fntg")
                || bodyLower.contains("mais uma etapa em uma concess")) {
            String jsonError = """
                {"message":"Não é possível inserir mais uma etapa em uma concessão em menos de 1 minuto desde a última. Por favor, aguarde 1 minuto antes de tentar novamente."}
                """.trim();
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonError);
        }

        if (body != null && !body.isBlank()) {
            String trimmed = body.trim();
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                return ResponseEntity.status(e.getStatusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(trimmed);
            }
            if (trimmed.startsWith("<") || trimmed.contains("<html") || trimmed.contains("<HTML")) {
                String jsonError = "{\"message\":\"Falha no serviço de Recursos Humanos ao " + action + ".\"}";
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jsonError);
            }
            String escaped = trimmed.replace("\\", "\\\\").replace("\"", "\\\"").replaceAll("[\\r\\n]+", " ");
            String jsonError = "{\"message\":\"" + escaped + "\"}";
            return ResponseEntity.status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonError);
        }

        return ResponseEntity.status(e.getStatusCode()).build();
    }

    @FunctionalInterface
    private interface MpVirtualCall {
        ResponseEntity<String> execute();
    }

    private record UsuarioAutenticado(String login, String matricula) {
    }
}
