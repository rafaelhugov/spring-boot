package mppb.restapi.mobile.quality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import mppb.restapi.common.infrastructure.client.authapi.AuthServiceClient;
import mppb.restapi.common.infrastructure.client.authapi.dto.AutorizaRequestDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.AutorizaResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoRequestDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoResponseDto;
import mppb.restapi.common.infrastructure.security.authorization.TokenAuthentication;
import mppb.restapi.mobile.core.config.ValidadorConfiguracaoSegura;
import mppb.restapi.mobile.feature.carteirafuncional.dto.CarteiraFuncionalTO;
import mppb.restapi.mobile.feature.carteirafuncional.service.CarteiraService;
import mppb.restapi.mobile.feature.concessaofolga.dto.OperacaoSolicitacaoRequestDto;
import mppb.restapi.mobile.feature.concessaofolga.dto.SolicitacaoMarcacaoFeriasRequestDto;
import mppb.restapi.mobile.feature.concessaofolga.service.ConcessaoFolgaService;
import mppb.restapi.mobile.feature.contracheque.dto.ContrachequeResponseDto;
import mppb.restapi.mobile.feature.contracheque.service.ContrachequeService;
import mppb.restapi.mobile.feature.designacao.dto.DesignacoesAtivasResponse;
import mppb.restapi.mobile.feature.pagamentos.dto.PagamentosResponseDto;
import mppb.restapi.mobile.feature.pagamentos.service.PagamentosService;
import mppb.restapi.mobile.feature.perfil.dto.PerfilUsuarioDto;
import mppb.restapi.mobile.feature.perfil.service.PerfilUsuarioService;
import mppb.restapi.mobile.feature.synthetic.audit.AuditoriaUsuarioSintetico;
import mppb.restapi.mobile.feature.synthetic.auth.AutenticadorStoreReview;
import mppb.restapi.mobile.feature.synthetic.auth.AutenticadorUsuarioSintetico;
import mppb.restapi.mobile.feature.synthetic.auth.SyntheticTokenAuthenticationProvider;
import mppb.restapi.mobile.feature.synthetic.config.StoreReviewProperties;
import mppb.restapi.mobile.feature.synthetic.config.SyntheticUsersProperties;
import mppb.restapi.mobile.feature.synthetic.controller.AutenticacaoSinteticaController;
import mppb.restapi.mobile.feature.synthetic.dataset.SyntheticDataService;
import mppb.restapi.mobile.feature.synthetic.model.IdentidadeSintetica;
import mppb.restapi.mobile.feature.synthetic.sandbox.SyntheticSandboxStorage;

@DisplayName("STORE-REVIEW-R02: Implementação e Validação do Contrato de Store Review Isolado")
public class StoreReviewIsoladoTest {

    private static final String UAT_SECRET = "segredo-de-uat-32-caracteres-minimo-123";
    private static final String REVIEW_SECRET = "segredo-de-review-exclusivo-32-chars-456";

    private SyntheticUsersProperties syntheticProperties;
    private StoreReviewProperties reviewProperties;
    private AuditoriaUsuarioSintetico auditoria;
    private AutenticadorUsuarioSintetico autenticadorUat;
    private AutenticadorStoreReview autenticadorReview;
    private SyntheticTokenAuthenticationProvider authProvider;
    private AutenticacaoSinteticaController authController;
    private SyntheticDataService dataService;
    private SyntheticSandboxStorage sandboxStorage;
    private FakeAuthServiceClient fakeAuthClient;
    private MockEnvironment reviewEnvironment;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        reviewEnvironment = new MockEnvironment();
        reviewEnvironment.setActiveProfiles("review");

        syntheticProperties = new SyntheticUsersProperties();
        syntheticProperties.setEnabled(true);
        syntheticProperties.setJwtSecret(UAT_SECRET);

        Map<String, String> senhas = new HashMap<>();
        senhas.put("uat-membro", "SenhaUatMembro123!");
        senhas.put("uat-servidor", "SenhaUatServidor123!");
        senhas.put("google-review-membro", "SenhaGoogleMembro456!");
        senhas.put("google-review-servidor", "SenhaGoogleServidor456!");
        senhas.put("apple-review-membro", "SenhaAppleMembro789!");
        senhas.put("apple-review-servidor", "SenhaAppleServidor789!");
        syntheticProperties.setPasswords(senhas);

        reviewProperties = new StoreReviewProperties();
        reviewProperties.setEnabled(true);
        reviewProperties.setJwtSecret(REVIEW_SECRET);
        reviewProperties.setTokenTtlSeconds(86400);
        reviewProperties.setIssuer("mppb-store-review");
        reviewProperties.setAudience("mppb-mobile-review");

        auditoria = new AuditoriaUsuarioSintetico();
        autenticadorUat = new AutenticadorUsuarioSintetico(syntheticProperties, auditoria);
        autenticadorReview = new AutenticadorStoreReview(reviewProperties, syntheticProperties, auditoria);

        authProvider = new SyntheticTokenAuthenticationProvider(autenticadorUat, autenticadorReview, auditoria, reviewEnvironment);
        authController = new AutenticacaoSinteticaController(autenticadorUat, autenticadorReview, reviewProperties, reviewEnvironment);

        sandboxStorage = new SyntheticSandboxStorage(auditoria);
        dataService = new SyntheticDataService(autenticadorUat, auditoria, autenticadorReview);
        fakeAuthClient = new FakeAuthServiceClient();

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("01. profile review + feature ligada = startup valido")
    void test01_profileReviewEFeatureLigada_startupValido() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("review");
        env.setProperty("review.enabled", "true");
        env.setProperty("review.jwt-secret", REVIEW_SECRET);
        env.setProperty("uat.synthetic-users.passwords.google-review-membro", "senha1");
        env.setProperty("uat.synthetic-users.passwords.google-review-servidor", "senha2");
        env.setProperty("uat.synthetic-users.passwords.apple-review-membro", "senha3");
        env.setProperty("uat.synthetic-users.passwords.apple-review-servidor", "senha4");

