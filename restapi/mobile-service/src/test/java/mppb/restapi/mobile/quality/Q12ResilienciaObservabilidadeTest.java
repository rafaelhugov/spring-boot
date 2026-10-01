package mppb.restapi.mobile.quality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;

import feign.Request;
import feign.Request.HttpMethod;
import feign.Response;
import feign.RetryableException;
import mppb.restapi.common.core.error.AppException;
import mppb.restapi.common.core.error.BusinessRuleException;
import mppb.restapi.common.core.error.NotFoundException;
import mppb.restapi.common.core.error.feign.ClientApiException;
import mppb.restapi.common.core.error.feign.FeignErrorDecoder;
import mppb.restapi.common.core.handler.ApiExceptionHandler;
import mppb.restapi.common.infrastructure.client.authapi.AuthServiceClient;
import mppb.restapi.common.infrastructure.client.authapi.dto.AutorizaRequestDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.AutorizaResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoRequestDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoResponseDto;
import mppb.restapi.common.infrastructure.env.RuntimeEnv;
import mppb.restapi.mobile.feature.perfil.controller.PerfilUsuarioController;
import mppb.restapi.mobile.feature.perfil.dto.PerfilUsuarioDto;
import mppb.restapi.mobile.feature.perfil.repository.PerfilUsuarioRepository;
import mppb.restapi.mobile.feature.perfil.service.PerfilUsuarioService;

/**
 * Bateria de testes automatizados determinísticos da fase Q12:
 * Resiliência, Observabilidade e Recuperação no backend Spring Boot (restapi-common e mobile-service).
 */
class Q12ResilienciaObservabilidadeTest {

    private ApiExceptionHandler apiExceptionHandler;
    private RuntimeEnv runtimeEnv;
    private FeignErrorDecoder feignErrorDecoder;

    @BeforeEach
    void setUp() {
        StaticMessageSource messageSource = new StaticMessageSource();
        runtimeEnv = new RuntimeEnv();
        apiExceptionHandler = new ApiExceptionHandler(messageSource, runtimeEnv);
        feignErrorDecoder = new FeignErrorDecoder();
    }

    private void setActiveProfiles(String profiles) throws Exception {
        Field field = RuntimeEnv.class.getDeclaredField("activeProfiles");
        field.setAccessible(true);
        field.set(runtimeEnv, profiles);
    }

    // =========================================================================
    // Q12-T10: Exception Handlers (@RestControllerAdvice)
    // =========================================================================

