package mppb.restapi.mobile.quality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import feign.Request;
import feign.Request.HttpMethod;
import feign.Response;
import feign.RetryableException;
import mppb.restapi.common.core.error.feign.ClientApiException;
import mppb.restapi.common.core.error.feign.FeignErrorDecoder;
import mppb.restapi.common.core.handler.ApiExceptionHandler;
import mppb.restapi.common.infrastructure.client.authapi.AuthServiceClient;
import mppb.restapi.common.infrastructure.client.authapi.dto.AutorizaRequestDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.AutorizaResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoRequestDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoResponseDto;
import mppb.restapi.common.infrastructure.env.RuntimeEnv;
import mppb.restapi.common.infrastructure.security.authorization.TokenAuthentication;
import mppb.restapi.mobile.feature.perfil.controller.PerfilUsuarioController;
import mppb.restapi.mobile.feature.perfil.dto.PerfilUsuarioDto;
import mppb.restapi.mobile.feature.perfil.repository.PerfilUsuarioRepository;
import mppb.restapi.mobile.feature.perfil.service.PerfilUsuarioService;

/**
 * Bateria de testes determinísticos Q7 para validação de resiliência a falhas de rede,
 * indisponibilidade de dependências externas, timeouts e tratamento de erros HTTP no backend.
 */
class Q7RedeInterrupcoesTest {

