package mppb.restapi.mobile.quality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import mppb.restapi.common.infrastructure.client.authapi.dto.AutenticaRequestDto;
import mppb.restapi.common.infrastructure.env.RuntimeEnv;
import mppb.restapi.mobile.feature.notificacao.dto.DispositivoNotificacaoResponse;
import mppb.restapi.mobile.feature.notificacao.dto.RegistrarDispositivoRequest;
import mppb.restapi.mobile.feature.notificacao.model.DispositivoFcm;
import mppb.restapi.mobile.feature.notificacao.repository.DispositivoFcmRepository;
import mppb.restapi.mobile.feature.notificacao.service.DispositivoFcmService;
import mppb.restapi.mobile.feature.perfil.dto.PerfilUsuarioDto;

/**
 * Bateria de testes automatizados determinísticos da fase Q13:
 * Mutation Testing, Property-Based Testing e Fuzz Testing no backend Spring Boot.
 *
 * Seed canônica: 130013
 * Iterações por bateria: 500 casos determinísticos
 */
class Q13MutationPropertyFuzzTest {

    private static final long CANONICAL_SEED = 130013L;
    private static final int ITERATIONS = 500;

    private ApiExceptionHandler apiExceptionHandler;
    private RuntimeEnv runtimeEnv;
    private FeignErrorDecoder feignErrorDecoder;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        StaticMessageSource messageSource = new StaticMessageSource();
        runtimeEnv = new RuntimeEnv();
        // Em homolog/prod, trace não deve vazar
        setActiveProfiles("prod");
        apiExceptionHandler = new ApiExceptionHandler(messageSource, runtimeEnv);
        feignErrorDecoder = new FeignErrorDecoder();
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private void setActiveProfiles(String profiles) throws Exception {
        Field field = RuntimeEnv.class.getDeclaredField("activeProfiles");
        field.setAccessible(true);
        field.set(runtimeEnv, profiles);
    }

    // =========================================================================
    // Property P08: Backend Exception Mapping & Sanitization Invariant
    // =========================================================================