    @Test
    @DisplayName("Q12-T10: BusinessRuleException gera HTTP 422 Unprocessable Entity padronizado")
    void testBusinessRuleExceptionGera422() {
        BusinessRuleException ex = new BusinessRuleException("Regra de negócio violada para operação solicitada");
        ResponseEntity<ProblemDetail> response = apiExceptionHandler.handleBusinessRuleException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Regra de negócio violada");
        assertThat(response.getBody().getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("Q12-T10: NotFoundException gera HTTP 404 Not Found")
    void testNotFoundExceptionGera404() {
        NotFoundException ex = new NotFoundException("Recurso consultado inexistente");
        ResponseEntity<ProblemDetail> response = apiExceptionHandler.handleNotFoundException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Recurso não encontrado");
    }

    @Test
    @DisplayName("Q12-T10: RetryableException de downstream gera HTTP 503 Service Unavailable")
    void testRetryableExceptionGera503() {
        Request request = Request.create(HttpMethod.GET, "http://auth-service/acesso", Map.of(), null, StandardCharsets.UTF_8, null);
        RetryableException ex = new RetryableException(503, "Connection refused", HttpMethod.GET, new Date(), request);

        ResponseEntity<ProblemDetail> response = apiExceptionHandler.handleRetryableException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Serviço externo indisponível");
        assertThat(response.getBody().getDetail()).contains("O serviço dependente não pôde ser contatado");
    }

    @Test
    @DisplayName("Q12-T10: ClientApiException de downstream gera HTTP 502 Bad Gateway")
    void testClientApiExceptionGera502() {
        ClientApiException ex = new ClientApiException(500, "Erro interno reportado pelo sistema remoto");
        ResponseEntity<ProblemDetail> response = apiExceptionHandler.handleClientApiException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Erro em serviço externo");
    }

    @Test
    @DisplayName("Q12-T10: AppException genérico gera HTTP 500 sem expor causa interna")
    void testAppExceptionGera500() {
        AppException ex = new AppException("Falha genérica no processamento interno");
        ResponseEntity<ProblemDetail> response = apiExceptionHandler.handleAppException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Erro genérico da aplicação");
        assertThat(response.getBody().getDetail()).contains("Ocorreu um erro inesperado no sistema");
    }

    // =========================================================================
    // Q12-T11: Erro inesperado backend (Proteção de Stacktrace e Dados Sensíveis)
    // =========================================================================

    @Test
    @DisplayName("Q12-T11: Em profile prod, stacktrace (trace) e OMITIDA de ProblemDetail para evitar vazamento")
    void testErroInesperadoEmProdOmitiStackTrace() throws Exception {
        setActiveProfiles("prod");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        Exception ex = new RuntimeException("Falha crítica de conexão SQL: jdbc:postgresql://db-internal:5432/mppb?password=secret");

        ResponseEntity<ProblemDetail> response = apiExceptionHandler.handleGenericException(ex, servletResponse);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Erro interno no servidor");
        assertThat(response.getBody().getDetail()).isEqualTo("Ocorreu um erro inesperado no sistema. Por favor, tente novamente mais tarde.");
        assertThat(response.getBody().getProperties()).doesNotContainKey("trace");
    }

    @Test
    @DisplayName("Q12-T11: Falha durante geração de PDF escreve JSON direto no response stream sem quebrar serializador")
    void testFalhaDurantePdfEscreveJsonDireto() throws Exception {
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        servletResponse.setContentType("application/pdf");
        Exception ex = new RuntimeException("Erro ao compilar relatório Jasper");

        ResponseEntity<ProblemDetail> response = apiExceptionHandler.handleGenericException(ex, servletResponse);

        // Deve retornar null pois a resposta foi gravada diretamente no HttpServletResponse
        assertThat(response).isNull();
        assertThat(servletResponse.getStatus()).isEqualTo(500);
        assertThat(servletResponse.getContentType()).contains("application/problem+json");
        assertThat(servletResponse.getContentAsString()).contains("Erro interno no servidor");
        assertThat(servletResponse.getContentAsString()).doesNotContain("Jasper");
    }

    // =========================================================================
    // Q12-T12 / Q12-T13 / Q12-T15: Downstream Failure, Timeout e Recovery
    // =========================================================================

    @Test
    @DisplayName("Q12-T12/T13: FeignErrorDecoder traduz respostas de erro 4xx/5xx downstream para ClientApiException")
    void testFeignErrorDecoderTraduzDownstream() {
        Request request = Request.create(HttpMethod.POST, "http://mpvirtual/api/protocolo", Map.of(), null, StandardCharsets.UTF_8, null);
        Response response = Response.builder()
                .status(504)
                .reason("Gateway Timeout")
                .request(request)
                .headers(Map.of())
                .body("Upstream server timeout", StandardCharsets.UTF_8)
                .build();

        Exception decoded = feignErrorDecoder.decode("MPVirtualClient#protocolar", response);

        assertThat(decoded).isInstanceOf(ClientApiException.class);
        ClientApiException clientEx = (ClientApiException) decoded;
        assertThat(clientEx.getMessage()).contains("MPVirtualClient#protocolar");
        assertThat(clientEx.getMessage()).contains("ApiHttpStatus: 504");
    }

    @Test
    @DisplayName("Q12-T15: Downstream recupera: falha temporaria em autorizacao seguida de sucesso restaura servico")
    void testRecuperacaoAposFalhaDownstream() {
        AtomicBoolean downstreamOnline = new AtomicBoolean(false);

        AuthServiceClient resilientAuthClient = new AuthServiceClient() {
            @Override
            public AutorizaResponseDto autoriza(AutorizaRequestDto autorizaRequestTO) {
                return null;
            }

            @Override
            public ConsultaAutorizacaoResponseDto consultaAutorizacao(ConsultaAutorizacaoRequestDto req) {
                if (!downstreamOnline.get()) {
                    throw new RuntimeException("Downstream auth-service connection timeout");
                }
                mppb.restapi.common.infrastructure.client.authapi.dto.UsuarioAutorizadoResponseDto usuario =
                        new mppb.restapi.common.infrastructure.client.authapi.dto.UsuarioAutorizadoResponseDto(1L, "Servidor Teste", "servidor.teste");
                return new ConsultaAutorizacaoResponseDto(null, usuario);
            }
        };

        PerfilUsuarioRepository repo = new PerfilUsuarioRepository(null) {
            @Override
            public PerfilUsuarioDto buscarPorLogin(String login) {
                if ("servidor.teste".equals(login)) {
                    PerfilUsuarioDto perfil = new PerfilUsuarioDto();
                    perfil.setNome("Servidor Teste");
                    perfil.setMatricula("99999");
                    return perfil;
                }
                return null;
            }
        };

        PerfilUsuarioService service = new PerfilUsuarioService(resilientAuthClient, repo);
        PerfilUsuarioController controller = new PerfilUsuarioController(service);

        // 1. Downstream offline: deve capturar a falha sem crash e retornar null / 401
        assertDoesNotThrow(() -> {
            PerfilUsuarioDto dtoFalha = service.buscarPerfilAutenticado("token-teste");
            assertThat(dtoFalha).isNull();
        });

        // 2. Downstream volta ao ar (recuperação): nova requisição é bem-sucedida sem reiniciar o processo
        downstreamOnline.set(true);
        assertDoesNotThrow(() -> {
            PerfilUsuarioDto dtoSucesso = service.buscarPerfilAutenticado("token-teste");
            assertThat(dtoSucesso).isNotNull();
            assertThat(dtoSucesso.getNome()).isEqualTo("Servidor Teste");
            assertThat(dtoSucesso.getMatricula()).isEqualTo("99999");
        });
    }

    // =========================================================================
    // Q12-T16: Auditoria de Logs e Dados Sensíveis
    // =========================================================================

    @Test
    @DisplayName("Q12-T16: Resposta de erro ProblemDetail nao vaza token bearer ou senha na mensagem do usuario")
    void testRespostaErroNaoVazaTokenOuSenha() {
        AppException ex = new AppException("Falha no login com token Bearer eyJhbGciOiJIUzI1NiJ9.secret");
        ResponseEntity<ProblemDetail> response = apiExceptionHandler.handleAppException(ex);

        assertThat(response.getBody()).isNotNull();
        // A mensagem ao usuário é a genérica e não o detalhe interno com o token
        assertThat(response.getBody().getDetail()).doesNotContain("eyJhbGciOiJIUzI1NiJ9");
        assertThat(response.getBody().getDetail()).contains("Ocorreu um erro inesperado");
    }

    // =========================================================================
    // Q12-T17: Auditoria de Correlação (traceId, requestId, MDC)
    // =========================================================================

    @Test
    @DisplayName("Q12-T17: ProblemDetail atual contem timestamp mas nao possui correlationId/traceId nativo (GAP auditado)")
    void testAuditoriaCorrelacaoMdcTraceId() {
        ProblemDetail problem = apiExceptionHandler.handleNotFoundException(new NotFoundException("Item")).getBody();

        assertThat(problem).isNotNull();
        assertThat(problem.getProperties()).containsKey("timestamp");
        // Constatação canônica Q12: Ausência de MDC/X-Request-ID/traceId na estrutura de erro atual
        assertThat(problem.getProperties()).doesNotContainKey("correlationId");
        assertThat(problem.getProperties()).doesNotContainKey("traceId");
        assertThat(problem.getProperties()).doesNotContainKey("requestId");
    }
}
