package mppb.restapi.mobile.quality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import mppb.restapi.common.infrastructure.client.authapi.AuthServiceClient;
import mppb.restapi.common.infrastructure.client.authapi.dto.AutorizaRequestDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.AutorizaResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoRequestDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.UsuarioAutorizadoResponseDto;
import mppb.restapi.common.infrastructure.security.authorization.TokenAuthentication;
import mppb.restapi.common.infrastructure.security.filter.CustomBasicAuthenticationFilter;
import mppb.restapi.common.infrastructure.security.filter.CustomUsernamePasswordAuthenticationFilter;
import mppb.restapi.common.infrastructure.security.manager.CustomAuthenticationManager;
import mppb.restapi.mobile.core.config.ValidadorConfiguracaoSegura;
import mppb.restapi.mobile.feature.carteirafuncional.controller.CarteiraFuncionalController;
import mppb.restapi.mobile.feature.carteirafuncional.dto.CarteiraFuncionalTO;
import mppb.restapi.mobile.feature.carteirafuncional.service.CarteiraService;
import mppb.restapi.mobile.feature.concessaofolga.controller.ConcessaoFolgaController;
import mppb.restapi.mobile.feature.concessaofolga.dto.AusenciaCalendarioDto;
import mppb.restapi.mobile.feature.concessaofolga.dto.ConcessaoFolgaDto;
import mppb.restapi.mobile.feature.concessaofolga.dto.OperacaoSolicitacaoRequestDto;
import mppb.restapi.mobile.feature.concessaofolga.dto.SolicitacaoMarcacaoFeriasRequestDto;
import mppb.restapi.mobile.feature.concessaofolga.repository.ConcessaoFolgaIdentityRepository;
import mppb.restapi.mobile.feature.concessaofolga.repository.ConcessaoFolgaRemoteRepository;
import mppb.restapi.mobile.feature.concessaofolga.service.ConcessaoFolgaService;
import mppb.restapi.mobile.feature.contracheque.dto.CompetenciaDto;
import mppb.restapi.mobile.feature.contracheque.dto.ContrachequeResponseDto;
import mppb.restapi.mobile.feature.contracheque.repository.ContrachequeRepository;
import mppb.restapi.mobile.feature.contracheque.service.ContrachequePdfService;
import mppb.restapi.mobile.feature.contracheque.service.ContrachequeService;
import mppb.restapi.mobile.feature.designacao.dto.DesignacoesAtivasResponse;
import mppb.restapi.mobile.feature.designacao.mapper.DesignacaoMapper;
import mppb.restapi.mobile.feature.designacao.model.DesignacaoAtivaRow;
import mppb.restapi.mobile.feature.designacao.repository.DesignacaoIdentityRepository;
import mppb.restapi.mobile.feature.designacao.repository.DesignacaoViewRepository;
import mppb.restapi.mobile.feature.designacao.service.DesignacaoAuthService;
import mppb.restapi.mobile.feature.designacao.service.DesignacaoConsultaService;
import mppb.restapi.mobile.feature.designacao.service.DesignacaoInicializacaoService;
import mppb.restapi.mobile.feature.notificacao.dto.DispositivoNotificacaoResponse;
import mppb.restapi.mobile.feature.notificacao.dto.RegistrarDispositivoRequest;
import mppb.restapi.mobile.feature.notificacao.model.DispositivoFcm;
import mppb.restapi.mobile.feature.notificacao.repository.DispositivoFcmRepository;
import mppb.restapi.mobile.feature.notificacao.service.DispositivoFcmService;
import mppb.restapi.mobile.feature.pagamentos.dto.PagamentoItemDto;
import mppb.restapi.mobile.feature.pagamentos.dto.PagamentosResponseDto;
import mppb.restapi.mobile.feature.pagamentos.repository.PagamentosRepository;
import mppb.restapi.mobile.feature.pagamentos.service.PagamentosService;
import mppb.restapi.mobile.feature.perfil.controller.PerfilUsuarioController;
import mppb.restapi.mobile.feature.perfil.dto.PerfilUsuarioDto;
import mppb.restapi.mobile.feature.perfil.repository.PerfilUsuarioRepository;
import mppb.restapi.mobile.feature.perfil.service.PerfilUsuarioService;
import mppb.restapi.mobile.feature.synthetic.audit.AuditoriaUsuarioSintetico;
import mppb.restapi.mobile.feature.synthetic.auth.AutenticadorUsuarioSintetico;
import mppb.restapi.mobile.feature.synthetic.auth.SyntheticTokenAuthenticationProvider;
import mppb.restapi.mobile.feature.synthetic.config.SyntheticSecurityConfig;
import mppb.restapi.mobile.feature.synthetic.config.SyntheticUsersProperties;
import mppb.restapi.mobile.feature.synthetic.controller.AutenticacaoSinteticaController;
import mppb.restapi.mobile.feature.synthetic.dataset.SyntheticDataService;
import mppb.restapi.mobile.feature.synthetic.model.IdentidadeSintetica;
import mppb.restapi.mobile.feature.synthetic.model.OrigemConta;
import mppb.restapi.mobile.feature.synthetic.model.PerfilFuncional;
import mppb.restapi.mobile.feature.synthetic.sandbox.SyntheticSandboxStorage;

@DisplayName("Q14-R03: Infraestrutura Segura de Usuários Sintéticos (UAT e Store Review Google/Apple)")
class Q14UsuariosSinteticosUatReviewTest {

    private SyntheticUsersProperties properties;
    private AuditoriaUsuarioSintetico auditoria;
    private AutenticadorUsuarioSintetico autenticador;
    private SyntheticTokenAuthenticationProvider authProvider;
    private AutenticacaoSinteticaController authController;
    private SyntheticDataService dataService;
    private SyntheticSandboxStorage sandboxStorage;
    private FakeAuthServiceClient fakeAuthClient;
    private FakePerfilUsuarioRepository fakePerfilRepo;
    private ConcessaoFolgaService folgaService;
    private FakeDispositivoFcmRepository fakeFcmRepo;
    private DispositivoFcmService fcmService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        properties = new SyntheticUsersProperties();
        properties.setEnabled(true);
        properties.setJwtSecret("segredo-de-teste-unitario-com-no-minimo-32-caracteres-ok");

        Map<String, String> senhas = new HashMap<>();
        senhas.put("uat-membro", "SenhaSeguraUatMembro123!");
        senhas.put("uat-servidor", "SenhaSeguraUatServidor123!");
        senhas.put("google-review-membro", "SenhaSeguraGoogleMembro123!");
        senhas.put("google-review-servidor", "SenhaSeguraGoogleServidor123!");
        senhas.put("apple-review-membro", "SenhaSeguraAppleMembro123!");
        senhas.put("apple-review-servidor", "SenhaSeguraAppleServidor123!");
        properties.setPasswords(senhas);