    @Test
    @DisplayName("P08: Invariante de mapeamento de exceções e proteção contra vazamento de stacktrace (500 iterações)")
    void testPropertyP08ExceptionMappingAndSanitization() {
        Random rng = new Random(CANONICAL_SEED);
        String[] sensitivePrefixes = {
            "SQL error near 'SELECT * FROM tb_usuario WHERE '1'='1''",
            "File not found: C:\\Windows\\System32\\drivers\\etc\\hosts",
            "/etc/shadow permission denied for user mppb",
            "Token expired: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummy_signature",
            "Connection failed to jdbc:postgresql://10.0.0.1:5432/mppb?password=secret_db_pass",
            "Validation failure in internal memory pointer 0x7FFEED20"
        };

        for (int i = 0; i < ITERATIONS; i++) {
            String sensitiveSnippet = sensitivePrefixes[i % sensitivePrefixes.length] + " [iter=" + i + ", rnd=" + rng.nextInt(10000) + "]";
            int typeSelector = i % 6;

            ResponseEntity<ProblemDetail> response;
            HttpStatus expectedStatus;

            switch (typeSelector) {
                case 0 -> {
                    expectedStatus = HttpStatus.UNPROCESSABLE_ENTITY;
                    response = apiExceptionHandler.handleBusinessRuleException(new BusinessRuleException(sensitiveSnippet));
                }
                case 1 -> {
                    expectedStatus = HttpStatus.NOT_FOUND;
                    response = apiExceptionHandler.handleNotFoundException(new NotFoundException(sensitiveSnippet));
                }
                case 2 -> {
                    expectedStatus = HttpStatus.SERVICE_UNAVAILABLE;
                    Request req = Request.create(HttpMethod.GET, "http://internal-service/api", Map.of(), null, StandardCharsets.UTF_8, null);
                    response = apiExceptionHandler.handleRetryableException(new RetryableException(503, sensitiveSnippet, HttpMethod.GET, new Date(), req));
                }
                case 3 -> {
                    expectedStatus = HttpStatus.BAD_GATEWAY;
                    response = apiExceptionHandler.handleClientApiException(new ClientApiException(502, sensitiveSnippet));
                }
                case 4 -> {
                    expectedStatus = HttpStatus.FORBIDDEN;
                    response = apiExceptionHandler.handleAccessDeniedException(new AccessDeniedException(sensitiveSnippet));
                }
                default -> {
                    expectedStatus = HttpStatus.INTERNAL_SERVER_ERROR;
                    MockHttpServletResponse mockResponse = new MockHttpServletResponse();
                    response = apiExceptionHandler.handleGenericException(new RuntimeException(sensitiveSnippet), mockResponse);
                }
            }

            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(expectedStatus);

            ProblemDetail body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getTitle()).isNotBlank();
            assertThat(body.getProperties()).containsKey("timestamp");

            // INVARIANTE CRÍTICA: em profile prod, a propriedade 'trace' NUNCA deve existir
            assertThat(body.getProperties().get("trace"))
                    .as("Trace vazado na iteracao %d para status %s", i, expectedStatus)
                    .isNull();

            // INVARIANTE: para erro 500 genérico, a mensagem pública deve ser mascarada
            if (expectedStatus == HttpStatus.INTERNAL_SERVER_ERROR) {
                assertThat(body.getDetail()).isEqualTo("Ocorreu um erro inesperado no sistema. Por favor, tente novamente mais tarde.");
                assertThat(body.getDetail()).doesNotContain("password=");
                assertThat(body.getDetail()).doesNotContain("SELECT *");
            }
        }
    }

    // =========================================================================
    // Property P09: FCM Token Hashing & Normalization Invariant (Kills MUT-BACKEND-08)
    // =========================================================================

    @Test
    @DisplayName("P09: Invariante de hash SHA-256 e normalização em DispositivoFcmService (500 iterações, mata MUT-BACKEND-08)")
    void testPropertyP09FcmTokenHashingAndNormalization() throws Exception {
        Random rng = new Random(CANONICAL_SEED);
        MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");

        AtomicReference<String> capturedTokenHash = new AtomicReference<>();
        AtomicReference<String> capturedPlataforma = new AtomicReference<>();

        DispositivoFcmRepository mockRepo = new DispositivoFcmRepository(null) {
            @Override
            public Optional<DispositivoFcm> findByTokenHash(String tokenHash) {
                capturedTokenHash.set(tokenHash);
                return Optional.empty();
            }

            @Override
            public Optional<DispositivoFcm> findByIdentificador(String identificador) {
                return Optional.empty();
            }

            @Override
            public DispositivoFcm inserir(String identificador, String servidorMatricula, String tokenFcm, String tokenHash, String plataforma) {
                capturedTokenHash.set(tokenHash);
                capturedPlataforma.set(plataforma);
                return new DispositivoFcm(1L, identificador, servidorMatricula, tokenFcm, tokenHash, plataforma, true, java.time.LocalDateTime.now(), java.time.LocalDateTime.now(), null);
            }
        };

        DispositivoFcmService service = new DispositivoFcmService(mockRepo);

        for (int i = 0; i < ITERATIONS; i++) {
            // Gerar token FCM sintético variado
            int tokenLen = 16 + rng.nextInt(128);
            StringBuilder sb = new StringBuilder(tokenLen);
            for (int k = 0; k < tokenLen; k++) {
                char c = (char) ('a' + rng.nextInt(26));
                sb.append(c);
            }
            String rawToken = sb.toString();

            // Calcular SHA-256 de referência canônico
            byte[] expectedHashBytes = sha256Digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : expectedHashBytes) {
                hexString.append(String.format("%02x", b));
            }
            String expectedHash = hexString.toString();

            // Plataforma com casing variado
            String rawPlatform = (i % 2 == 0) ? "AnDrOiD" : "iOs";
            String expectedPlatform = (i % 2 == 0) ? "ANDROID" : "IOS";

            RegistrarDispositivoRequest request = new RegistrarDispositivoRequest(
                    "dev-ident-" + i,
                    rawToken,
                    rawPlatform
            );

            DispositivoNotificacaoResponse resp = service.registrar("99999", request);

            assertThat(resp).isNotNull();
            // INVARIANTE 1: hash passado ao repositório DEVE ser exatamente o SHA-256 canônico em minúsculas
            // Se MUT-BACKEND-08 omitir o sha256 e passar o token bruto, esta asserção FALHA imediatamente!
            assertThat(capturedTokenHash.get())
                    .as("MUT-BACKEND-08: TokenHash deve coincidir com o hash SHA-256 de 64 caracteres na iteracao %d", i)
                    .isEqualTo(expectedHash);

            assertThat(capturedTokenHash.get()).hasSize(64);
            assertThat(capturedTokenHash.get()).matches("^[0-9a-f]{64}$");

            // INVARIANTE 2: plataforma normalizada para maiúsculo
            assertThat(capturedPlataforma.get()).isEqualTo(expectedPlatform);
        }
    }

    // =========================================================================
    // Fuzz F05: Backend DTO Jackson Deserialization (500 casos)
    // =========================================================================

    @Test
    @DisplayName("F05: Fuzzing de serialização/desserialização Jackson de DTOs backend (500 casos)")
    void testFuzzF05BackendDtoJackson() {
        Random rng = new Random(CANONICAL_SEED);

        String[] basePayloads = {
            "{}",
            "{\"tokenFcm\":\"sampleToken123\",\"plataforma\":\"ANDROID\"}",
            "{\"identificador\":\"id-xyz\",\"tokenFcm\":\"valid-token\",\"plataforma\":\"IOS\"}",
            "{\"login\":\"user123\",\"nome\":\"Maria Silva\",\"matricula\":\"12345\"}",
            "{\"usuario\":\"user.test\",\"senha\":\"pass123\"}"
        };

        for (int i = 0; i < ITERATIONS; i++) {
            int mutationType = rng.nextInt(7);
            String rawPayload = basePayloads[i % basePayloads.length];
            String payload;

            switch (mutationType) {
                case 0 -> payload = rawPayload;
                case 1 -> payload = ""; // vazio
                case 2 -> payload = "    "; // whitespace
                case 3 -> payload = rawPayload.substring(0, rawPayload.length() / 2); // truncado/malformado
                case 4 -> {
                    // Propriedades desconhecidas e injection
                    payload = rawPayload.replace("}", ",\"unknown_prop_" + i + "\":\"fuzz-val-" + rng.nextInt(1000) + "\"}");
                }
                case 5 -> {
                    // Tipos invertidos (número em vez de string)
                    payload = rawPayload.replace("\"ANDROID\"", "12345").replace("\"user123\"", "null");
                }
                default -> {
                    // Strings extremas e unicode
                    String extremeStr = "A".repeat(1 + (i % 256)) + "🚀🧪\u200B\uFEFF";
                    payload = rawPayload.replace("sampleToken123", extremeStr);
                }
            }

            // CRITÉRIO F05: Jackson desserializa com sucesso ou lança JsonProcessingException / IllegalArgumentException controlada
            // NUNCA deve ocorrer OutOfMemoryError, StackOverflowError ou encerramento anômalo da JVM
            final String testPayload = payload;
            assertDoesNotThrow(() -> {
                try {
                    if (testPayload.contains("tokenFcm")) {
                        objectMapper.readValue(testPayload, RegistrarDispositivoRequest.class);
                    } else if (testPayload.contains("matricula")) {
                        objectMapper.readValue(testPayload, PerfilUsuarioDto.class);
                    } else {
                        objectMapper.readValue(testPayload, AutenticaRequestDto.class);
                    }
                } catch (JsonProcessingException e) {
                    // Exceção esperada e controlada para payloads malformados
                    assertThat(e.getMessage()).isNotNull();
                } catch (IllegalArgumentException e) {
                    // Exceção de parsing de tipos controlada
                    assertThat(e.getMessage()).isNotNull();
                }
            }, "Fuzz F05 causou crash inesperado na iteracao " + i + " com payload: " + testPayload);
        }
    }

    // =========================================================================
    // Fuzz F06: Exception Handler Public Response Sanitization (500 casos)
    // =========================================================================

    @Test
    @DisplayName("F06: Fuzzing de mensagens e dados maliciosos no ApiExceptionHandler (500 casos)")
    void testFuzzF06ExceptionHandlerSanitization() {
        Random rng = new Random(CANONICAL_SEED);

        String[] maliciousVectors = {
            "'; DROP TABLE tb_usuario; --",
            "<script>alert('XSS')</script>",
            "C:\\inetpub\\wwwroot\\web.config",
            "/etc/passwd\u0000.png",
            "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummy",
            "password=my_ultra_secret_pw",
            "\uD83D\uDCA3\uD83D\uDD25\u0000\n\r\t",
            "A".repeat(500)
        };

        for (int i = 0; i < ITERATIONS; i++) {
            String vector = maliciousVectors[rng.nextInt(maliciousVectors.length)] + " [" + i + "]";
            Exception ex = (i % 2 == 0) ? new RuntimeException(vector) : new BusinessRuleException(vector);

            ResponseEntity<ProblemDetail> response;
            if (ex instanceof BusinessRuleException bre) {
                response = apiExceptionHandler.handleBusinessRuleException(bre);
            } else {
                MockHttpServletResponse servletResponse = new MockHttpServletResponse();
                response = apiExceptionHandler.handleGenericException(ex, servletResponse);
            }

            assertThat(response).isNotNull();
            ProblemDetail body = response.getBody();
            assertThat(body).isNotNull();

            // Sanitização: trace nunca deve ser incluído em prod
            assertThat(body.getProperties().get("trace")).isNull();

            // Para 500, a mensagem de saída nunca expõe o vetor sensível
            if (response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                assertThat(body.getDetail()).isEqualTo("Ocorreu um erro inesperado no sistema. Por favor, tente novamente mais tarde.");
                assertThat(body.getDetail()).doesNotContain("DROP TABLE");
                assertThat(body.getDetail()).doesNotContain("password=");
                assertThat(body.getDetail()).doesNotContain("etc/passwd");
            }
        }
    }

    // =========================================================================
    // Fuzz F07: FeignErrorDecoder HTTP Status and Body Fuzzing (500 casos, mata MUT-BACKEND-06)
    // =========================================================================

    @Test
    @DisplayName("F07: Fuzzing de status HTTP e bodies no FeignErrorDecoder (500 casos, mata MUT-BACKEND-06)")
    void testFuzzF07FeignErrorDecoder() {
        Random rng = new Random(CANONICAL_SEED);

        int[] sampleStatuses = {
            400, 401, 403, 404, 409, 422, 429, // 4xx
            500, 502, 503, 504, 599,           // 5xx
            200, 204, 301, 302, 399, 600, 601  // fora de [400, 600)
        };

        for (int i = 0; i < ITERATIONS; i++) {
            int status = sampleStatuses[rng.nextInt(sampleStatuses.length)];
            int bodyType = rng.nextInt(5);

            String bodyContent;
            switch (bodyType) {
                case 0 -> bodyContent = null;
                case 1 -> bodyContent = "";
                case 2 -> bodyContent = "Normal error body " + i;
                case 3 -> bodyContent = "A".repeat(1 + rng.nextInt(500));
                default -> bodyContent = "Unicode \uD83D\uDD25\uD83D\uDCA5 accents: áéíóúç \n \r \t";
            }

            Response.Builder responseBuilder = Response.builder()
                    .status(status)
                    .reason("Reason-" + status)
                    .request(Request.create(HttpMethod.GET, "http://upstream/fuzz", Map.of(), null, StandardCharsets.UTF_8, null))
                    .headers(Map.of());

            if (bodyContent != null) {
                responseBuilder.body(bodyContent, StandardCharsets.UTF_8);
            }

            Response feignResponse = responseBuilder.build();
            String methodKey = "UpstreamClient#endpoint" + i;

            // CRITÉRIO F07: o decoder NUNCA deve lançar NullPointerException ou travar
            Exception decoded = feignErrorDecoder.decode(methodKey, feignResponse);
            assertThat(decoded).isNotNull();

            if (status >= 400 && status < 600) {
                // INVARIANTE CRÍTICA (KILLS MUT-BACKEND-06):
                // Para QUALQUER status entre 400 e 599 (inclusive 4xx como 400, 404, 422),
                // o FeignErrorDecoder DEVE produzir ClientApiException!
                // Se MUT-BACKEND-06 alterar 'status >= 400' para 'status > 500', falhará para 4xx!
                assertThat(decoded)
                        .as("MUT-BACKEND-06: Status %d deve decodificar para ClientApiException na iteracao %d", status, i)
                        .isInstanceOf(ClientApiException.class);

                ClientApiException clientEx = (ClientApiException) decoded;
                assertThat(clientEx.getMessage()).contains(methodKey);
                assertThat(clientEx.getMessage()).contains("ApiHttpStatus: " + status);
                if (bodyType == 2) {
                    assertThat(clientEx.getMessage())
                            .as("MUT-BACKEND-07: Body deve ser incorporado na mensagem na iteracao %d", i)
                            .contains(bodyContent);
                }
            } else {
                // Fora da faixa [400, 600), usa o fallback padrão do Feign
                assertThat(decoded).isNotInstanceOf(ClientApiException.class);
            }
        }
    }
}