        ValidadorConfiguracaoSegura validador = new ValidadorConfiguracaoSegura(env);
        validador.validar();
    }

    @Test
    @DisplayName("02. prod + review feature ou synthetic = startup recusado")
    void test02_prodComReviewFeatureOuSynthetic_startupRecusado() {
        // Cenário A: prod com review.enabled=true
        MockEnvironment envA = new MockEnvironment();
        envA.setActiveProfiles("prod");
        envA.setProperty("review.enabled", "true");
        ValidadorConfiguracaoSegura validadorA = new ValidadorConfiguracaoSegura(envA);
        IllegalStateException exA = assertThrows(IllegalStateException.class, validadorA::validar);
        assertTrue(exA.getMessage().contains("Store Review nao pode ser habilitado em perfis de producao"));

        // Cenário B: prod com uat.synthetic-users.enabled=true
        MockEnvironment envB = new MockEnvironment();
        envB.setActiveProfiles("prod");
        envB.setProperty("uat.synthetic-users.enabled", "true");
        ValidadorConfiguracaoSegura validadorB = new ValidadorConfiguracaoSegura(envB);
        IllegalStateException exB = assertThrows(IllegalStateException.class, validadorB::validar);
        assertTrue(exB.getMessage().contains("Usuarios sinteticos UAT/Review nao podem ser habilitados em perfis de producao"));

        // Cenário C: perfil híbrido proibido prod,review
        MockEnvironment envC = new MockEnvironment();
        envC.setActiveProfiles("prod", "review");
        ValidadorConfiguracaoSegura validadorC = new ValidadorConfiguracaoSegura(envC);
        IllegalStateException exC = assertThrows(IllegalStateException.class, validadorC::validar);
        assertTrue(exC.getMessage().contains("Perfil review nao pode ser combinado com perfis de producao"));

        // Cenário D: prod com STORE_REVIEW_ENABLED=true
        MockEnvironment envD = new MockEnvironment();
        envD.setActiveProfiles("prod");
        envD.setProperty("STORE_REVIEW_ENABLED", "true");
        ValidadorConfiguracaoSegura validadorD = new ValidadorConfiguracaoSegura(envD);
        IllegalStateException exD = assertThrows(IllegalStateException.class, validadorD::validar);
        assertTrue(exD.getMessage().contains("Store Review nao pode ser habilitado em perfis de producao"));
    }

    @Test
    @DisplayName("03. quatro identidades review validas autenticam com sucesso e recebem token review")
    void test03_quatroIdentidadesValidas_autenticamComSucesso() throws Exception {
        String[] logins = {
                "membro.google",
                "servidor.google",
                "membro.apple",
                "servidor.apple"
        };
        String[] senhas = {
                "SenhaGoogleMembro456!",
                "SenhaGoogleServidor456!",
                "SenhaAppleMembro789!",
                "SenhaAppleServidor789!"
        };

        for (int i = 0; i < logins.length; i++) {
            AutorizaRequestDto request = new AutorizaRequestDto(logins[i], senhas[i], "appmppb");

            mockMvc.perform(post("/acesso/autoriza")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(result -> {
                        String body = result.getResponse().getContentAsString();
                        AutorizaResponseDto resp = objectMapper.readValue(body, AutorizaResponseDto.class);
                        assertTrue(resp.getToken().startsWith(AutenticadorStoreReview.REVIEW_TOKEN_PREFIX));
                    });
        }
    }

    @Test
    @DisplayName("04. senha errada = 401")
    void test04_senhaErrada_retorna401() throws Exception {
        AutorizaRequestDto request = new AutorizaRequestDto("membro.google", "SenhaIncorreta!", "appmppb");

        mockMvc.perform(post("/acesso/autoriza")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("security.invalid-credentials"));
    }

    @Test
    @DisplayName("05. login desconhecido = 401")
    void test05_loginDesconhecido_retorna401() throws Exception {
        AutorizaRequestDto request = new AutorizaRequestDto("review.google.qualquer", "Senha123!", "appmppb");

        mockMvc.perform(post("/acesso/autoriza")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("security.invalid-credentials"));
    }

    @Test
    @DisplayName("06. uat.membro e uat.servidor = 401 no backend review")
    void test06_uatMembroEUatServidor_retornam401NoAmbienteReview() throws Exception {
        AutorizaRequestDto reqMembro = new AutorizaRequestDto("uat.membro", "SenhaUatMembro123!", "appmppb");

        mockMvc.perform(post("/acesso/autoriza")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqMembro)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("security.invalid-credentials"));

        AutorizaRequestDto reqServidor = new AutorizaRequestDto("uat.servidor", "SenhaUatServidor123!", "appmppb");

        mockMvc.perform(post("/acesso/autoriza")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqServidor)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("security.invalid-credentials"));
    }

    @Test
    @DisplayName("07. usuario institucional = 401 no backend review")
    void test07_usuarioInstitucional_retorna401NoAmbienteReview() throws Exception {
        AutorizaRequestDto request = new AutorizaRequestDto("promotor.institucional", "SenhaInstitucionalReal123!", "appmppb");

        mockMvc.perform(post("/acesso/autoriza")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("security.invalid-credentials"));
    }

    @Test
    @DisplayName("08. AuthServiceClient = zero interacoes")
    void test08_authServiceClient_zeroInteracoes() {
        assertEquals(0, fakeAuthClient.getChamadasAutoriza());
        assertEquals(0, fakeAuthClient.getChamadasConsulta());
    }

    @Test
    @DisplayName("09. issuer correto (mppb-store-review)")
    void test09_issuerCorreto() throws Exception {
        Optional<String> tokenOpt = autenticadorReview.autenticarReview("membro.google", "SenhaGoogleMembro456!");
        assertTrue(tokenOpt.isPresent());

        String token = tokenOpt.get();
        String jwtPart = token.substring(AutenticadorStoreReview.REVIEW_TOKEN_PREFIX.length());
        String payloadB64 = jwtPart.split("\\.")[1];
        JsonNode payload = objectMapper.readTree(Base64.getUrlDecoder().decode(payloadB64));

        assertEquals("mppb-store-review", payload.path("iss").asText());
    }

    @Test
    @DisplayName("10. audience correto (mppb-mobile-review)")
    void test10_audienceCorreto() throws Exception {
        Optional<String> tokenOpt = autenticadorReview.autenticarReview("servidor.apple", "SenhaAppleServidor789!");
        assertTrue(tokenOpt.isPresent());

        String token = tokenOpt.get();
        String jwtPart = token.substring(AutenticadorStoreReview.REVIEW_TOKEN_PREFIX.length());
        String payloadB64 = jwtPart.split("\\.")[1];
        JsonNode payload = objectMapper.readTree(Base64.getUrlDecoder().decode(payloadB64));

        assertEquals("mppb-mobile-review", payload.path("aud").asText());
    }

    @Test
    @DisplayName("11. token adulterado rejeitado")
    void test11_tokenAdulteradoRejeitado() {
        Optional<String> tokenOpt = autenticadorReview.autenticarReview("membro.google", "SenhaGoogleMembro456!");
        assertTrue(tokenOpt.isPresent());

        String tokenValido = tokenOpt.get();
        String tokenAdulterado = tokenValido.substring(0, tokenValido.length() - 4) + "XXXX";

        Optional<IdentidadeSintetica> res = autenticadorReview.validarTokenReview(tokenAdulterado);
        assertFalse(res.isPresent());

        TokenAuthentication tokenAuth = new TokenAuthentication(tokenAdulterado);
        assertThrows(BadCredentialsException.class, () -> authProvider.authenticate(tokenAuth));
    }

    @Test
    @DisplayName("12. token expirado rejeitado")
    void test12_tokenExpiradoRejeitado() {
        reviewProperties.setTokenTtlSeconds(-10);
        AutenticadorStoreReview autenticadorExpirador = new AutenticadorStoreReview(reviewProperties, syntheticProperties, auditoria);

        String tokenExpirado = autenticadorExpirador.gerarTokenReview(IdentidadeSintetica.REVIEW_GOOGLE_MEMBRO);
        Optional<IdentidadeSintetica> res = autenticadorReview.validarTokenReview(tokenExpirado);
        assertFalse(res.isPresent());

        TokenAuthentication tokenAuth = new TokenAuthentication(tokenExpirado);
        assertThrows(BadCredentialsException.class, () -> authProvider.authenticate(tokenAuth));
    }

    @Test
    @DisplayName("13. segredo ausente ou igual ao de UAT = fail-fast")
    void test13_segredoAusenteOuIgualUat_failFast() {
        // Cenário A: secret ausente
        MockEnvironment envA = new MockEnvironment();
        envA.setActiveProfiles("review");
        envA.setProperty("review.enabled", "true");
        ValidadorConfiguracaoSegura valA = new ValidadorConfiguracaoSegura(envA);
        IllegalStateException exA = assertThrows(IllegalStateException.class, valA::validar);
        assertTrue(exA.getMessage().contains("review.jwt-secret obrigatorio"));

        // Cenário B: secret igual ao de UAT
        MockEnvironment envB = new MockEnvironment();
        envB.setActiveProfiles("review");
        envB.setProperty("review.enabled", "true");
        envB.setProperty("review.jwt-secret", UAT_SECRET);
        envB.setProperty("uat.synthetic-users.jwt-secret", UAT_SECRET);
        ValidadorConfiguracaoSegura valB = new ValidadorConfiguracaoSegura(envB);
        IllegalStateException exB = assertThrows(IllegalStateException.class, valB::validar);
        assertTrue(exB.getMessage().contains("A chave de assinatura do Store Review deve ser exclusiva e diferente da chave de UAT"));
    }

    @Test
    @DisplayName("14. review.enabled=false bloqueia contas")
    void test14_reviewEnabledFalse_bloqueiaContas() {
        reviewProperties.setEnabled(false);
        Optional<String> tokenOpt = autenticadorReview.autenticarReview("membro.google", "SenhaGoogleMembro456!");
        assertFalse(tokenOpt.isPresent());

        AutorizaRequestDto request = new AutorizaRequestDto("membro.google", "SenhaGoogleMembro456!", "appmppb");

        ResponseEntity<?> response = authController.autoriza(request);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("15. review.valid-until expirado bloqueia contas")
    void test15_reviewValidUntilExpirado_bloqueiaContas() {
        reviewProperties.setValidUntil("2020-01-01T00:00:00Z");
        assertTrue(reviewProperties.isExpirado());

        Optional<String> tokenOpt = autenticadorReview.autenticarReview("membro.apple", "SenhaAppleMembro789!");
        assertFalse(tokenOpt.isPresent());

        AutorizaRequestDto request = new AutorizaRequestDto("membro.apple", "SenhaAppleMembro789!", "appmppb");

        ResponseEntity<?> response = authController.autoriza(request);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("16. datasets membro/servidor corretamente segregados")
    void test16_datasetsMembroEServidor_corretamenteSegregados() {
        // Validação Membro: membro.google e membro.apple
        for (String loginMembro : List.of("membro.google", "membro.apple")) {
            CarteiraFuncionalTO carteira = dataService.getCarteiraByLogin(loginMembro);
            assertEquals("Lucas Almeida Monteiro", carteira.getNome());
            assertEquals("Promotor de Justiça", carteira.getCargo());
            assertEquals("900001", carteira.getMatricula());

            PerfilUsuarioDto perfil = dataService.getPerfilByLogin(loginMembro);
            assertEquals("Lucas Almeida Monteiro", perfil.getNome());
            assertEquals("Promotor de Justiça", perfil.getCargo());

            DesignacoesAtivasResponse desig = dataService.getDesignacoesAtivas("900001");
            assertEquals(1, desig.getDesignacoes().size());
            assertEquals("Ato PGJ nº 101/2025", desig.getDesignacoes().get(0).getAto());
        }

        // Validação Servidor: servidor.google e servidor.apple
        for (String loginServidor : List.of("servidor.google", "servidor.apple")) {
            CarteiraFuncionalTO carteira = dataService.getCarteiraByLogin(loginServidor);
            assertEquals("Mariana Costa Ribeiro", carteira.getNome());
            assertEquals("Analista Ministerial", carteira.getCargo());
            assertEquals("900002", carteira.getMatricula());

            PerfilUsuarioDto perfil = dataService.getPerfilByLogin(loginServidor);
            assertEquals("Mariana Costa Ribeiro", perfil.getNome());
            assertEquals("Analista Ministerial", perfil.getCargo());

            DesignacoesAtivasResponse desig = dataService.getDesignacoesAtivas("900002");
            assertEquals(0, desig.getDesignacoes().size());
        }
    }

    @Test
    @DisplayName("17. nenhum repository real acionado")
    void test17_nenhumRepositoryRealAcionado() {
        CarteiraService carteiraService = new CarteiraService(null, null, dataService);
        CarteiraFuncionalTO carteira = carteiraService.recuperaCarteiraFuncionalByLogin("membro.google");
        assertNotNull(carteira);
        assertEquals("900001", carteira.getMatricula());

        String tokenMembro = autenticadorReview.gerarTokenReview(IdentidadeSintetica.REVIEW_GOOGLE_MEMBRO);
        PerfilUsuarioService perfilService = new PerfilUsuarioService(null, null, dataService);
        PerfilUsuarioDto perfil = perfilService.buscarPerfilAutenticado(tokenMembro);
        assertNotNull(perfil);
        assertEquals("Lucas Almeida Monteiro", perfil.getNome());

        PagamentosService pagService = new PagamentosService(null, dataService);
        PagamentosResponseDto pag = pagService.listarPagamentos("2026", "00000000191");
        assertNotNull(pag);
        assertEquals(2, pag.getPagamentos().size());

        ContrachequeService contraService = new ContrachequeService(null, dataService);
        ContrachequeResponseDto cc = contraService.detalharContracheque(900001, 3, 2026);
        assertNotNull(cc);
        assertNotNull(cc.getMesAno());
    }

    @Test
    @DisplayName("18. mutacoes permanecem em sandbox isolado")
    void test18_mutacoesPermanecemEmSandbox() {
        sandboxStorage.reset();

        ConcessaoFolgaService service = new ConcessaoFolgaService(null, null, null, dataService, sandboxStorage);
        String tokenMembro = autenticadorReview.gerarTokenReview(IdentidadeSintetica.REVIEW_GOOGLE_MEMBRO);

        // Agendar gozo
        SolicitacaoMarcacaoFeriasRequestDto req = new SolicitacaoMarcacaoFeriasRequestDto();
        req.setDataInicial("2026-11-01");
        req.setNumeroDias(15);

        ResponseEntity<String> criada = service.cadastrarSolicitacao(101L, req, tokenMembro);
        assertEquals(HttpStatus.OK, criada.getStatusCode());

        // Cancelar no sandbox
        ResponseEntity<String> cancelada = service.cancelarSolicitacao(5001L, tokenMembro);
        assertEquals(HttpStatus.OK, cancelada.getStatusCode());
        assertTrue(cancelada.getBody().contains("CANCELADA"));
    }

    @Test
    @DisplayName("19. cross-object authorization continua bloqueada entre identidades review")
    void test19_crossObjectAuthorizationContinuaBloqueada() {
        sandboxStorage.reset();
        ConcessaoFolgaService service = new ConcessaoFolgaService(null, null, null, dataService, sandboxStorage);

        String tokenMembro = autenticadorReview.gerarTokenReview(IdentidadeSintetica.REVIEW_GOOGLE_MEMBRO);
        String tokenServidor = autenticadorReview.gerarTokenReview(IdentidadeSintetica.REVIEW_APPLE_SERVIDOR);

        // Membro consulta solicitacao 5001
        ResponseEntity<String> consultaMembro = service.consultarSolicitacao(5001L, tokenMembro);
        assertEquals(HttpStatus.OK, consultaMembro.getStatusCode());

        // Servidor (900002) tenta consultar a solicitacao 5001 do Membro (900001) -> 403 Forbidden
        ResponseEntity<String> consultaServidor = service.consultarSolicitacao(5001L, tokenServidor);
        assertEquals(HttpStatus.FORBIDDEN, consultaServidor.getStatusCode());

        // Servidor (900002) tenta cancelar a solicitacao 5001 do Membro (900001) -> 403 Forbidden
        ResponseEntity<String> cancelamentoServidor = service.cancelarSolicitacao(5001L, tokenServidor);
        assertEquals(HttpStatus.FORBIDDEN, cancelamentoServidor.getStatusCode());

        // Servidor (900002) tenta adiar a solicitacao 5001 do Membro (900001) -> 403 Forbidden
        OperacaoSolicitacaoRequestDto adiarDto = new OperacaoSolicitacaoRequestDto();
        adiarDto.setDataAdiamento("2026-12-01");
        ResponseEntity<String> adiamentoServidor = service.adiarSolicitacao(5001L, adiarDto, tokenServidor);
        assertEquals(HttpStatus.FORBIDDEN, adiamentoServidor.getStatusCode());

        // Estado do Membro permanece inalterado
        ResponseEntity<String> consultaMembroFinal = service.consultarSolicitacao(5001L, tokenMembro);
        assertEquals(HttpStatus.OK, consultaMembroFinal.getStatusCode());
        assertTrue(consultaMembroFinal.getBody().contains("HOMOLOGADA"));
    }

    @Test
    @DisplayName("20. nenhum segredo versionado")
    void test20_nenhumSegredoVersionado() {
        MockEnvironment env = new MockEnvironment();
        assertNull(env.getProperty("review.jwt-secret"));
        assertNull(env.getProperty("uat.synthetic-users.jwt-secret"));
        assertNull(env.getProperty("uat.synthetic-users.passwords.google-review-membro"));
        assertNull(env.getProperty("uat.synthetic-users.passwords.apple-review-membro"));
    }

    @Test
    @DisplayName("21. variaveis de ambiente Portainer mapeiam corretamente e permitem startup")
    void test21_variaveisAmbientePortainerMapeiamCorretamenteEPermitemStartup() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("review");
        env.setProperty("STORE_REVIEW_ENABLED", "true");
        env.setProperty("STORE_REVIEW_JWT_SECRET", "super-secret-jwt-key-portainer-review-12345");
        env.setProperty("GOOGLE_REVIEW_MEMBER_PASSWORD", "PassGoogleMembro123!");
        env.setProperty("GOOGLE_REVIEW_SERVER_PASSWORD", "PassGoogleServidor123!");
        env.setProperty("APPLE_REVIEW_MEMBER_PASSWORD", "PassAppleMembro123!");
        env.setProperty("APPLE_REVIEW_SERVER_PASSWORD", "PassAppleServidor123!");

        ValidadorConfiguracaoSegura validador = new ValidadorConfiguracaoSegura(env);
        assertDoesNotThrow(validador::validar);
    }

    @Test
    @DisplayName("22. review.enabled=true com segredo ausente resulta em fail-fast")
    void test22_reviewEnabledTrue_segredoAusente_failFast() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("review");
        env.setProperty("STORE_REVIEW_ENABLED", "true");
        // Segredo ausente propositalmente
        env.setProperty("GOOGLE_REVIEW_MEMBER_PASSWORD", "Pass1!");
        env.setProperty("GOOGLE_REVIEW_SERVER_PASSWORD", "Pass2!");
        env.setProperty("APPLE_REVIEW_MEMBER_PASSWORD", "Pass3!");
        env.setProperty("APPLE_REVIEW_SERVER_PASSWORD", "Pass4!");

        ValidadorConfiguracaoSegura validador = new ValidadorConfiguracaoSegura(env);
        IllegalStateException ex = assertThrows(IllegalStateException.class, validador::validar);
        assertTrue(ex.getMessage().contains("review.jwt-secret obrigatorio") || ex.getMessage().contains("STORE_REVIEW_JWT_SECRET"));
    }

    @Test
    @DisplayName("23. cada senha de persona review ausente causa fail-fast individual")
    void test23_senhasReviewAusentes_failFastIndividual() {
        List<String> variaveisSenha = List.of(
                "GOOGLE_REVIEW_MEMBER_PASSWORD",
                "GOOGLE_REVIEW_SERVER_PASSWORD",
                "APPLE_REVIEW_MEMBER_PASSWORD",
                "APPLE_REVIEW_SERVER_PASSWORD"
        );

        for (String varAusente : variaveisSenha) {
            MockEnvironment env = new MockEnvironment();
            env.setActiveProfiles("review");
            env.setProperty("STORE_REVIEW_ENABLED", "true");
            env.setProperty("STORE_REVIEW_JWT_SECRET", "secret-key-123");

            for (String v : variaveisSenha) {
                if (!v.equals(varAusente)) {
                    env.setProperty(v, "SenhaValida123!");
                }
            }

            ValidadorConfiguracaoSegura validador = new ValidadorConfiguracaoSegura(env);
            IllegalStateException ex = assertThrows(IllegalStateException.class, validador::validar,
                    "Deveria falhar para variavel ausente: " + varAusente);
            assertTrue(ex.getMessage().contains(varAusente),
                    "Mensagem deveria indicar variavel ausente: " + varAusente);
        }
    }

    @Test
    @DisplayName("24. credenciais de Store Review nao possuem valor default e iniciam nulas/vazias")
    void test24_credenciaisNaoPossuemValorDefault() {
        SyntheticUsersProperties prop = new SyntheticUsersProperties();
        assertNull(prop.getSenha("google-review-membro"));
        assertNull(prop.getSenha("google-review-servidor"));
        assertNull(prop.getSenha("apple-review-membro"));
        assertNull(prop.getSenha("apple-review-servidor"));

        StoreReviewProperties reviewProp = new StoreReviewProperties();
        assertFalse(reviewProp.isEnabled());
        assertNull(reviewProp.getJwtSecret());
        assertNull(reviewProp.getValidUntil());
        assertFalse(reviewProp.isExpirado());
    }

    @Test
    @DisplayName("25. valid-until vazio permite autenticacao e expirado bloqueia")
    void test25_validUntilVazioPermiteAutenticacaoEExpiradoBloqueia() {
        StoreReviewProperties propVazio = new StoreReviewProperties();
        propVazio.setEnabled(true);
        propVazio.setValidUntil("");
        assertFalse(propVazio.isExpirado());

        StoreReviewProperties propExpirado = new StoreReviewProperties();
        propExpirado.setEnabled(true);
        propExpirado.setValidUntil("2020-01-01");
        assertTrue(propExpirado.isExpirado());

        StoreReviewProperties propFuturo = new StoreReviewProperties();
        propFuturo.setEnabled(true);
        propFuturo.setValidUntil("2099-12-31");
        assertFalse(propFuturo.isExpirado());
    }

    @Test
    @DisplayName("26. R03 - Cenário A: SPRING_PROFILES_ACTIVE=prod + MPPB_ENVIRONMENT=homolog + STORE_REVIEW_ENABLED=true = PERMITIDO")
    void test26_r03_cenarioA_prodProfileComHomologEnvironment_permitido() {
        MockEnvironment env = ambienteProducaoValido()
                .withProperty("MPPB_ENVIRONMENT", "homolog")
                .withProperty("STORE_REVIEW_ENABLED", "true")
                .withProperty("STORE_REVIEW_JWT_SECRET", REVIEW_SECRET)
                .withProperty("GOOGLE_REVIEW_MEMBER_PASSWORD", "senha1")
                .withProperty("GOOGLE_REVIEW_SERVER_PASSWORD", "senha2")
                .withProperty("APPLE_REVIEW_MEMBER_PASSWORD", "senha3")
                .withProperty("APPLE_REVIEW_SERVER_PASSWORD", "senha4");

        ValidadorConfiguracaoSegura validador = new ValidadorConfiguracaoSegura(env);
        assertDoesNotThrow(validador::validar);
    }

    @Test
    @DisplayName("27. R03 - Cenário B: SPRING_PROFILES_ACTIVE=prod + MPPB_ENVIRONMENT=prod + STORE_REVIEW_ENABLED=true = STARTUP RECUSADO")
    void test27_r03_cenarioB_prodProfileComProdEnvironment_startupRecusado() {
        MockEnvironment env = ambienteProducaoValido()
                .withProperty("MPPB_ENVIRONMENT", "prod")
                .withProperty("STORE_REVIEW_ENABLED", "true")
                .withProperty("STORE_REVIEW_JWT_SECRET", REVIEW_SECRET)
                .withProperty("GOOGLE_REVIEW_MEMBER_PASSWORD", "senha1")
                .withProperty("GOOGLE_REVIEW_SERVER_PASSWORD", "senha2")
                .withProperty("APPLE_REVIEW_MEMBER_PASSWORD", "senha3")
                .withProperty("APPLE_REVIEW_SERVER_PASSWORD", "senha4");

        ValidadorConfiguracaoSegura validador = new ValidadorConfiguracaoSegura(env);
        IllegalStateException ex = assertThrows(IllegalStateException.class, validador::validar);
        assertTrue(ex.getMessage().contains("Store Review nao pode ser habilitado no ambiente operacional de producao"));
    }

    @Test
    @DisplayName("28. R03 - Cenário C: SPRING_PROFILES_ACTIVE=prod + MPPB_ENVIRONMENT ausente + STORE_REVIEW_ENABLED=true = STARTUP RECUSADO")
    void test28_r03_cenarioC_prodProfileComEnvironmentAusente_startupRecusado() {
        MockEnvironment env = ambienteProducaoValido()
                .withProperty("STORE_REVIEW_ENABLED", "true")
                .withProperty("STORE_REVIEW_JWT_SECRET", REVIEW_SECRET)
                .withProperty("GOOGLE_REVIEW_MEMBER_PASSWORD", "senha1")
                .withProperty("GOOGLE_REVIEW_SERVER_PASSWORD", "senha2")
                .withProperty("APPLE_REVIEW_MEMBER_PASSWORD", "senha3")
                .withProperty("APPLE_REVIEW_SERVER_PASSWORD", "senha4");

        ValidadorConfiguracaoSegura validador = new ValidadorConfiguracaoSegura(env);
        IllegalStateException ex = assertThrows(IllegalStateException.class, validador::validar);
        assertTrue(ex.getMessage().contains("Store Review nao pode ser habilitado em perfis de producao"));
    }

    @Test
    @DisplayName("29. R03 - Cenário D: MPPB_ENVIRONMENT inválido + STORE_REVIEW_ENABLED=true = STARTUP RECUSADO")
    void test29_r03_cenarioD_environmentInvalido_startupRecusado() {
        MockEnvironment envProd = ambienteProducaoValido()
                .withProperty("MPPB_ENVIRONMENT", "ambiente_desconhecido")
                .withProperty("STORE_REVIEW_ENABLED", "true")
                .withProperty("STORE_REVIEW_JWT_SECRET", REVIEW_SECRET)
                .withProperty("GOOGLE_REVIEW_MEMBER_PASSWORD", "senha1")
                .withProperty("GOOGLE_REVIEW_SERVER_PASSWORD", "senha2")
                .withProperty("APPLE_REVIEW_MEMBER_PASSWORD", "senha3")
                .withProperty("APPLE_REVIEW_SERVER_PASSWORD", "senha4");

        ValidadorConfiguracaoSegura validadorProd = new ValidadorConfiguracaoSegura(envProd);
        IllegalStateException exProd = assertThrows(IllegalStateException.class, validadorProd::validar);
        assertTrue(exProd.getMessage().contains("Store Review nao pode ser habilitado em perfis de producao"));

        MockEnvironment envHomolog = new MockEnvironment();
        envHomolog.setActiveProfiles("homolog");
        envHomolog.setProperty("MPPB_ENVIRONMENT", "staging_invalido");
        envHomolog.setProperty("STORE_REVIEW_ENABLED", "true");
        envHomolog.setProperty("STORE_REVIEW_JWT_SECRET", REVIEW_SECRET);
        envHomolog.setProperty("uat.synthetic-users.passwords.google-review-membro", "senha1");
        envHomolog.setProperty("uat.synthetic-users.passwords.google-review-servidor", "senha2");
        envHomolog.setProperty("uat.synthetic-users.passwords.apple-review-membro", "senha3");
        envHomolog.setProperty("uat.synthetic-users.passwords.apple-review-servidor", "senha4");
        envHomolog.setProperty("uat.synthetic-users.jwt-secret", UAT_SECRET);
        envHomolog.setProperty("uat.synthetic-users.passwords.uat-membro", "senhaUat1");
        envHomolog.setProperty("uat.synthetic-users.passwords.uat-servidor", "senhaUat2");

        ValidadorConfiguracaoSegura validadorHomolog = new ValidadorConfiguracaoSegura(envHomolog);
        IllegalStateException exHomolog = assertThrows(IllegalStateException.class, validadorHomolog::validar);
        assertTrue(exHomolog.getMessage().contains("Store Review nao pode ser habilitado em ambiente operacional desconhecido ou invalido"));
    }

    @Test
    @DisplayName("30. R03 - Cenário E: MPPB_ENVIRONMENT=homolog + STORE_REVIEW_ENABLED=false = funcionamento normal preservado")
    void test30_r03_cenarioE_homologEnvironmentSemStoreReview_funcionamentoNormal() {
        MockEnvironment env = ambienteProducaoValido()
                .withProperty("MPPB_ENVIRONMENT", "homolog")
                .withProperty("STORE_REVIEW_ENABLED", "false");

        ValidadorConfiguracaoSegura validador = new ValidadorConfiguracaoSegura(env);
        assertDoesNotThrow(validador::validar);
    }

    @Test
    @DisplayName("31. R03 - Cenários F, G, H, I: 4 identidades Review permitidas, UAT e institucional rejeitados com 401, AuthServiceClient=0")
    void test31_r03_cenariosFGHI_identidadesReviewPermitidasEUatInstitucionalRejeitados() throws Exception {
        MockEnvironment envHomolog = new MockEnvironment();
        envHomolog.setActiveProfiles("prod");
        envHomolog.setProperty("MPPB_ENVIRONMENT", "homolog");
        envHomolog.setProperty("STORE_REVIEW_ENABLED", "true");

        AutenticacaoSinteticaController controller = new AutenticacaoSinteticaController(
                autenticadorUat, autenticadorReview, reviewProperties, envHomolog);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        // F: 4 identidades review = PERMITIDAS
        String[][] identidadesReview = {
                {"membro.google", "SenhaGoogleMembro456!"},
                {"servidor.google", "SenhaGoogleServidor456!"},
                {"membro.apple", "SenhaAppleMembro789!"},
                {"servidor.apple", "SenhaAppleServidor789!"}
        };

        for (String[] id : identidadesReview) {
            AutorizaRequestDto request = new AutorizaRequestDto(id[0], id[1], "appmppb");

            mvc.perform(post("/acesso/autoriza")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty());
        }

        // G: uat.membro e uat.servidor = REJEITADOS
        String[][] uatUsers = {
                {"uat.membro", "SenhaUatMembro123!"},
                {"uat.servidor", "SenhaUatServidor123!"}
        };
        for (String[] uat : uatUsers) {
            AutorizaRequestDto request = new AutorizaRequestDto(uat[0], uat[1], "appmppb");

            mvc.perform(post("/acesso/autoriza")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("security.invalid-credentials"));
        }

        // H: usuário institucional no fluxo Review = REJEITADO
        String[] usuariosInstitucionais = {"joao.silva", "maria.souza@mppb.mp.br", "12345678900"};
        for (String loginInst : usuariosInstitucionais) {
            AutorizaRequestDto request = new AutorizaRequestDto(loginInst, "senhaQualquer123", "appmppb");

            mvc.perform(post("/acesso/autoriza")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("security.invalid-credentials"));
        }

        // I: AuthServiceClient interactions = 0
        assertEquals(0, fakeAuthClient.getChamadasAutoriza(), "AuthServiceClient interactions autoriza deve ser 0");
        assertEquals(0, fakeAuthClient.getChamadasConsulta(), "AuthServiceClient interactions consulta deve ser 0");
    }

    @Test
    @DisplayName("32. R03 - Cenário J: STORE_REVIEW_VALID_UNTIL vazio = permitido (sem expiração automática)")
    void test32_r03_cenarioJ_validUntilVazioPermitido() {
        MockEnvironment env = ambienteProducaoValido()
                .withProperty("MPPB_ENVIRONMENT", "homolog")
                .withProperty("STORE_REVIEW_ENABLED", "true")
                .withProperty("STORE_REVIEW_JWT_SECRET", REVIEW_SECRET)
                .withProperty("STORE_REVIEW_VALID_UNTIL", "")
                .withProperty("GOOGLE_REVIEW_MEMBER_PASSWORD", "senha1")
                .withProperty("GOOGLE_REVIEW_SERVER_PASSWORD", "senha2")
                .withProperty("APPLE_REVIEW_MEMBER_PASSWORD", "senha3")
                .withProperty("APPLE_REVIEW_SERVER_PASSWORD", "senha4");

        ValidadorConfiguracaoSegura validador = new ValidadorConfiguracaoSegura(env);
        assertDoesNotThrow(validador::validar);

        StoreReviewProperties props = new StoreReviewProperties();
        props.setEnabled(true);
        props.setValidUntil("");
        assertFalse(props.isExpirado(), "STORE_REVIEW_VALID_UNTIL vazio nao deve expirar");

        props.setValidUntil(null);
        assertFalse(props.isExpirado(), "STORE_REVIEW_VALID_UNTIL nulo nao deve expirar");
    }

    @Test
    @DisplayName("33. Store Review hibrido em prod aceita Google, Apple e preserva login real")
    void test33_storeReviewHibridoProd_aceitaGoogleAppleEPreservaLoginReal() throws Exception {
        MockEnvironment env = ambienteProducaoValido()
                .withProperty("STORE_REVIEW_HYBRID_ENABLED", "true")
                .withProperty("STORE_REVIEW_JWT_SECRET", REVIEW_SECRET)
                .withProperty("GOOGLE_REVIEW_MEMBER_PASSWORD", "SenhaGoogleMembro456!")
                .withProperty("GOOGLE_REVIEW_SERVER_PASSWORD", "SenhaGoogleServidor456!")
                .withProperty("APPLE_REVIEW_MEMBER_PASSWORD", "SenhaAppleMembro789!")
                .withProperty("APPLE_REVIEW_SERVER_PASSWORD", "SenhaAppleServidor789!");

        ValidadorConfiguracaoSegura validador = new ValidadorConfiguracaoSegura(env);
        assertDoesNotThrow(validador::validar);

        StoreReviewProperties props = new StoreReviewProperties();
        props.setHybridEnabled(true);
        props.setJwtSecret(REVIEW_SECRET);
        props.setTokenTtlSeconds(86400);
        props.setIssuer("mppb-store-review");
        props.setAudience("mppb-mobile-review");

        AutenticadorStoreReview autenticadorReview = new AutenticadorStoreReview(props, syntheticProperties, auditoria);
        FakeAuthServiceClient authClient = new FakeAuthServiceClient();
        authClient.setTokenAutoriza("TOKEN-INSTITUCIONAL-REAL");

        AutenticacaoSinteticaController controller = new AutenticacaoSinteticaController(
                autenticadorUat,
                autenticadorReview,
                props,
                env,
                authClient);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        mvc.perform(post("/acesso/autoriza")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new AutorizaRequestDto("membro.google", "SenhaGoogleMembro456!", "appmppb"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());

        mvc.perform(post("/acesso/autoriza")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new AutorizaRequestDto("servidor.apple", "SenhaAppleServidor789!", "appmppb"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());

        assertEquals(0, authClient.getChamadasAutoriza(), "Review sintetico nao deve chamar Auth API real");

        mvc.perform(post("/acesso/autoriza")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new AutorizaRequestDto("rafael.vasconcelos", "SenhaReal", "appmppb"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("TOKEN-INSTITUCIONAL-REAL"));

        assertEquals(1, authClient.getChamadasAutoriza(), "Login real deve ser repassado para Auth API");
        assertEquals("rafael.vasconcelos", authClient.getUltimoLoginAutoriza());
    }

    @Test
    @DisplayName("34. Store Review hibrido em prod rejeita senha errada sem chamar Auth API")
    void test34_storeReviewHibridoProd_rejeitaSenhaSinteticaErrada() throws Exception {
        MockEnvironment env = ambienteProducaoValido()
                .withProperty("STORE_REVIEW_HYBRID_ENABLED", "true")
                .withProperty("STORE_REVIEW_JWT_SECRET", REVIEW_SECRET)
                .withProperty("GOOGLE_REVIEW_MEMBER_PASSWORD", "SenhaGoogleMembro456!")
                .withProperty("GOOGLE_REVIEW_SERVER_PASSWORD", "SenhaGoogleServidor456!")
                .withProperty("APPLE_REVIEW_MEMBER_PASSWORD", "SenhaAppleMembro789!")
                .withProperty("APPLE_REVIEW_SERVER_PASSWORD", "SenhaAppleServidor789!");

        StoreReviewProperties props = new StoreReviewProperties();
        props.setHybridEnabled(true);
        props.setJwtSecret(REVIEW_SECRET);
        props.setTokenTtlSeconds(86400);
        props.setIssuer("mppb-store-review");
        props.setAudience("mppb-mobile-review");

        AutenticadorStoreReview autenticadorReview = new AutenticadorStoreReview(props, syntheticProperties, auditoria);
        FakeAuthServiceClient authClient = new FakeAuthServiceClient();

        AutenticacaoSinteticaController controller = new AutenticacaoSinteticaController(
                autenticadorUat,
                autenticadorReview,
                props,
                env,
                authClient);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        mvc.perform(post("/acesso/autoriza")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new AutorizaRequestDto("membro.google", "SenhaIncorreta!", "appmppb"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("security.invalid-credentials"));

        assertEquals(0, authClient.getChamadasAutoriza(), "Senha sintetica errada nao deve ser encaminhada para Auth API");
    }

    private MockEnvironment ambienteProducaoValido() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        return environment
                .withProperty("api.auth.url", "https://auth-api.mppb.mp.br")
                .withProperty("spring.datasource.url", "jdbc:postgresql://db-producao:5432/mobile")
                .withProperty("spring.datasource.username", "mobile_app")
                .withProperty("spring.datasource.password", "senha-forte")
                .withProperty("spring.jpa.show-sql", "false")
                .withProperty("spring.jpa.properties.hibernate.format_sql", "false")
                .withProperty("mpvirtual.recursos-humanos.base-url", "http://mpvirtual-interno/api")
                .withProperty("jcr.user", "mobile_jcr")
                .withProperty("jcr.password", "senha-jcr")
                .withProperty("cors.enabled", "false")
                .withProperty("cors.access-control-headers.allow-credentials", "false")
                .withProperty("cors.access-control-headers.allow-origin", "*");
    }

    private static class FakeAuthServiceClient implements AuthServiceClient {
        private int chamadasAutoriza = 0;
        private int chamadasConsulta = 0;
        private String tokenAutoriza;
        private String ultimoLoginAutoriza;

        @Override
        public AutorizaResponseDto autoriza(AutorizaRequestDto request) {
            chamadasAutoriza++;
            ultimoLoginAutoriza = request != null ? request.getLogin() : null;
            AutorizaResponseDto response = new AutorizaResponseDto();
            response.setToken(tokenAutoriza);
            return response;
        }

        @Override
        public ConsultaAutorizacaoResponseDto consultaAutorizacao(ConsultaAutorizacaoRequestDto request) {
            chamadasConsulta++;
            return new ConsultaAutorizacaoResponseDto();
        }

        public int getChamadasAutoriza() {
            return chamadasAutoriza;
        }

        public int getChamadasConsulta() {
            return chamadasConsulta;
        }

        public void setTokenAutoriza(String tokenAutoriza) {
            this.tokenAutoriza = tokenAutoriza;
        }

        public String getUltimoLoginAutoriza() {
            return ultimoLoginAutoriza;
        }
    }
}