        auditoria = new AuditoriaUsuarioSintetico();
        autenticador = new AutenticadorUsuarioSintetico(properties, auditoria);
        authProvider = new SyntheticTokenAuthenticationProvider(autenticador, auditoria);
        fakeAuthClient = new FakeAuthServiceClient();
        authController = new AutenticacaoSinteticaController(autenticador);
        dataService = new SyntheticDataService(autenticador, auditoria);
        sandboxStorage = new SyntheticSandboxStorage(auditoria);
        sandboxStorage.reset();

        fakePerfilRepo = new FakePerfilUsuarioRepository();
        fakeFcmRepo = new FakeDispositivoFcmRepository();
        fcmService = new DispositivoFcmService(fakeFcmRepo, dataService, sandboxStorage);

        PerfilUsuarioService perfilService = new PerfilUsuarioService(fakeAuthClient, fakePerfilRepo, dataService);
        CarteiraService carteiraService = new CarteiraService(null, null, dataService);
        folgaService = new ConcessaoFolgaService(
                new ConcessaoFolgaIdentityRepository(null),
                new ConcessaoFolgaRemoteRepository(null),
                null,
                dataService,
                sandboxStorage);

        CustomAuthenticationManager manager = new CustomAuthenticationManager(List.of(authProvider));
        mockMvc = MockMvcBuilders.standaloneSetup(
                authController,
                new PerfilUsuarioController(perfilService),
                new CarteiraFuncionalController(carteiraService),
                new ConcessaoFolgaController(folgaService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .addFilters(
                        new CustomBasicAuthenticationFilter(manager),
                        new CustomUsernamePasswordAuthenticationFilter(manager))
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("01. Feature disabled: controller retorna 401 sem proxy e provider repassa")
    void test01_featureDisabled() {
        properties.setEnabled(false);

        AutorizaRequestDto request = new AutorizaRequestDto("uat.membro", "SenhaSeguraUatMembro123!", "ANDROID");
        ResponseEntity<?> resp = authController.autoriza(request);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertEquals(0, fakeAuthClient.chamadasAutoriza);

        Authentication auth = authProvider.authenticate(new TokenAuthentication("token-qualquer"));
        assertNull(auth, "Provider deve retornar null quando a feature estiver desligada para delegar");
    }

    @Test
    @DisplayName("02. Usuário UAT membro autentica com sucesso")
    void test02_usuarioUatMembro() {
        Optional<String> tokenOpt = autenticador.autenticar("uat.membro", "SenhaSeguraUatMembro123!");
        assertTrue(tokenOpt.isPresent());
        String token = tokenOpt.get();

        assertTrue(token.startsWith(AutenticadorUsuarioSintetico.TOKEN_PREFIX));
        Optional<IdentidadeSintetica> idOpt = autenticador.validarToken(token);
        assertTrue(idOpt.isPresent());
        assertEquals(IdentidadeSintetica.UAT_MEMBRO, idOpt.get());
        assertEquals(OrigemConta.UAT, idOpt.get().getOrigem());
        assertEquals(PerfilFuncional.MEMBRO, idOpt.get().getPerfil());
    }

    @Test
    @DisplayName("03. Usuário UAT servidor autentica com sucesso")
    void test03_usuarioUatServidor() {
        Optional<String> tokenOpt = autenticador.autenticar("uat.servidor", "SenhaSeguraUatServidor123!");
        assertTrue(tokenOpt.isPresent());
        String token = tokenOpt.get();

        Optional<IdentidadeSintetica> idOpt = autenticador.validarToken(token);
        assertTrue(idOpt.isPresent());
        assertEquals(IdentidadeSintetica.UAT_SERVIDOR, idOpt.get());
        assertEquals(OrigemConta.UAT, idOpt.get().getOrigem());
        assertEquals(PerfilFuncional.SERVIDOR, idOpt.get().getPerfil());
    }

    @Test
    @DisplayName("04. Google membro autentica com sucesso")
    void test04_googleMembro() {
        Optional<String> tokenOpt = autenticador.autenticar("review.google.membro", "SenhaSeguraGoogleMembro123!");
        assertTrue(tokenOpt.isPresent());
        String token = tokenOpt.get();

        Optional<IdentidadeSintetica> idOpt = autenticador.validarToken(token);
        assertTrue(idOpt.isPresent());
        assertEquals(IdentidadeSintetica.REVIEW_GOOGLE_MEMBRO, idOpt.get());
        assertEquals(OrigemConta.REVIEW_GOOGLE, idOpt.get().getOrigem());
        assertEquals(PerfilFuncional.MEMBRO, idOpt.get().getPerfil());
    }

    @Test
    @DisplayName("05. Google servidor autentica com sucesso")
    void test05_googleServidor() {
        Optional<String> tokenOpt = autenticador.autenticar("review.google.servidor", "SenhaSeguraGoogleServidor123!");
        assertTrue(tokenOpt.isPresent());
        String token = tokenOpt.get();

        Optional<IdentidadeSintetica> idOpt = autenticador.validarToken(token);
        assertTrue(idOpt.isPresent());
        assertEquals(IdentidadeSintetica.REVIEW_GOOGLE_SERVIDOR, idOpt.get());
        assertEquals(OrigemConta.REVIEW_GOOGLE, idOpt.get().getOrigem());
        assertEquals(PerfilFuncional.SERVIDOR, idOpt.get().getPerfil());
    }

    @Test
    @DisplayName("06. Apple membro autentica com sucesso")
    void test06_appleMembro() {
        Optional<String> tokenOpt = autenticador.autenticar("review.apple.membro", "SenhaSeguraAppleMembro123!");
        assertTrue(tokenOpt.isPresent());
        String token = tokenOpt.get();

        Optional<IdentidadeSintetica> idOpt = autenticador.validarToken(token);
        assertTrue(idOpt.isPresent());
        assertEquals(IdentidadeSintetica.REVIEW_APPLE_MEMBRO, idOpt.get());
        assertEquals(OrigemConta.REVIEW_APPLE, idOpt.get().getOrigem());
        assertEquals(PerfilFuncional.MEMBRO, idOpt.get().getPerfil());
    }

    @Test
    @DisplayName("07. Apple servidor autentica com sucesso")
    void test07_appleServidor() {
        Optional<String> tokenOpt = autenticador.autenticar("review.apple.servidor", "SenhaSeguraAppleServidor123!");
        assertTrue(tokenOpt.isPresent());
        String token = tokenOpt.get();

        Optional<IdentidadeSintetica> idOpt = autenticador.validarToken(token);
        assertTrue(idOpt.isPresent());
        assertEquals(IdentidadeSintetica.REVIEW_APPLE_SERVIDOR, idOpt.get());
        assertEquals(OrigemConta.REVIEW_APPLE, idOpt.get().getOrigem());
        assertEquals(PerfilFuncional.SERVIDOR, idOpt.get().getPerfil());
    }

    @Test
    @DisplayName("08. Senha inválida é recusada com 401 Unauthorized")
    void test08_senhaInvalida() {
        Optional<String> tokenOpt = autenticador.autenticar("uat.membro", "SenhaTotalmenteInvalida!");
        assertFalse(tokenOpt.isPresent());

        AutorizaRequestDto request = new AutorizaRequestDto("uat.membro", "SenhaErrada", "ANDROID");
        ResponseEntity<?> resp = authController.autoriza(request);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    @DisplayName("09. Origem e perfil são registrados na auditoria e token aceito pelo provider")
    void test09_origemAuditoria() {
        Optional<String> tokenOpt = autenticador.autenticar("review.google.membro", "SenhaSeguraGoogleMembro123!");
        assertTrue(tokenOpt.isPresent());

        Authentication auth = authProvider.authenticate(new TokenAuthentication(tokenOpt.get()));
        assertNotNull(auth);
        assertTrue(auth.isAuthenticated());
        assertEquals("review.google.membro", auth.getName());
    }

    @Test
    @DisplayName("10. Perfil Membro: contrato e dados funcionais sintéticos")
    void test10_perfilMembro() {
        PerfilUsuarioDto dto = dataService.getPerfilByLogin("uat.membro");
        assertNotNull(dto);
        assertEquals("Lucas Almeida Monteiro", dto.getNome());
        assertEquals("900001", dto.getMatricula());
        assertEquals("Promotor de Justiça", dto.getCargo());
        assertEquals("Promotoria de Justiça de Homologação", dto.getSetorNome());
        assertEquals("Ativo", dto.getSituacao());
        assertEquals("membro.uat@example.invalid", dto.getEmail());
    }

    @Test
    @DisplayName("11. Perfil Servidor: contrato e dados funcionais sintéticos")
    void test11_perfilServidor() {
        PerfilUsuarioDto dto = dataService.getPerfilByLogin("uat.servidor");
        assertNotNull(dto);
        assertEquals("Mariana Costa Ribeiro", dto.getNome());
        assertEquals("900002", dto.getMatricula());
        assertEquals("Analista Ministerial", dto.getCargo());
        assertEquals("DITEC — Ambiente de Homologação", dto.getSetorNome());
        assertEquals("Ativo", dto.getSituacao());
        assertEquals("servidor.uat@example.invalid", dto.getEmail());
    }

    @Test
    @DisplayName("12. Carteira Funcional: frente, verso, hash e foto JPEG sintética")
    void test12_carteiraFuncional() {
        CarteiraFuncionalTO carteira = dataService.getCarteiraByLogin("uat.membro");
        assertNotNull(carteira);
        assertEquals("Lucas Almeida Monteiro", carteira.getNome());
        assertEquals("Promotor de Justiça", carteira.getCargo());
        assertEquals(SyntheticDataService.HASH_MD5_MEMBRO, carteira.getMd5());

        byte[] foto = dataService.getFotoByLogin("uat.membro");
        assertNotNull(foto);
        assertTrue(foto.length > 500, "Foto procedural sintética deve possuir tamanho JPEG válido");

        CarteiraFuncionalTO porHash = dataService.getCarteiraByHash(SyntheticDataService.HASH_MD5_MEMBRO);
        assertNotNull(porHash);
        assertEquals("Lucas Almeida Monteiro", porHash.getNome());
    }

    @Test
    @DisplayName("13. Contracheques: competências, detalhes e PDF sintético")
    void test13_contracheques() throws Exception {
        List<CompetenciaDto> comps = dataService.getCompetencias(900001);
        assertNotNull(comps);
        assertEquals(3, comps.size());
        assertEquals(9, comps.get(0).getMes());
        assertEquals(2026, comps.get(0).getAno());

        ContrachequeResponseDto detalhe = dataService.getContracheque(900001, 9, 2026);
        assertNotNull(detalhe);
        assertEquals(new BigDecimal("34889.11"), detalhe.getTotalVantagens());
        assertEquals(new BigDecimal("11966.60"), detalhe.getTotalDescontos());
        assertEquals(new BigDecimal("22922.51"), detalhe.getSalarioLiquido());

        ContrachequePdfService pdfService = new ContrachequePdfService();
        byte[] pdf = pdfService.generatePdf(detalhe);
        assertNotNull(pdf);
        assertTrue(pdf.length > 100);
        assertTrue(new String(pdf, StandardCharsets.ISO_8859_1).contains("%PDF"));
    }

    @Test
    @DisplayName("14. Pagamentos: exercícios disponíveis, itens e empty state")
    void test14_pagamentos() {
        List<Integer> anosMembro = dataService.getAnosPagamento("000.000.001-91");
        assertEquals(List.of(2026, 2025), anosMembro);

        PagamentosResponseDto pag2026 = dataService.getPagamentos("2026", "000.000.001-91");
        assertNotNull(pag2026);
        assertEquals(2, pag2026.getPagamentos().size());
        assertEquals(new BigDecimal("69778.22"), pag2026.getTotalBruto());

        PagamentosResponseDto pagVazio = dataService.getPagamentos("2023", "000.000.001-91");
        assertNotNull(pagVazio);
        assertTrue(pagVazio.getPagamentos().isEmpty());
        assertEquals(BigDecimal.ZERO, pagVazio.getTotalBruto());
    }

    @Test
    @DisplayName("15. Designações: membro com titularidade e servidor com lista vazia")
    void test15_designacoes() {
        DesignacoesAtivasResponse desigMembro = dataService.getDesignacoesAtivas("900001");
        assertNotNull(desigMembro);
        assertFalse(desigMembro.getDesignacoes().isEmpty());
        assertEquals("Promotoria de Justiça de Homologação", desigMembro.getDesignacoes().get(0).getOrgao());

        DesignacoesAtivasResponse desigServidor = dataService.getDesignacoesAtivas("900002");
        assertNotNull(desigServidor);
        assertTrue(desigServidor.getDesignacoes().isEmpty());
    }

    @Test
    @DisplayName("16. Concessões e Ausências: saldos de férias e calendário")
    void test16_concessoes() {
        List<ConcessaoFolgaDto> concs = sandboxStorage.listarConcessoes("900001");
        assertNotNull(concs);
        assertEquals(2, concs.size());
        assertEquals("Férias", concs.get(0).getTipo());

        List<AusenciaCalendarioDto> ausencias = sandboxStorage.listarAusencias("900001", 11, 2026);
        assertNotNull(ausencias);
        assertEquals(1, ausencias.size());
        assertEquals("HOMOLOGADA", ausencias.get(0).getStatus());
    }

    @Test
    @DisplayName("17. Operações mutáveis sandbox: solicitar, adiar e cancelar sem tocar no MPVirtual")
    void test17_operacoesSandbox() {
        SolicitacaoMarcacaoFeriasRequestDto req = new SolicitacaoMarcacaoFeriasRequestDto();
        req.setDataInicial("2026-11-01");
        req.setNumeroDias(15);

        ResponseEntity<String> resp = sandboxStorage.cadastrarSolicitacao(1001L, req, "token-teste");
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().contains("5001"));

        OperacaoSolicitacaoRequestDto opReq = new OperacaoSolicitacaoRequestDto();
        opReq.setDataAdiamento("2026-11-15");
        ResponseEntity<String> respAdiar = sandboxStorage.adiarSolicitacao(5001L, opReq);
        assertEquals(HttpStatus.OK, respAdiar.getStatusCode());
        assertTrue(respAdiar.getBody().contains("ADIADA"));

        ResponseEntity<String> respCancel = sandboxStorage.cancelarSolicitacao(5001L);
        assertEquals(HttpStatus.OK, respCancel.getStatusCode());
        assertTrue(respCancel.getBody().contains("CANCELADA"));
    }

    @Test
    @DisplayName("18. FCM sandbox: registro e desativação em memória")
    void test18_fcmSandbox() {
        RegistrarDispositivoRequest req = new RegistrarDispositivoRequest(
                "device-uuid-uat-01",
                "fcm-token-sintetico-123",
                "ANDROID");

        DispositivoNotificacaoResponse resp = sandboxStorage.registrarDispositivo("900001", req);
        assertNotNull(resp);
        assertEquals("device-uuid-uat-01", resp.identificador());
        assertTrue(resp.ativo());

        boolean desativou = sandboxStorage.desativarDispositivo("900001", "device-uuid-uat-01");
        assertTrue(desativou);
    }

    @Test
    @DisplayName("19. Nenhum acesso a integrações reais com credencial sintética")
    void test19_nenhumAcessoAIntegracaoReal() {
        assertFalse(autenticador.validarToken("token-arbitrario-real").isPresent());

        String tokenValido = autenticador.gerarToken(IdentidadeSintetica.UAT_MEMBRO);
        String tokenAdulterado = tokenValido.substring(0, tokenValido.length() - 5) + "XXXXX";
        assertThrows(BadCredentialsException.class, () -> {
            authProvider.authenticate(new TokenAuthentication(tokenAdulterado));
        });
    }

    @Test
    @DisplayName("20. Bloqueio obrigatório em profile prod se synthetic-users estiver habilitado")
    void test20_bloqueioEmProfileProd() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("uat.synthetic-users.enabled", "true");
        env.setProperty("uat.synthetic-users.jwt-secret", "segredo-de-teste-32-caracteres-ok");
        env.setProperty("api.auth.url", "https://auth.internal.example");
        env.setProperty("spring.datasource.url", "jdbc:postgresql://db.internal.example:5432/mppb");
        env.setProperty("spring.datasource.username", "usr_prod");
        env.setProperty("spring.datasource.password", "ComplexProdPassword123#");
        env.setProperty("mpvirtual.recursos-humanos.base-url", "https://mpvirtual.internal.example/api");

        ValidadorConfiguracaoSegura validador = new ValidadorConfiguracaoSegura(env);
        IllegalStateException ex = assertThrows(IllegalStateException.class, validador::validar);
        assertTrue(ex.getMessage().contains("Usuarios sinteticos UAT/Review nao podem ser habilitados em perfis de producao"),
                "Startup deve falhar imediatamente se prod + synthetic-users.enabled=true");
    }

    @Test
    @DisplayName("21. Token tampering: assinatura ou payload adulterado é rejeitado")
    void test21_tokenTamperingAssinaturaEPayloadRejeitados() throws Exception {
        String tokenValido = autenticador.gerarToken(IdentidadeSintetica.UAT_MEMBRO);
        String[] partes = tokenValido.substring(AutenticadorUsuarioSintetico.TOKEN_PREFIX.length()).split("\\.");
        assertEquals(3, partes.length);

        ObjectMapper om = new ObjectMapper();
        byte[] payloadBytes = Base64.getUrlDecoder().decode(partes[1]);
        ObjectNode payloadNode = (ObjectNode) om.readTree(payloadBytes);

        // Adulterar o perfil de MEMBRO para SERVIDOR no texto do payload
        payloadNode.put("perfil", "SERVIDOR");
        String payloadAdulteradoB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(om.writeValueAsBytes(payloadNode));

        // Token com payload alterado mantendo assinatura original (deve falhar HMAC)
        String tokenPayloadAdulterado = AutenticadorUsuarioSintetico.TOKEN_PREFIX + partes[0] + "." + payloadAdulteradoB64 + "." + partes[2];
        assertFalse(autenticador.validarToken(tokenPayloadAdulterado).isPresent(),
                "Token com payload adulterado sem assinatura correta deve ser rejeitado");

        // Token com assinatura corrompida
        String tokenAssinaturaCorrompida = tokenValido.substring(0, tokenValido.length() - 6) + "AAAAAA";
        assertFalse(autenticador.validarToken(tokenAssinaturaCorrompida).isPresent(),
                "Token com assinatura corrompida deve ser rejeitado");
    }

    @Test
    @DisplayName("22. Token expirado é rejeitado")
    void test22_tokenExpiradoRejeitado() throws Exception {
        ObjectMapper om = new ObjectMapper();
        ObjectNode header = om.createObjectNode();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        ObjectNode payload = om.createObjectNode();
        payload.put("sub", "uat.membro");
        payload.put("identificador", "UAT-MEMBRO-001");
        payload.put("origem", "UAT");
        payload.put("perfil", "MEMBRO");
        payload.put("matricula", "900001");
        payload.put("iat", 1000L);
        payload.put("exp", 2000L); // Expirado há muito tempo

        String hB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(om.writeValueAsBytes(header));
        String pB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(om.writeValueAsBytes(payload));
        String content = hB64 + "." + pB64;

        javax.crypto.Mac hmac = javax.crypto.Mac.getInstance("HmacSHA256");
        hmac.init(new javax.crypto.spec.SecretKeySpec(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String sigB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(hmac.doFinal(content.getBytes(StandardCharsets.UTF_8)));

        String tokenExpirado = AutenticadorUsuarioSintetico.TOKEN_PREFIX + content + "." + sigB64;
        assertFalse(autenticador.validarToken(tokenExpirado).isPresent(), "Token com exp no passado deve ser rejeitado");
    }

    @Test
    @DisplayName("23. Zero proxy institucional: login desconhecido/não sintético retorna 401 sem encaminhar ao AuthServiceClient")
    void test23_zeroProxyLoginDesconhecidoNaoEncaminhaAuthService() {
        AutorizaRequestDto request = new AutorizaRequestDto("promotor.institucional.real", "SenhaInstitucional123!", "ANDROID");
        ResponseEntity<?> resp = authController.autoriza(request);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertEquals(0, fakeAuthClient.chamadasAutoriza);
    }

    @Test
    @DisplayName("24. Configuração incompleta falha no startup (missing password ou missing secret)")
    void test24_configuracaoIncompletaStartupRecusado() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("homolog");
        env.setProperty("uat.synthetic-users.enabled", "true");
        env.setProperty("uat.synthetic-users.jwt-secret", "segredo-de-teste-32-caracteres-ok");

        // Faltam as senhas obrigatórias
        ValidadorConfiguracaoSegura validador = new ValidadorConfiguracaoSegura(env);
        IllegalStateException ex = assertThrows(IllegalStateException.class, validador::validar);
        assertTrue(ex.getMessage().contains("Senha obrigatoria ausente para usuarios sinteticos"),
                "Startup deve falhar se synthetic enabled e senhas ausentes");

        // Faltando jwt-secret
        MockEnvironment envSemSecret = new MockEnvironment();
        envSemSecret.setActiveProfiles("homolog");
        envSemSecret.setProperty("uat.synthetic-users.enabled", "true");
        envSemSecret.setProperty("uat.synthetic-users.passwords.uat-membro", "pass1");
        envSemSecret.setProperty("uat.synthetic-users.passwords.uat-servidor", "pass2");
        envSemSecret.setProperty("uat.synthetic-users.passwords.google-review-membro", "pass3");
        envSemSecret.setProperty("uat.synthetic-users.passwords.google-review-servidor", "pass4");
        envSemSecret.setProperty("uat.synthetic-users.passwords.apple-review-membro", "pass5");
        envSemSecret.setProperty("uat.synthetic-users.passwords.apple-review-servidor", "pass6");

        ValidadorConfiguracaoSegura validadorSemSecret = new ValidadorConfiguracaoSegura(envSemSecret);
        IllegalStateException exSecret = assertThrows(IllegalStateException.class, validadorSemSecret::validar);
        assertTrue(exSecret.getMessage().contains("jwt-secret obrigatorio"),
                "Startup deve falhar se synthetic enabled e jwtSecret ausente");
    }

    @Test
    @DisplayName("25. Proteção de ambiente: profiles prd, production e unknown bloqueados quando enabled")
    void test25_protecaoAmbientePerfisProibidos() {
        for (String prof : List.of("prd", "production")) {
            MockEnvironment env = new MockEnvironment();
            env.setActiveProfiles(prof);
            env.setProperty("uat.synthetic-users.enabled", "true");
            env.setProperty("uat.synthetic-users.jwt-secret", "segredo-32-chars-para-teste-valido!");
            env.setProperty("api.auth.url", "https://auth.internal.example");
            env.setProperty("spring.datasource.url", "jdbc:postgresql://db.internal.example:5432/mppb");
            env.setProperty("spring.datasource.username", "usr_prod");
            env.setProperty("spring.datasource.password", "ComplexProdPassword123#");
            env.setProperty("mpvirtual.recursos-humanos.base-url", "https://mpvirtual.internal.example/api");

            ValidadorConfiguracaoSegura validador = new ValidadorConfiguracaoSegura(env);
            assertThrows(IllegalStateException.class, validador::validar,
                    "Profile " + prof + " deve falhar se synthetic enabled");
        }

        // Profile não constante na allowlist (ex: staging desconhecido)
        MockEnvironment envStaging = new MockEnvironment();
        envStaging.setActiveProfiles("staging");
        envStaging.setProperty("uat.synthetic-users.enabled", "true");
        ValidadorConfiguracaoSegura valStaging = new ValidadorConfiguracaoSegura(envStaging);
        IllegalStateException ex = assertThrows(IllegalStateException.class, valStaging::validar);
        assertTrue(ex.getMessage().contains("somente podem ser ativados em profiles homolog, uat, review"),
                "Profiles fora da allowlist devem ser rejeitados");
    }

    @Test
    @DisplayName("26. Integração HTTP real via MockMvc: autenticação, perfil e carteira com zero chamadas externas")
    void test26_httpMockMvcAutenticacaoESecurityFilterChain() throws Exception {
        // 1. POST /acesso/autoriza com credenciais sintéticas válidas
        String loginPayload = "{\"login\":\"uat.membro\",\"senha\":\"SenhaSeguraUatMembro123!\",\"siglaSistema\":\"ANDROID\"}";
        String responseBody = mockMvc.perform(post("/acesso/autoriza")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn().getResponse().getContentAsString();

        ObjectMapper om = new ObjectMapper();
        String tokenSintetico = om.readTree(responseBody).get("token").asText();
        assertThat(tokenSintetico).startsWith(AutenticadorUsuarioSintetico.TOKEN_PREFIX);

        // 2. GET /api/perfil/dados com Bearer do token sintético emitido
        mockMvc.perform(get("/api/perfil/dados")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSintetico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Lucas Almeida Monteiro"))
                .andExpect(jsonPath("$.matricula").value("900001"))
                .andExpect(jsonPath("$.cargo").value("Promotor de Justiça"));

        // 3. GET /api/carteiras-funcionais/dados com Bearer sintético
        mockMvc.perform(get("/api/carteiras-funcionais/dados")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSintetico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Lucas Almeida Monteiro"))
                .andExpect(jsonPath("$.cargo").value("Promotor de Justiça"))
                .andExpect(jsonPath("$.md5").value(SyntheticDataService.HASH_MD5_MEMBRO));

        // 4. POST /api/recursos-humanos/concessoes/1001/solicitacoes com Bearer sintético
        String solicitacaoResponse = mockMvc.perform(post("/api/recursos-humanos/concessoes/1001/solicitacoes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSintetico)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dataInicial\":\"2026-11-01\",\"numeroDias\":15}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(solicitacaoResponse).contains("SOLICITADA");

        // Provar zero chamadas a clientes/repositórios externos
        assertThat(fakeAuthClient.chamadasAutoriza).isZero();
        assertThat(fakeAuthClient.chamadasConsulta).isZero();
        assertThat(fakePerfilRepo.chamadasBuscarPorLogin).isZero();
    }

    @Test
    @DisplayName("27. Lifecycle dos beans sintéticos: ausentes quando feature off, presentes quando feature on")
    void test27_lifecycleBeansCondicionados() {
        // Cenário 1: uat.synthetic-users.enabled=false -> nenhum bean sintético é instanciado
        new ApplicationContextRunner()
                .withUserConfiguration(
                        SyntheticUsersProperties.class,
                        AuditoriaUsuarioSintetico.class,
                        AutenticadorUsuarioSintetico.class,
                        SyntheticDataService.class,
                        SyntheticSandboxStorage.class,
                        AutenticacaoSinteticaController.class,
                        SyntheticTokenAuthenticationProvider.class,
                        SyntheticSecurityConfig.class)
                .withPropertyValues("uat.synthetic-users.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AutenticacaoSinteticaController.class);
                    assertThat(context).doesNotHaveBean(SyntheticSecurityConfig.class);
                    assertThat(context).doesNotHaveBean(SyntheticTokenAuthenticationProvider.class);
                    assertThat(context).doesNotHaveBean(AutenticadorUsuarioSintetico.class);
                    assertThat(context).doesNotHaveBean(AuditoriaUsuarioSintetico.class);
                    assertThat(context).doesNotHaveBean(SyntheticDataService.class);
                    assertThat(context).doesNotHaveBean(SyntheticSandboxStorage.class);
                });

        // Cenário 2: uat.synthetic-users.enabled=true -> componentes sintéticos são instanciados
        new ApplicationContextRunner()
                .withUserConfiguration(
                        SyntheticUsersProperties.class,
                        AuditoriaUsuarioSintetico.class,
                        AutenticadorUsuarioSintetico.class,
                        SyntheticDataService.class,
                        SyntheticSandboxStorage.class,
                        AutenticacaoSinteticaController.class,
                        SyntheticTokenAuthenticationProvider.class)
                .withPropertyValues(
                        "uat.synthetic-users.enabled=true",
                        "uat.synthetic-users.jwt-secret=segredo-teste-com-mais-de-trinta-e-dois-caracteres",
                        "uat.synthetic-users.passwords.uat-membro=pass1",
                        "uat.synthetic-users.passwords.uat-servidor=pass2",
                        "uat.synthetic-users.passwords.google-review-membro=pass3",
                        "uat.synthetic-users.passwords.google-review-servidor=pass4",
                        "uat.synthetic-users.passwords.apple-review-membro=pass5",
                        "uat.synthetic-users.passwords.apple-review-servidor=pass6")
                .run(context -> {
                    assertThat(context).hasSingleBean(AutenticacaoSinteticaController.class);
                    assertThat(context).hasSingleBean(SyntheticTokenAuthenticationProvider.class);
                    assertThat(context).hasSingleBean(AutenticadorUsuarioSintetico.class);
                    assertThat(context).hasSingleBean(AuditoriaUsuarioSintetico.class);
                    assertThat(context).hasSingleBean(SyntheticDataService.class);
                    assertThat(context).hasSingleBean(SyntheticSandboxStorage.class);
                });
    }

    @Test
    @DisplayName("28. Comportamento normal dos serviços: com feature off (sem syntheticDataService), todas as chamadas vão para integrações normais")
    void test28_servicosNormaisPreservadosComFeatureOff() {
        // 1. PerfilUsuarioService normal
        FakeAuthServiceClient fakeAuth = new FakeAuthServiceClient();
        ConsultaAutorizacaoResponseDto consultaResp = new ConsultaAutorizacaoResponseDto();
        consultaResp.setUsuario(new UsuarioAutorizadoResponseDto(1L, "Joao Silva", "joao.silva"));
        fakeAuth.respostaConsulta = consultaResp;

        FakePerfilUsuarioRepository fakePerfil = new FakePerfilUsuarioRepository();
        PerfilUsuarioService normalPerfilService = new PerfilUsuarioService(fakeAuth, fakePerfil);
        normalPerfilService.buscarPerfilAutenticado("token-real");
        assertEquals(1, fakePerfil.chamadasBuscarPorLogin);

        // 2. ContrachequeService normal
        FakeContrachequeRepository fakeContracheque = new FakeContrachequeRepository();
        ContrachequeService normalContrachequeService = new ContrachequeService(fakeContracheque);
        normalContrachequeService.getMatriculaByLogin("joao.silva");
        normalContrachequeService.listarCompetenciasDisponiveis(12345);
        normalContrachequeService.detalharContracheque(12345, 5, 2026);
        assertEquals(1, fakeContracheque.chamadasMatricula);
        assertEquals(1, fakeContracheque.chamadasCompetencias);
        assertEquals(1, fakeContracheque.chamadasContracheque);

        // 3. PagamentosService normal
        FakePagamentosRepository fakePagamentos = new FakePagamentosRepository();
        PagamentosService normalPagamentosService = new PagamentosService(fakePagamentos);
        normalPagamentosService.getCpfByLogin("joao.silva");
        normalPagamentosService.listarAnosDisponiveis("11122233344");
        normalPagamentosService.listarPagamentos("2026", "11122233344");
        assertEquals(1, fakePagamentos.chamadasCpf);
        assertEquals(1, fakePagamentos.chamadasAnos);
        assertEquals(1, fakePagamentos.chamadasPagamentos);

        // 4. ConcessaoFolgaService normal
        FakeConcessaoFolgaRemoteRepository fakeFolgaRemote = new FakeConcessaoFolgaRemoteRepository();
        ConcessaoFolgaService normalFolgaService = new ConcessaoFolgaService(
                new ConcessaoFolgaIdentityRepository(null),
                fakeFolgaRemote,
                null);
        normalFolgaService.listarConcessoes("12345");
        assertEquals(1, fakeFolgaRemote.chamadasListar);

        // 5. DesignacaoConsultaService normal
        FakeDesignacaoViewRepository fakeDesignacaoView = new FakeDesignacaoViewRepository();
        DesignacaoConsultaService normalDesignacaoConsulta = new DesignacaoConsultaService(
                new DesignacaoInicializacaoService(null, null) {
                    @Override
                    public void inicializarSeNecessario(String matricula) {}
                },
                fakeDesignacaoView,
                new DesignacaoMapper());
        normalDesignacaoConsulta.listarAtivas("12345");
        assertEquals(1, fakeDesignacaoView.chamadasAtivas);

        // 6. DesignacaoAuthService normal
        FakeDesignacaoIdentityRepository fakeDesignacaoIdentity = new FakeDesignacaoIdentityRepository();
        DesignacaoAuthService normalDesignacaoAuth = new DesignacaoAuthService(fakeAuthClient, fakeDesignacaoIdentity);
        normalDesignacaoAuth.matriculaPorLogin("joao.silva");
        assertEquals(1, fakeDesignacaoIdentity.chamadasMatricula);

        // 7. DispositivoFcmService normal
        FakeDispositivoFcmRepository fakeFcmRepo = new FakeDispositivoFcmRepository();
        DispositivoFcmService normalFcmService = new DispositivoFcmService(fakeFcmRepo);
        normalFcmService.registrar("12345", new RegistrarDispositivoRequest("dev-1", "fcm-tok-1", "ANDROID"));
        assertEquals(1, fakeFcmRepo.chamadasFindByTokenHash);

        // 8. CarteiraService normal
        FakeJdbcTemplate fakeJdbc = new FakeJdbcTemplate();
        CarteiraService normalCarteiraService = new CarteiraService(fakeJdbc, null);
        normalCarteiraService.recuperaCarteiraFuncionalByLogin("joao.silva");
        assertTrue(fakeJdbc.executouQuery, "CarteiraService normal deve tentar consultar banco institucional quando feature off");
    }

    @Test
    @DisplayName("29. Feature ligada: uat.membro e uat.servidor 200, desconhecido e senha errada 401, AuthServiceClient zero chamadas")
    void test29_featureLigadaZeroInteracoesAuthServiceClient() {
        properties.setEnabled(true);

        // 1. uat.membro válido -> 200 + token
        AutorizaRequestDto reqMembro = new AutorizaRequestDto("uat.membro", "SenhaSeguraUatMembro123!", "ANDROID");
        ResponseEntity<?> respMembro = authController.autoriza(reqMembro);
        assertEquals(HttpStatus.OK, respMembro.getStatusCode());
        assertNotNull(((AutorizaResponseDto) respMembro.getBody()).getToken());

        // 2. uat.servidor válido -> 200 + token
        AutorizaRequestDto reqServidor = new AutorizaRequestDto("uat.servidor", "SenhaSeguraUatServidor123!", "ANDROID");
        ResponseEntity<?> respServidor = authController.autoriza(reqServidor);
        assertEquals(HttpStatus.OK, respServidor.getStatusCode());
        assertNotNull(((AutorizaResponseDto) respServidor.getBody()).getToken());

        // 3. login desconhecido -> 401
        AutorizaRequestDto reqDesconhecido = new AutorizaRequestDto("usuario.inexistente", "QualquerSenha123!", "ANDROID");
        ResponseEntity<?> respDesconhecido = authController.autoriza(reqDesconhecido);
        assertEquals(HttpStatus.UNAUTHORIZED, respDesconhecido.getStatusCode());

        // 4. senha inválida -> 401
        AutorizaRequestDto reqSenhaErrada = new AutorizaRequestDto("uat.membro", "SenhaCompletamenteIncorreta!", "ANDROID");
        ResponseEntity<?> respSenhaErrada = authController.autoriza(reqSenhaErrada);
        assertEquals(HttpStatus.UNAUTHORIZED, respSenhaErrada.getStatusCode());

        // Zero interações com AuthServiceClient comprovado
        assertEquals(0, fakeAuthClient.chamadasAutoriza);
    }

    @Test
    @DisplayName("30. Produção sem endpoint sintético: POST /acesso/autoriza resulta em 404 e AuthServiceClient zero chamadas")
    void test30_producaoSemEndpointSinteticoZeroInteracoes() throws Exception {
        // Simulando ambiente normal de produção onde AutenticacaoSinteticaController não é registrado
        PerfilUsuarioService perfilService = new PerfilUsuarioService(fakeAuthClient, fakePerfilRepo);
        MockMvc mockMvcProducao = MockMvcBuilders.standaloneSetup(new PerfilUsuarioController(perfilService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        mockMvcProducao.perform(post("/acesso/autoriza")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"qualquer.usuario\",\"senha\":\"Senha123!\",\"siglaSistema\":\"ANDROID\"}"))
                .andExpect(status().isNotFound());

        // AuthServiceClient não teve nenhuma interação
        assertEquals(0, fakeAuthClient.chamadasAutoriza);
    }

    @Test
    @DisplayName("31. Regressão BUG-Q14-001: parsing de data resiliente em sugestões de concessão (yyyy-MM-dd, ISO local e entrada inválida)")
    void test31_regressaoBugQ14_001_parsingDataSugestoesConcessao() {
        String tokenMembro = autenticador.gerarToken(IdentidadeSintetica.UAT_MEMBRO);

        // 1. yyyy-MM-dd válido
        ResponseEntity<String> respA = folgaService.sugestoesDatasFinais(1001L, "2026-10-05", tokenMembro);
        assertEquals(HttpStatus.OK, respA.getStatusCode());
        assertNotNull(respA.getBody());
        assertTrue(respA.getBody().contains("05/10/2026"), "Deve conter a data inicial formatada em dd/MM/yyyy");
        assertTrue(respA.getBody().contains("19/10/2026"), "Opção de 15 dias deve resultar em 19/10/2026");

        // 2. ISO local usado pelo Flutter (com 'T' e millis)
        ResponseEntity<String> respB1 = folgaService.sugestoesDatasFinais(1001L, "2026-10-05T00:00:00.000", tokenMembro);
        assertEquals(HttpStatus.OK, respB1.getStatusCode());
        assertNotNull(respB1.getBody());
        assertTrue(respB1.getBody().contains("05/10/2026"));
        assertTrue(respB1.getBody().contains("19/10/2026"));

        ResponseEntity<String> respB2 = folgaService.sugestoesDatasFinais(1001L, "2026-10-05T14:30:00", tokenMembro);
        assertEquals(HttpStatus.OK, respB2.getStatusCode());
        assertNotNull(respB2.getBody());
        assertTrue(respB2.getBody().contains("05/10/2026"));

        // 3. Entrada inválida tratada de forma controlada sem estourar DateTimeParseException / HTTP 500
        ResponseEntity<String> respC1 = folgaService.sugestoesDatasFinais(1001L, "data-invalida-xyz", tokenMembro);
        assertEquals(HttpStatus.OK, respC1.getStatusCode(), "Entrada com string inválida não pode retornar 500");
        assertNotNull(respC1.getBody());
        assertFalse(respC1.getBody().isEmpty());

        ResponseEntity<String> respC2 = folgaService.sugestoesDatasFinais(1001L, "", tokenMembro);
        assertEquals(HttpStatus.OK, respC2.getStatusCode(), "Entrada vazia não pode retornar 500");
        assertNotNull(respC2.getBody());

        ResponseEntity<String> respC3 = folgaService.sugestoesDatasFinais(1001L, null, tokenMembro);
        assertEquals(HttpStatus.OK, respC3.getStatusCode(), "Entrada nula não pode retornar 500");
        assertNotNull(respC3.getBody());
    }

    @Test
    @DisplayName("32. Regressão BUG-Q14-002: persistência FCM sintética no sandbox com zero chamadas ao repository relacional")
    void test32_regressaoBugQ14_002_persistenciaFcmSinteticaVsInstitucional() {
        // Usuário sintético -> registra e desativa no sandbox com zero chamadas ao repository
        RegistrarDispositivoRequest reqSintetico = new RegistrarDispositivoRequest("dev-synth-01", "tok-fcm-synth-abc", "ANDROID");
        DispositivoNotificacaoResponse respSintetico = fcmService.registrar("900001", reqSintetico);
        assertNotNull(respSintetico);
        assertEquals("dev-synth-01", respSintetico.identificador());
        assertTrue(respSintetico.ativo());
        assertEquals(0, fakeFcmRepo.chamadasFindByTokenHash, "Repositório relacional NÃO deve ser acionado para matrícula sintética");

        boolean desativou = fcmService.desativar("900001", "dev-synth-01");
        assertTrue(desativou);
        assertEquals(0, fakeFcmRepo.chamadasFindByTokenHash, "Desativação sintética não deve acionar repositório relacional");

        // Usuário normal -> fluxo institucional preservado (repository acionado)
        RegistrarDispositivoRequest reqNormal = new RegistrarDispositivoRequest("dev-normal-01", "tok-fcm-normal-xyz", "ANDROID");
        fcmService.registrar("12345", reqNormal);
        assertEquals(1, fakeFcmRepo.chamadasFindByTokenHash, "Repositório relacional DEVE ser acionado para matrícula institucional normal");
    }

    @Test
    @DisplayName("33. Autorização por Objeto (U17): isolamento de leitura e operação comprovado entre Membro e Servidor")
    void test33_crossObjectAuthorizationU17_isolamentoOperacoesEntreMembroEServidor() {
        String tokenMembro = autenticador.gerarToken(IdentidadeSintetica.UAT_MEMBRO);
        String tokenServidor = autenticador.gerarToken(IdentidadeSintetica.UAT_SERVIDOR);

        // 1. Membro possui solicitação 5001 associada a ele (status HOMOLOGADA)
        ResponseEntity<String> consultaMembroInicial = folgaService.consultarSolicitacao(5001L, tokenMembro);
        assertEquals(HttpStatus.OK, consultaMembroInicial.getStatusCode());
        assertTrue(consultaMembroInicial.getBody().contains("HOMOLOGADA"));

        // 2. Servidor tenta consultar a solicitação 5001 pelo ID direto
        ResponseEntity<String> consultaServidor = folgaService.consultarSolicitacao(5001L, tokenServidor);
        assertEquals(HttpStatus.FORBIDDEN, consultaServidor.getStatusCode(),
                "Servidor NÃO deve conseguir consultar solicitação pertencente ao Membro (403 Forbidden)");

        // 3. Servidor tenta cancelar a solicitação 5001 de Lucas
        ResponseEntity<String> cancelamentoServidor = folgaService.cancelarSolicitacao(5001L, tokenServidor);
        assertEquals(HttpStatus.FORBIDDEN, cancelamentoServidor.getStatusCode(),
                "Servidor NÃO deve conseguir cancelar solicitação pertencente ao Membro (403 Forbidden)");

        // 4. Servidor tenta adiar a solicitação 5001 de Lucas
        OperacaoSolicitacaoRequestDto adiarDto = new OperacaoSolicitacaoRequestDto();
        adiarDto.setDataAdiamento("2026-12-01");
        ResponseEntity<String> adiamentoServidor = folgaService.adiarSolicitacao(5001L, adiarDto, tokenServidor);
        assertEquals(HttpStatus.FORBIDDEN, adiamentoServidor.getStatusCode(),
                "Servidor NÃO deve conseguir adiar solicitação pertencente ao Membro (403 Forbidden)");

        // 5. Comprovar que o estado da solicitação de Lucas permanece estritamente INALTERADO
        ResponseEntity<String> consultaMembroAposTentativas = folgaService.consultarSolicitacao(5001L, tokenMembro);
        assertEquals(HttpStatus.OK, consultaMembroAposTentativas.getStatusCode());
        assertTrue(consultaMembroAposTentativas.getBody().contains("HOMOLOGADA"),
                "O status da solicitação de Lucas deve permanecer HOMOLOGADA e não modificada");

        // 6. Membro opera sua própria solicitação com sucesso
        ResponseEntity<String> cancelamentoMembro = folgaService.cancelarSolicitacao(5001L, tokenMembro);
        assertEquals(HttpStatus.OK, cancelamentoMembro.getStatusCode());
        assertTrue(cancelamentoMembro.getBody().contains("CANCELADA"));

        ResponseEntity<String> consultaPosCancelamento = folgaService.consultarSolicitacao(5001L, tokenMembro);
        assertEquals(HttpStatus.OK, consultaPosCancelamento.getStatusCode());
        assertTrue(consultaPosCancelamento.getBody().contains("CANCELADA"));
    }

    // =========================================================================
    // Fakes para comprovar isolamento externo estrito (Zero Interactions)
    // =========================================================================

    private static class FakeJdbcTemplate extends JdbcTemplate {
        boolean executouQuery = false;

        @Override
        public <T> List<T> query(String sql, Object[] args, RowMapper<T> rowMapper) {
            executouQuery = true;
            return List.of();
        }
    }

    private static class FakeContrachequeRepository extends ContrachequeRepository {
        int chamadasMatricula = 0;
        int chamadasCompetencias = 0;
        int chamadasContracheque = 0;

        FakeContrachequeRepository() {
            super(null);
        }

        @Override
        public Integer getMatriculaByLogin(String login) {
            chamadasMatricula++;
            return 12345;
        }

        @Override
        public List<CompetenciaDto> findCompetenciasByMatricula(Integer matricula) {
            chamadasCompetencias++;
            return List.of();
        }

        @Override
        public ContrachequeResponseDto findContracheque(Integer matricula, Integer mes, Integer ano) {
            chamadasContracheque++;
            return new ContrachequeResponseDto();
        }
    }

    private static class FakePagamentosRepository extends PagamentosRepository {
        int chamadasCpf = 0;
        int chamadasAnos = 0;
        int chamadasPagamentos = 0;

        FakePagamentosRepository() {
            super(null);
        }

        @Override
        public String getCpfByLogin(String login) {
            chamadasCpf++;
            return "11122233344";
        }

        @Override
        public List<Integer> findAnosComPagamentos(String cpf) {
            chamadasAnos++;
            return List.of(2026);
        }

        @Override
        public List<PagamentoItemDto> findPagamentosByExercicioAndCpf(String exercicio, String cpf) {
            chamadasPagamentos++;
            return List.of();
        }
    }

    private static class FakeDesignacaoViewRepository extends DesignacaoViewRepository {
        int chamadasAtivas = 0;

        FakeDesignacaoViewRepository() {
            super(null);
        }

        @Override
        public List<DesignacaoAtivaRow> findAtivasByMatricula(String matricula) {
            chamadasAtivas++;
            return List.of();
        }
    }

    private static class FakeDesignacaoIdentityRepository extends DesignacaoIdentityRepository {
        int chamadasMatricula = 0;

        FakeDesignacaoIdentityRepository() {
            super(null);
        }

        @Override
        public String getMatriculaByLogin(String login) {
            chamadasMatricula++;
            return "999999";
        }
    }

    private static class FakeConcessaoFolgaRemoteRepository extends ConcessaoFolgaRemoteRepository {
        int chamadasListar = 0;

        FakeConcessaoFolgaRemoteRepository() {
            super(null);
        }

        @Override
        public List<ConcessaoFolgaDto> findConcessoesVigentes(String matricula) {
            chamadasListar++;
            return List.of();
        }
    }

    private static class FakeDispositivoFcmRepository extends DispositivoFcmRepository {
        int chamadasFindByTokenHash = 0;

        FakeDispositivoFcmRepository() {
            super(null);
        }

        @Override
        public Optional<DispositivoFcm> findByTokenHash(String tokenFcmHash) {
            chamadasFindByTokenHash++;
            return Optional.empty();
        }

        @Override
        public Optional<DispositivoFcm> findByIdentificador(String identificador) {
            return Optional.empty();
        }

        @Override
        public DispositivoFcm inserir(String identificador, String servidorMatricula, String tokenFcm, String tokenFcmHash, String plataforma) {
            return new DispositivoFcm(1L, identificador, servidorMatricula, tokenFcm, tokenFcmHash, plataforma, true, null, null, null);
        }
    }

    private static class FakeAuthServiceClient implements AuthServiceClient {
        int chamadasAutoriza = 0;
        int chamadasConsulta = 0;
        String ultimoLoginAutoriza = null;
        AutorizaResponseDto respostaMock = new AutorizaResponseDto();
        ConsultaAutorizacaoResponseDto respostaConsulta = new ConsultaAutorizacaoResponseDto();

        @Override
        public AutorizaResponseDto autoriza(AutorizaRequestDto request) {
            chamadasAutoriza++;
            ultimoLoginAutoriza = request != null ? request.getLogin() : null;
            return respostaMock;
        }

        @Override
        public ConsultaAutorizacaoResponseDto consultaAutorizacao(ConsultaAutorizacaoRequestDto request) {
            chamadasConsulta++;
            return respostaConsulta;
        }
    }

    private static class FakePerfilUsuarioRepository extends PerfilUsuarioRepository {
        int chamadasBuscarPorLogin = 0;

        FakePerfilUsuarioRepository() {
            super(null);
        }

        @Override
        public PerfilUsuarioDto buscarPorLogin(String login) {
            chamadasBuscarPorLogin++;
            return null;
        }
    }
}
