package mppb.restapi.mobile.quality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import mppb.restapi.common.infrastructure.client.authapi.AuthServiceClient;
import mppb.restapi.common.infrastructure.client.authapi.dto.AcessoDataResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.AutorizaRequestDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.AutorizaResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoRequestDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.UsuarioAutorizadoResponseDto;
import mppb.restapi.common.infrastructure.security.authorization.TokenAuthentication;
import mppb.restapi.common.infrastructure.security.filter.CustomBasicAuthenticationFilter;
import mppb.restapi.common.infrastructure.security.filter.CustomUsernamePasswordAuthenticationFilter;
import mppb.restapi.common.infrastructure.security.manager.CustomAuthenticationManager;
import mppb.restapi.mobile.feature.designacao.repository.DesignacaoIdentityRepository;
import mppb.restapi.mobile.feature.designacao.service.DesignacaoAuthService;
import mppb.restapi.mobile.feature.notificacao.controller.DispositivoNotificacaoController;
import mppb.restapi.mobile.feature.notificacao.model.DispositivoFcm;
import mppb.restapi.mobile.feature.notificacao.repository.DispositivoFcmRepository;
import mppb.restapi.mobile.feature.notificacao.service.DispositivoFcmService;
import mppb.restapi.mobile.feature.pagamentos.controller.PagamentosController;
import mppb.restapi.mobile.feature.pagamentos.dto.PagamentoItemDto;
import mppb.restapi.mobile.feature.pagamentos.repository.PagamentosRepository;
import mppb.restapi.mobile.feature.pagamentos.service.PagamentosService;
import mppb.restapi.mobile.feature.perfil.controller.PerfilUsuarioController;
import mppb.restapi.mobile.feature.perfil.dto.PerfilUsuarioDto;
import mppb.restapi.mobile.feature.perfil.repository.PerfilUsuarioRepository;
import mppb.restapi.mobile.feature.perfil.service.PerfilUsuarioService;

class Q4IntegracaoControladaTest {

    private FakeAuthServiceClient authClient;
    private FakePerfilRepository perfilRepository;
    private FakePagamentosRepository pagamentosRepository;
    private FakeDesignacaoIdentityRepository identityRepository;
    private FakeDispositivoFcmRepository dispositivoRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        authClient = new FakeAuthServiceClient();
        perfilRepository = new FakePerfilRepository();
        pagamentosRepository = new FakePagamentosRepository();
        identityRepository = new FakeDesignacaoIdentityRepository();
        dispositivoRepository = new FakeDispositivoFcmRepository();

        PerfilUsuarioService perfilService = new PerfilUsuarioService(authClient, perfilRepository);
        ReflectionTestUtils.setField(perfilService, "siglaSistema", "ANDROID");
        PagamentosService pagamentosService = new PagamentosService(pagamentosRepository);
        DesignacaoAuthService designacaoAuthService = new DesignacaoAuthService(authClient, identityRepository);
        DispositivoFcmService dispositivoService = new DispositivoFcmService(dispositivoRepository);

        CustomAuthenticationManager manager = new CustomAuthenticationManager(
                List.of(new StubTokenAuthenticationProvider()));