    private ApiExceptionHandler apiExceptionHandler;
    private FeignErrorDecoder feignErrorDecoder;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        StaticMessageSource messageSource = new StaticMessageSource();
        RuntimeEnv runtimeEnv = new RuntimeEnv();
        apiExceptionHandler = new ApiExceptionHandler(messageSource, runtimeEnv);
        feignErrorDecoder = new FeignErrorDecoder();
    }

    // =========================================================================
    // Q7-T19: Dependência Externa Indisponível (RetryableException -> 503)
    // =========================================================================

    @Test
    @DisplayName("Q7-T19: Falha de conexao com cliente externo gera HTTP 503 com ProblemDetail padronizado")
    void testFalhaConexaoDependenciaExternaGera503ProblemDetail() {
        Request request = Request.create(HttpMethod.GET, "http://10.128.4.14:8082/api/externa", Map.of(), null, StandardCharsets.UTF_8, null);
        RetryableException ex = new RetryableException(
                503,
                "Connection refused: connect to external auth-service",
                HttpMethod.GET,
                new Date(),
                request
        );

        ResponseEntity<ProblemDetail> response = apiExceptionHandler.handleRetryableException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Serviço externo indisponível");
        assertThat(response.getBody().getDetail()).contains("O serviço dependente não pôde ser contatado");
    }

    // =========================================================================
    // Q7-T20: Timeout Backend em Dependência Externa
    // =========================================================================

    @Test
    @DisplayName("Q7-T20: Timeout de socket em cliente Feign nao trava thread e retorna HTTP 503")
    void testTimeoutDependenciaExternaNaoTravaThreadERetorna503() throws Exception {
        Request request = Request.create(HttpMethod.POST, "http://10.128.4.14:8082/recursos-humanos", Map.of(), null, StandardCharsets.UTF_8, null);
        RetryableException timeoutException = new RetryableException(
                504,
                "Read timed out executing POST http://10.128.4.14:8082/recursos-humanos",
                HttpMethod.POST,
                new Date(),
                request
        );

        CompletableFuture<ResponseEntity<ProblemDetail>> future = CompletableFuture.supplyAsync(() ->
                apiExceptionHandler.handleRetryableException(timeoutException)
        );

        // Garante que o tratamento do timeout e imediato sem travar thread no pool
        ResponseEntity<ProblemDetail> response = future.get(1, TimeUnit.SECONDS);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Serviço externo indisponível");
    }

    // =========================================================================
    // Q7-T19: Dependência Externa Retorna Erro 5xx (ClientApiException -> 502)
    // =========================================================================

    @Test
    @DisplayName("Q7-T19: Erro 5xx retornado por sistema externo e decodificado e traduzido em HTTP 502")
    void testServicoExternoRetorna5xxTraduzEm502BadGateway() {
        Request request = Request.create(HttpMethod.GET, "http://auth-service/acesso", Map.of(), null, StandardCharsets.UTF_8, null);
        Response feignResponse = Response.builder()
                .status(500)
                .reason("Internal Server Error")
                .request(request)
                .headers(Map.of())
                .body("Erro interno no cluster upstream", StandardCharsets.UTF_8)
                .build();

        Exception decoded = feignErrorDecoder.decode("AuthServiceClient#consultaAutorizacao", feignResponse);

        assertThat(decoded).isInstanceOf(ClientApiException.class);
        ClientApiException clientEx = (ClientApiException) decoded;

        ResponseEntity<ProblemDetail> response = apiExceptionHandler.handleClientApiException(clientEx);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Erro em serviço externo");
    }

    // =========================================================================
    // Q7-T19: Resposta Malformada ou Vazia de Dependência Não Quebra o Serviço
    // =========================================================================

    @Test
    @DisplayName("Q7-T19: Falha em consulta externa de autorizacao e capturada graciosamente sem exception descontrolada")
    void testFalhaConsultaAutorizacaoRetornaNullControlado() {
        AuthServiceClient failingAuthClient = new AuthServiceClient() {
            @Override
            public AutorizaResponseDto autoriza(AutorizaRequestDto autorizaRequestTO) {
                return null;
            }

            @Override
            public ConsultaAutorizacaoResponseDto consultaAutorizacao(ConsultaAutorizacaoRequestDto req) {
                throw new RuntimeException("Host externo inacessivel");
            }
        };

        PerfilUsuarioRepository repo = new PerfilUsuarioRepository(null) {
            @Override
            public PerfilUsuarioDto buscarPorLogin(String login) {
                return null;
            }
        };

        PerfilUsuarioService perfilService = new PerfilUsuarioService(failingAuthClient, repo);

        // Deve retornar null silenciosamente com log.warn, sem propagar exception descontrolada
        assertDoesNotThrow(() -> {
            PerfilUsuarioDto dto = perfilService.buscarPerfilAutenticado("token-qualquer");
            assertThat(dto).isNull();
        });
    }

    // =========================================================================
    // Q7-T15: Sessão com Falha de Token Retorna HTTP 401
    // =========================================================================

    @Test
    @DisplayName("Q7-T15: Token invalido ou nulo no controller de Perfil retorna HTTP 401")
    void testPerfilComTokenNuloOuInvalidoRetorna401() {
        AuthServiceClient mockAuthClient = new AuthServiceClient() {
            @Override
            public AutorizaResponseDto autoriza(AutorizaRequestDto autorizaRequestTO) {
                return null;
            }

            @Override
            public ConsultaAutorizacaoResponseDto consultaAutorizacao(ConsultaAutorizacaoRequestDto req) {
                return null; // Token invalido
            }
        };

        PerfilUsuarioRepository repo = new PerfilUsuarioRepository(null) {
            @Override
            public PerfilUsuarioDto buscarPorLogin(String login) {
                return null;
            }
        };

        PerfilUsuarioService service = new PerfilUsuarioService(mockAuthClient, repo);
        PerfilUsuarioController controller = new PerfilUsuarioController(service);

        // 1. Sem autenticacao no contexto
        ResponseEntity<PerfilUsuarioDto> respSemAuth = controller.dados();
        assertThat(respSemAuth.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // 2. Com token presente mas que nao resolve perfil no upstream
        TokenAuthentication authentication = new TokenAuthentication("token-expirado");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        ResponseEntity<PerfilUsuarioDto> respTokenExpirado = controller.dados();
        assertThat(respTokenExpirado.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // =========================================================================
    // Q7-T16: Falha de Autorização (403 Forbidden)
    // =========================================================================

    @Test
    @DisplayName("Q7-T16: AccessDeniedException e traduzido em HTTP 403 sem destruir sessao")
    void testAccessDeniedExceptionTraduzEm403Forbidden() {
        AccessDeniedException ex = new AccessDeniedException("Usuario sem permissao para esta operacao");

        ResponseEntity<ProblemDetail> response = apiExceptionHandler.handleAccessDeniedException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Acesso negado");
    }
}