        mockMvc = MockMvcBuilders.standaloneSetup(
                new PerfilUsuarioController(perfilService),
                new PagamentosController(pagamentosService),
                new DispositivoNotificacaoController(designacaoAuthService, dispositivoService))
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
    void contextoSpringQ4CarregaWiringRepresentativoSemInfraExterna() {
        new ApplicationContextRunner()
                .withUserConfiguration(Q4ContextConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(PerfilUsuarioController.class);
                    assertThat(context).hasSingleBean(PagamentosController.class);
                    assertThat(context).hasSingleBean(DispositivoNotificacaoController.class);
                    assertThat(context).hasSingleBean(CustomBasicAuthenticationFilter.class);
                    assertThat(context).hasSingleBean(CustomUsernamePasswordAuthenticationFilter.class);
                });
    }

    @Test
    void perfilAtravessaFiltroSecurityControllerServiceAuthApiERepositorio() throws Exception {
        mockMvc.perform(get("/api/perfil/dados")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token-valido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("usuario.q4"))
                .andExpect(jsonPath("$.matricula").value("12345"));

        assertThat(authClient.tokenConsultaRecebido).isEqualTo("token-valido");
        assertThat(perfilRepository.loginRecebido).isEqualTo("usuario.q4");
    }

    @Test
    void bearerInvalidoRetorna401AntesDoController() throws Exception {
        mockMvc.perform(get("/api/perfil/dados")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"error\":\"Unauthorized\"}"));

        assertThat(authClient.tokenConsultaRecebido).isNull();
        assertThat(perfilRepository.loginRecebido).isNull();
    }

    @Test
    void pagamentosAtravessaSecurityControllerServiceERepositorioComCpfNormalizado() throws Exception {
        mockMvc.perform(get("/pagamentos/2026")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token-valido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nuExercicio").value("2026"))
                .andExpect(jsonPath("$.totalPagamentos").value(1))
                .andExpect(jsonPath("$.totalBruto").value(1000.50))
                .andExpect(jsonPath("$.totalLiquido").value(900.25));

        assertThat(pagamentosRepository.loginCpfRecebido).isEqualTo("usuario.q4");
        assertThat(pagamentosRepository.cpfPagamentosRecebido).isEqualTo("12345678900");
    }

    @Test
    void notificacaoRegistraDispositivoComIdentidadeAutenticadaSemExporToken() throws Exception {
        mockMvc.perform(post("/api/notificacoes/dispositivos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token-valido")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "identificador": "device-q4",
                          "tokenFcm": "token-fcm-sanitizado",
                          "plataforma": "ANDROID"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificador").value("device-q4"))
                .andExpect(jsonPath("$.plataforma").value("ANDROID"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.tokenFcm").doesNotExist());

        assertThat(identityRepository.loginRecebido).isEqualTo("usuario.q4");
        assertThat(dispositivoRepository.matriculaInserida).isEqualTo("12345");
        assertThat(dispositivoRepository.tokenFcmInserido).isEqualTo("token-fcm-sanitizado");
        assertThat(dispositivoRepository.tokenHashInserido).isNotBlank();
    }

    @Test
    void notificacaoSemIdentidadeLocalRetorna401SemPersistir() throws Exception {
        identityRepository.matricula = null;

        mockMvc.perform(post("/api/notificacoes/dispositivos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token-valido")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "identificador": "device-q4",
                          "tokenFcm": "token-fcm-sanitizado",
                          "plataforma": "ANDROID"
                        }
                        """))
                .andExpect(status().isUnauthorized());

        assertThat(dispositivoRepository.inserirChamado).isFalse();
    }

    @Configuration
    static class Q4ContextConfig {

        @Bean
        AuthServiceClient authServiceClient() {
            return new FakeAuthServiceClient();
        }

        @Bean
        PerfilUsuarioRepository perfilUsuarioRepository() {
            return new FakePerfilRepository();
        }

        @Bean
        PerfilUsuarioService perfilUsuarioService(AuthServiceClient authClient, PerfilUsuarioRepository repository) {
            PerfilUsuarioService service = new PerfilUsuarioService(authClient, repository);
            ReflectionTestUtils.setField(service, "siglaSistema", "ANDROID");
            return service;
        }

        @Bean
        PerfilUsuarioController perfilUsuarioController(PerfilUsuarioService service) {
            return new PerfilUsuarioController(service);
        }

        @Bean
        PagamentosRepository pagamentosRepository() {
            return new FakePagamentosRepository();
        }

        @Bean
        PagamentosService pagamentosService(PagamentosRepository repository) {
            return new PagamentosService(repository);
        }

        @Bean
        PagamentosController pagamentosController(PagamentosService service) {
            return new PagamentosController(service);
        }

        @Bean
        DesignacaoIdentityRepository designacaoIdentityRepository() {
            return new FakeDesignacaoIdentityRepository();
        }

        @Bean
        DesignacaoAuthService designacaoAuthService(AuthServiceClient authClient, DesignacaoIdentityRepository repository) {
            return new DesignacaoAuthService(authClient, repository);
        }

        @Bean
        DispositivoFcmRepository dispositivoFcmRepository() {
            return new FakeDispositivoFcmRepository();
        }

        @Bean
        DispositivoFcmService dispositivoFcmService(DispositivoFcmRepository repository) {
            return new DispositivoFcmService(repository);
        }

        @Bean
        DispositivoNotificacaoController dispositivoNotificacaoController(
                DesignacaoAuthService authService,
                DispositivoFcmService service) {
            return new DispositivoNotificacaoController(authService, service);
        }

        @Bean
        CustomAuthenticationManager customAuthenticationManager() {
            return new CustomAuthenticationManager(List.of(new StubTokenAuthenticationProvider()));
        }

        @Bean
        CustomBasicAuthenticationFilter customBasicAuthenticationFilter(CustomAuthenticationManager manager) {
            return new CustomBasicAuthenticationFilter(manager);
        }

        @Bean
        CustomUsernamePasswordAuthenticationFilter customUsernamePasswordAuthenticationFilter(
                CustomAuthenticationManager manager) {
            return new CustomUsernamePasswordAuthenticationFilter(manager);
        }
    }

    private static class StubTokenAuthenticationProvider implements AuthenticationProvider {

        @Override
        public Authentication authenticate(Authentication authentication) throws AuthenticationException {
            TokenAuthentication token = (TokenAuthentication) authentication;
            if (!"token-valido".equals(token.getToken())) {
                throw new BadCredentialsException("token invalido q4");
            }
            return new TokenAuthentication(
                    token.getToken(),
                    new UsuarioAutorizadoResponseDto(1L, "Usuario Q4", "usuario.q4"),
                    new AcessoDataResponseDto(List.of(), List.of(), List.of(), List.of()),
                    null,
                    AuthorityUtils.createAuthorityList("ROLE_USER"));
        }

        @Override
        public boolean supports(Class<?> authentication) {
            return TokenAuthentication.class.equals(authentication);
        }
    }

    private static class FakeAuthServiceClient implements AuthServiceClient {

        private String tokenConsultaRecebido;

        @Override
        public AutorizaResponseDto autoriza(AutorizaRequestDto autorizaRequestTO) {
            return null;
        }

        @Override
        public ConsultaAutorizacaoResponseDto consultaAutorizacao(
                ConsultaAutorizacaoRequestDto consultaAutorizacaoRequestTO) {
            tokenConsultaRecebido = consultaAutorizacaoRequestTO.getToken();
            return new ConsultaAutorizacaoResponseDto(
                    new AcessoDataResponseDto(List.of(), List.of(), List.of(), List.of()),
                    new UsuarioAutorizadoResponseDto(1L, "Usuario Q4", "usuario.q4"));
        }
    }

    private static class FakePerfilRepository extends PerfilUsuarioRepository {

        private String loginRecebido;

        FakePerfilRepository() {
            super(null);
        }

        @Override
        public PerfilUsuarioDto buscarPorLogin(String login) {
            loginRecebido = login;
            PerfilUsuarioDto perfil = new PerfilUsuarioDto();
            perfil.setLogin(login);
            perfil.setNome("Usuario Q4");
            perfil.setMatricula("12345");
            perfil.setSituacao("Ativo");
            return perfil;
        }
    }

    private static class FakePagamentosRepository extends PagamentosRepository {

        private String loginCpfRecebido;
        private String cpfPagamentosRecebido;

        FakePagamentosRepository() {
            super(null);
        }

        @Override
        public String getCpfByLogin(String login) {
            loginCpfRecebido = login;
            return "123.456.789-00";
        }

        @Override
        public List<Integer> findAnosComPagamentos(String cpf) {
            return List.of(2026);
        }

        @Override
        public List<PagamentoItemDto> findPagamentosByExercicioAndCpf(String exercicio, String cpf) {
            cpfPagamentosRecebido = cpf;
            PagamentoItemDto item = new PagamentoItemDto();
            item.setNuExercicio(exercicio);
            item.setNuProcesso("Q4-1");
            item.setVlBruto(new BigDecimal("1000.50"));
            item.setVlLiquido(new BigDecimal("900.25"));
            return List.of(item);
        }
    }

    private static class FakeDesignacaoIdentityRepository extends DesignacaoIdentityRepository {

        private String matricula = "12345";
        private String loginRecebido;

        FakeDesignacaoIdentityRepository() {
            super(null);
        }

        @Override
        public String getMatriculaByLogin(String login) {
            loginRecebido = login;
            return matricula;
        }
    }

    private static class FakeDispositivoFcmRepository extends DispositivoFcmRepository {

        private boolean inserirChamado;
        private String matriculaInserida;
        private String tokenFcmInserido;
        private String tokenHashInserido;

        FakeDispositivoFcmRepository() {
            super(null);
        }

        @Override
        public Optional<DispositivoFcm> findByTokenHash(String tokenFcmHash) {
            return Optional.empty();
        }

        @Override
        public Optional<DispositivoFcm> findByIdentificador(String identificador) {
            return Optional.empty();
        }

        @Override
        public DispositivoFcm inserir(
                String identificador,
                String matricula,
                String tokenFcm,
                String tokenFcmHash,
                String plataforma) {
            inserirChamado = true;
            matriculaInserida = matricula;
            tokenFcmInserido = tokenFcm;
            tokenHashInserido = tokenFcmHash;
            LocalDateTime agora = LocalDateTime.of(2026, 9, 28, 12, 0);
            return new DispositivoFcm(1L, identificador, matricula, tokenFcm, tokenFcmHash, plataforma, true, agora,
                    agora, agora);
        }
    }
}
