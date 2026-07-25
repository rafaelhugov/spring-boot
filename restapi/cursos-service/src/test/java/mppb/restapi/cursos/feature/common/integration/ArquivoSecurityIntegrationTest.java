package mppb.restapi.cursos.feature.common.integration;

import feign.FeignException;
import mppb.restapi.common.core.error.NotFoundException;
import mppb.restapi.common.infrastructure.client.authapi.AuthServiceClient;
import mppb.restapi.common.infrastructure.client.authapi.dto.AcessoDataResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.FuncionalidadeAutorizadaResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.UsuarioAutorizadoResponseDto;
import mppb.restapi.common.infrastructure.client.mpvirtual.MPVirtualClient;
import mppb.restapi.common.infrastructure.security.authorization.TokenAuthentication;
import mppb.restapi.cursos.CursosApiApplication;
import mppb.restapi.cursos.feature.common.repository.ArquivoRepository;
import mppb.restapi.cursos.feature.documentos.service.DocumentosService;
import mppb.restapi.cursos.feature.manual.service.ManualPdfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(
        classes = CursosApiApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.profiles.active=integration",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true",
                "spring.jpa.show-sql=false",
                "spring.task.scheduling.enabled=false",
                "api.auth.url=http://127.0.0.1:1",
                "api.mpvirtual.url=http://127.0.0.1:1"
        })
@AutoConfigureMockMvc
@Sql(scripts = {
        "/sql/2026-07-16_curso_arquivo_comprobatorio.sql",
        "/sql/2026-07-17_inscricao_acessibilidade.sql"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArquivoSecurityIntegrationTest {

    static {
        System.setProperty("api.version", "1.40");
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("cursos_it")
            .withUsername("cursos")
            .withPassword("cursos");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ArquivoRepository arquivoRepository;

    @MockBean
    private AuthServiceClient authServiceClient;

    @MockBean
    private MPVirtualClient mpVirtualClient;

    @MockBean
    private DocumentosService documentosService;

    @MockBean
    private ManualPdfService manualPdfService;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                truncate table
                  curso.cursoarquivocomprobatorio,
                  curso.solicitacaoministrante,
                  curso.solicitacaocurso,
                  curso.ministrante,
                  curso.curso,
                  ged.arquivo
                restart identity cascade
                """);
    }

    @Test
    void endpointPrivadoSemAutenticacaoRetorna401() throws Exception {
        mockMvc.perform(get("/api/arquivos/100"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpointPrivadoComTokenInvalidoRetorna401() throws Exception {
        FeignException feignException = mock(FeignException.class);
        when(authServiceClient.consultaAutorizacao(any())).thenThrow(feignException);

        mockMvc.perform(get("/api/arquivos/100")
                        .header(HttpHeaders.AUTHORIZATION, "Token token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpointPrivadoComUsuarioSemAuthorityRetorna403() throws Exception {
        mockMvc.perform(get("/api/arquivos/100")
                        .with(user("sem.permissao")))
                .andExpect(status().isForbidden());
    }

    @Test
    void authorityInexistenteNaoConcedeAcesso() throws Exception {
        mockMvc.perform(get("/api/arquivos/100")
                        .with(user("perfil.errado").authorities(new SimpleGrantedAuthority("cursos.arquivo.baixar"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorBaixaArquivoComprobatorioComHeadersSeguros() throws Exception {
        inserirArquivo(100L, "comprovante.pdf", MediaType.APPLICATION_PDF_VALUE, pdfBytes());
        inserirCurso(10L, "F", null);
        inserirArquivoComprobatorio(10L, 100L);

        mockMvc.perform(get("/api/arquivos/100")
                        .with(usuario(999L, "admin", "cursos.manter.cursos")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pdfBytes()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("comprovante.pdf")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, not(containsString("/"))))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, not(containsString(".."))))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));
    }

    @Test
    void usuarioComCursosAcessoNaoBaixaArquivoComprobatorioAdministrativo() throws Exception {
        inserirArquivo(100L, "comprovante.pdf", MediaType.APPLICATION_PDF_VALUE, pdfBytes());
        inserirCurso(10L, "F", null);
        inserirArquivoComprobatorio(10L, 100L);

        mockMvc.perform(get("/api/arquivos/100")
                        .with(usuario(501L, "usuario.a", "cursos.acesso")))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioComCursosAcessoEAnalisarSolicitacoesNaoBaixaComprovatorioAdministrativo() throws Exception {
        inserirArquivo(100L, "comprovante.pdf", MediaType.APPLICATION_PDF_VALUE, pdfBytes());
        inserirCurso(10L, "F", null);
        inserirArquivoComprobatorio(10L, 100L);

        mockMvc.perform(get("/api/arquivos/100")
                        .with(usuario(501L, "usuario.a", "cursos.acesso", "cursos.analisar.solicitacoes")))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioBNaoDeveBaixarArquivo100DaSolicitacaoDoUsuarioA() throws Exception {
        inserirArquivo(100L, "programacao.pdf", MediaType.APPLICATION_PDF_VALUE, pdfBytes());
        inserirSolicitacao(10L, 501L, 100L);

        mockMvc.perform(get("/api/arquivos/100")
                        .with(usuario(502L, "usuario.b", "cursos.acesso")))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioADonoDaSolicitacaoDeveBaixarArquivo100() throws Exception {
        inserirArquivo(100L, "programacao.pdf", MediaType.APPLICATION_PDF_VALUE, pdfBytes());
        inserirSolicitacao(10L, 501L, 100L);

        mockMvc.perform(get("/api/arquivos/100")
                        .with(usuario(501L, "usuario.a", "cursos.acesso")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pdfBytes()));
    }

    @Test
    void administradorDeCursosDeveBaixarProgramacaoDeSolicitacaoDeOutroUsuario() throws Exception {
        inserirArquivo(100L, "programacao.pdf", MediaType.APPLICATION_PDF_VALUE, pdfBytes());
        inserirSolicitacao(10L, 501L, 100L);

        mockMvc.perform(get("/api/arquivos/100")
                        .with(usuario(999L, "admin", "cursos.manter.cursos")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pdfBytes()));
    }

    @Test
    void donoDaSolicitacaoBaixaFotoDeMinistranteDaPropriaSolicitacao() throws Exception {
        inserirArquivo(100L, "foto.png", MediaType.IMAGE_PNG_VALUE, pngBytes());
        inserirArquivo(101L, "curriculo.pdf", MediaType.APPLICATION_PDF_VALUE, pdfBytes());
        inserirSolicitacao(10L, 501L, null);
        inserirSolicitacaoMinistrante(10L, 100L, 101L);

        mockMvc.perform(get("/api/arquivos/100")
                        .with(usuario(501L, "usuario.a", "cursos.acesso")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pngBytes()));
    }

    @Test
    void administradorBaixaFotoDeMinistranteDaSolicitacao() throws Exception {
        inserirArquivo(100L, "foto.png", MediaType.IMAGE_PNG_VALUE, pngBytes());
        inserirArquivo(101L, "curriculo.pdf", MediaType.APPLICATION_PDF_VALUE, pdfBytes());
        inserirSolicitacao(10L, 501L, null);
        inserirSolicitacaoMinistrante(10L, 100L, 101L);

        mockMvc.perform(get("/api/arquivos/100")
                        .with(usuario(999L, "admin", "cursos.manter.cursos")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pngBytes()));
    }

    @Test
    void administradorBaixaCurriculoDeMinistranteDaSolicitacao() throws Exception {
        inserirArquivo(100L, "foto.png", MediaType.IMAGE_PNG_VALUE, pngBytes());
        inserirArquivo(101L, "curriculo.pdf", MediaType.APPLICATION_PDF_VALUE, pdfBytes());
        inserirSolicitacao(10L, 501L, null);
        inserirSolicitacaoMinistrante(10L, 100L, 101L);

        mockMvc.perform(get("/api/arquivos/101")
                        .with(usuario(999L, "admin", "cursos.manter.cursos")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pdfBytes()));
    }

    @Test
    void solicitanteAcessaCurriculoDeMinistranteDaPropriaSolicitacao() throws Exception {
        inserirArquivo(100L, "foto.png", MediaType.IMAGE_PNG_VALUE, pngBytes());
        inserirArquivo(101L, "curriculo.pdf", MediaType.APPLICATION_PDF_VALUE, pdfBytes());
        inserirSolicitacao(10L, 501L, null);
        inserirSolicitacaoMinistrante(10L, 100L, 101L);

        mockMvc.perform(get("/api/arquivos/101")
                        .with(usuario(501L, "usuario.a", "cursos.acesso")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pdfBytes()));
    }

    @Test
    void outroUsuarioNaoBaixaCurriculoDeMinistranteDaSolicitacaoDoUsuarioA() throws Exception {
        inserirArquivo(100L, "foto.png", MediaType.IMAGE_PNG_VALUE, pngBytes());
        inserirArquivo(101L, "curriculo.pdf", MediaType.APPLICATION_PDF_VALUE, pdfBytes());
        inserirSolicitacao(10L, 501L, null);
        inserirSolicitacaoMinistrante(10L, 100L, 101L);

        mockMvc.perform(get("/api/arquivos/101")
                        .with(usuario(502L, "usuario.b", "cursos.acesso")))
                .andExpect(status().isForbidden());
    }

    @Test
    void outroUsuarioNaoBaixaFotoDeMinistranteDaSolicitacaoDoUsuarioA() throws Exception {
        inserirArquivo(100L, "foto.png", MediaType.IMAGE_PNG_VALUE, pngBytes());
        inserirArquivo(101L, "curriculo.pdf", MediaType.APPLICATION_PDF_VALUE, pdfBytes());
        inserirSolicitacao(10L, 501L, null);
        inserirSolicitacaoMinistrante(10L, 100L, 101L);

        mockMvc.perform(get("/api/arquivos/100")
                        .with(usuario(502L, "usuario.b", "cursos.acesso")))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioComCursosAcessoNaoBaixaArquivoDeMinistranteCadastrado() throws Exception {
        inserirArquivo(100L, "curriculo-global.pdf", MediaType.APPLICATION_PDF_VALUE, pdfBytes());
        inserirMinistrante(20L, 100L, null);

        mockMvc.perform(get("/api/arquivos/100")
                        .with(usuario(501L, "usuario.a", "cursos.acesso")))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorBaixaArquivoDeMinistranteCadastrado() throws Exception {
        inserirArquivo(100L, "curriculo-global.pdf", MediaType.APPLICATION_PDF_VALUE, pdfBytes());
        inserirMinistrante(20L, 100L, null);

        mockMvc.perform(get("/api/arquivos/100")
                        .with(usuario(999L, "admin", "cursos.manter.cursos")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pdfBytes()));
    }

    @Test
    void usuarioComCursosAcessoNaoBaixaImagemCabecalhoPrivadaPeloEndpointPrivado() throws Exception {
        inserirArquivo(100L, "cabecalho.png", MediaType.IMAGE_PNG_VALUE, pngBytes());
        inserirCurso(10L, "C", 100L);

        mockMvc.perform(get("/api/arquivos/100")
                        .with(usuario(501L, "usuario.a", "cursos.acesso")))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorBaixaImagemCabecalhoPrivadaPeloEndpointPrivado() throws Exception {
        inserirArquivo(100L, "cabecalho.png", MediaType.IMAGE_PNG_VALUE, pngBytes());
        inserirCurso(10L, "C", 100L);

        mockMvc.perform(get("/api/arquivos/100")
                        .with(usuario(999L, "admin", "cursos.manter.cursos")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pngBytes()));
    }

    @Test
    void usuarioComCursosAcessoNaoBaixaTemplateCertificado() throws Exception {
        inserirArquivo(100L, "template.png", MediaType.IMAGE_PNG_VALUE, pngBytes());
        inserirCurso(10L, "F", null);
        vincularTemplateCertificado(10L, 100L);

        mockMvc.perform(get("/api/arquivos/100")
                        .with(usuario(501L, "usuario.a", "cursos.acesso")))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorBaixaTemplateCertificado() throws Exception {
        inserirArquivo(100L, "template.png", MediaType.IMAGE_PNG_VALUE, pngBytes());
        inserirCurso(10L, "F", null);
        vincularTemplateCertificado(10L, 100L);

        mockMvc.perform(get("/api/arquivos/100")
                        .with(usuario(999L, "admin", "cursos.manter.cursos")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pngBytes()));
    }

    @Test
    void participanteAutenticadoNaoBaixaTemplateCertificado() throws Exception {
        inserirArquivo(100L, "template.png", MediaType.IMAGE_PNG_VALUE, pngBytes());
        inserirCurso(10L, "F", null);
        vincularTemplateCertificado(10L, 100L);

        mockMvc.perform(get("/api/arquivos/100")
                        .with(usuario(700L, "participante", "cursos.acesso")))
                .andExpect(status().isForbidden());
    }

    @Test
    void semAutenticacaoNaoBaixaTemplateCertificado() throws Exception {
        mockMvc.perform(get("/api/arquivos/500"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void arquivoComVinculoAmbiguoRetorna403MesmoParaAdministrador() throws Exception {
        inserirArquivo(100L, "arquivo-ambiguo.png", MediaType.IMAGE_PNG_VALUE, pngBytes());
        inserirCurso(10L, "P", 100L);
        vincularTemplateCertificado(10L, 100L);

        mockMvc.perform(get("/api/arquivos/100")
                        .with(usuario(999L, "admin", "cursos.manter.cursos")))
                .andExpect(status().isForbidden());
    }

    @Test
    void alteracaoManualDoIdNaoPermiteDownloadDeArquivoDeOutroUsuario() throws Exception {
        inserirArquivo(100L, "arquivo-a.pdf", MediaType.APPLICATION_PDF_VALUE, pdfBytes());
        inserirArquivo(101L, "arquivo-b.pdf", MediaType.APPLICATION_PDF_VALUE, pdfBytes());
        inserirSolicitacao(10L, 501L, 100L);
        inserirSolicitacao(11L, 502L, 101L);

        mockMvc.perform(get("/api/arquivos/100")
                        .with(usuario(502L, "usuario.b", "cursos.acesso")))
                .andExpect(status().isForbidden());
    }

    @Test
    void arquivoSemVinculoRetorna403() throws Exception {
        inserirArquivo(100L, "solto.pdf", MediaType.APPLICATION_PDF_VALUE, pdfBytes());

        mockMvc.perform(get("/api/arquivos/100")
                        .with(usuario(501L, "usuario.a", "cursos.acesso")))
                .andExpect(status().isForbidden());
    }

    @Test
    void arquivoInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/arquivos/404")
                        .with(usuario(501L, "usuario.a", "cursos.acesso")))
                .andExpect(status().isNotFound());
    }

    @Test
    void imagemCabecalhoDeCursoPublicadoRetorna200NoEndpointPublico() throws Exception {
        inserirArquivo(100L, "cabecalho.png", MediaType.IMAGE_PNG_VALUE, pngBytes());
        inserirCurso(10L, "P", 100L);

        mockMvc.perform(get("/api/public/arquivos/100"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pngBytes()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void imagemDeCursoNaoPublicadoNaoRetornaNoEndpointPublico() throws Exception {
        inserirArquivo(100L, "cabecalho.png", MediaType.IMAGE_PNG_VALUE, pngBytes());
        inserirCurso(10L, "C", 100L);

        mockMvc.perform(get("/api/public/arquivos/100"))
                .andExpect(status().isForbidden());
    }

    @Test
    void arquivoPrivadoNaoRetornaNoEndpointPublico() throws Exception {
        inserirArquivo(100L, "programacao.pdf", MediaType.APPLICATION_PDF_VALUE, pdfBytes());
        inserirSolicitacao(10L, 501L, 100L);

        mockMvc.perform(get("/api/public/arquivos/100"))
                .andExpect(status().isForbidden());
    }

    @Test
    void fotoDeMinistranteNaoRetornaNoEndpointPublico() throws Exception {
        inserirArquivo(100L, "foto.png", MediaType.IMAGE_PNG_VALUE, pngBytes());
        inserirArquivo(101L, "curriculo.pdf", MediaType.APPLICATION_PDF_VALUE, pdfBytes());
        inserirSolicitacao(10L, 501L, null);
        inserirSolicitacaoMinistrante(10L, 100L, 101L);

        mockMvc.perform(get("/api/public/arquivos/100"))
                .andExpect(status().isForbidden());
    }

    @Test
    void curriculoDeMinistranteNaoRetornaNoEndpointPublico() throws Exception {
        inserirArquivo(100L, "foto.png", MediaType.IMAGE_PNG_VALUE, pngBytes());
        inserirArquivo(101L, "curriculo.pdf", MediaType.APPLICATION_PDF_VALUE, pdfBytes());
        inserirSolicitacao(10L, 501L, null);
        inserirSolicitacaoMinistrante(10L, 100L, 101L);

        mockMvc.perform(get("/api/public/arquivos/101"))
                .andExpect(status().isForbidden());
    }

    @Test
    void templateDeCursoPublicadoNaoRetornaNoEndpointPublico() throws Exception {
        inserirArquivo(100L, "cabecalho.png", MediaType.IMAGE_PNG_VALUE, pngBytes());
        inserirArquivo(101L, "template.png", MediaType.IMAGE_PNG_VALUE, pngBytes());
        inserirCurso(10L, "P", 100L);
        vincularTemplateCertificado(10L, 101L);

        mockMvc.perform(get("/api/public/arquivos/101"))
                .andExpect(status().isForbidden());
    }

    @Test
    void arquivoPublicoInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/public/arquivos/404"))
                .andExpect(status().isNotFound());
    }

    @Test
    void idSequencialConhecidoNaoContornaRegraDePublicacao() throws Exception {
        inserirArquivo(100L, "cabecalho-publicado.png", MediaType.IMAGE_PNG_VALUE, pngBytes());
        inserirArquivo(101L, "cabecalho-rascunho.png", MediaType.IMAGE_PNG_VALUE, pngBytes());
        inserirCurso(10L, "P", 100L);
        inserirCurso(11L, "C", 101L);

        mockMvc.perform(get("/api/public/arquivos/101"))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadSemAutenticacaoNaoEnviaArquivo() throws Exception {
        mockMvc.perform(multipart("/api/arquivos/upload")
                        .file(pdfMultipart("arquivo.pdf")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadSemAuthorityNaoEnviaArquivo() throws Exception {
        mockMvc.perform(multipart("/api/arquivos/upload")
                        .file(pdfMultipart("arquivo.pdf"))
                        .with(user("sem.permissao")))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioAutorizadoEnviaArquivoValido() throws Exception {
        mockMvc.perform(multipart("/api/arquivos/upload")
                        .file(pdfMultipart("arquivo.pdf"))
                        .with(usuario(501L, "usuario.a", "cursos.acesso")))
                .andExpect(status().isOk());

        org.assertj.core.api.Assertions.assertThat(arquivoRepository.count()).isEqualTo(1L);
    }

    @Test
    void arquivoInvalidoERejeitadoAntesDaPersistencia() throws Exception {
        MockMultipartFile invalido = new MockMultipartFile(
                "file",
                "arquivo.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "MZ".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/arquivos/upload")
                        .file(invalido)
                        .with(usuario(501L, "usuario.a", "cursos.acesso")))
                .andExpect(status().isUnprocessableEntity());

        org.assertj.core.api.Assertions.assertThat(arquivoRepository.count()).isZero();
    }

    @Test
    void uploadComprobatorioValidoCriaVinculoCorretoComCurso() throws Exception {
        inserirCurso(10L, "F", null);

        mockMvc.perform(multipart("/api/cursos/10/arquivos-comprobatorios")
                        .file(pdfMultipart("comprovante.pdf"))
                        .with(usuario(999L, "admin", "cursos.manter.cursos")))
                .andExpect(status().isCreated());

        Integer vinculos = jdbcTemplate.queryForObject(
                "select count(*) from curso.cursoarquivocomprobatorio where id_curso = 10",
                Integer.class);
        org.assertj.core.api.Assertions.assertThat(vinculos).isEqualTo(1);
    }

    @Test
    void usuarioSemCeafNaoAssociaArquivoComprobatorioAoCurso() throws Exception {
        inserirCurso(10L, "F", null);

        mockMvc.perform(multipart("/api/cursos/10/arquivos-comprobatorios")
                        .file(pdfMultipart("comprovante.pdf"))
                        .with(usuario(501L, "usuario.a", "cursos.acesso")))
                .andExpect(status().isForbidden());

        Integer vinculos = jdbcTemplate.queryForObject(
                "select count(*) from curso.cursoarquivocomprobatorio",
                Integer.class);
        org.assertj.core.api.Assertions.assertThat(vinculos).isZero();
    }

    @Test
    void gerarDocumentoSemAutenticacaoRetorna401() throws Exception {
        mockMvc.perform(post("/api/cursos/10/documentos/gerar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void gerarDocumentoComTokenInvalidoRetorna401() throws Exception {
        FeignException feignException = mock(FeignException.class);
        when(authServiceClient.consultaAutorizacao(any())).thenThrow(feignException);

        mockMvc.perform(post("/api/cursos/10/documentos/gerar")
                        .header(HttpHeaders.AUTHORIZATION, "Token token-invalido")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void gerarDocumentoSemAuthorityRetorna403() throws Exception {
        mockMvc.perform(post("/api/cursos/10/documentos/gerar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(user("sem.permissao")))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioComCursosAcessoNaoGeraDocumentoAdministrativo() throws Exception {
        mockMvc.perform(post("/api/cursos/10/documentos/gerar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(usuario(501L, "usuario.a", "cursos.acesso")))
                .andExpect(status().isForbidden());
    }

    @Test
    void authorityInexistenteNaoGeraDocumentoAdministrativo() throws Exception {
        mockMvc.perform(post("/api/cursos/10/documentos/gerar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(usuario(501L, "usuario.a", "cursos.arquivo.baixar")))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorGeraDocumentoAdministrativo() throws Exception {
        when(documentosService.gerarDocumento(anyLong(), any())).thenReturn(pdfBytes());

        mockMvc.perform(post("/api/cursos/10/documentos/gerar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(usuario(999L, "admin", "cursos.manter.cursos")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pdfBytes()));
    }

    @Test
    void gerarDocumentoParaCursoInexistenteRetorna404() throws Exception {
        when(documentosService.gerarDocumento(anyLong(), any()))
                .thenThrow(new NotFoundException("Curso não encontrado"));

        mockMvc.perform(post("/api/cursos/404/documentos/gerar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(usuario(999L, "admin", "cursos.manter.cursos")))
                .andExpect(status().isNotFound());
    }

    @Test
    void manualAdministrativoSemAutenticacaoRetorna401() throws Exception {
        mockMvc.perform(get("/api/manuais/pdf/administrativo"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void manualAdministrativoComTokenNaQueryRetorna401() throws Exception {
        mockMvc.perform(get("/api/manuais/pdf/administrativo?token=valor-de-teste"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void manualAdministrativoComTokenInvalidoRetorna401() throws Exception {
        FeignException feignException = mock(FeignException.class);
        when(authServiceClient.consultaAutorizacao(any())).thenThrow(feignException);

        mockMvc.perform(get("/api/manuais/pdf/administrativo")
                        .header(HttpHeaders.AUTHORIZATION, "Token valor-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void manualAdministrativoComHeaderTokenSemAuthorityRetorna403() throws Exception {
        when(authServiceClient.consultaAutorizacao(any())).thenReturn(authResponse(
                501L,
                "usuario",
                "cursos.acesso"));

        mockMvc.perform(get("/api/manuais/pdf/administrativo")
                        .header(HttpHeaders.AUTHORIZATION, "Token valor-sem-authority"))
                .andExpect(status().isForbidden());
    }

    @Test
    void manualAdministrativoComHeaderTokenAutorizadoRetorna200() throws Exception {
        when(authServiceClient.consultaAutorizacao(any())).thenReturn(authResponse(
                999L,
                "admin",
                "cursos.manter.cursos"));
        when(manualPdfService.gerarPdfManual("administrativo")).thenReturn(pdfBytes());

        mockMvc.perform(get("/api/manuais/pdf/administrativo")
                        .header(HttpHeaders.AUTHORIZATION, "Token valor-ceaf"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pdfBytes()));
    }

    private void inserirArquivo(Long id, String nome, String mimeType, byte[] bytes) {
        jdbcTemplate.update("""
                insert into ged.arquivo (id, nomearquivo, mimetype, tamanho, bytes, dataregistro)
                values (?, ?, ?, ?, ?, now())
                """, id, nome, mimeType, bytes.length, bytes);
    }

    private void inserirCurso(Long id, String status, Long headerImageId) {
        jdbcTemplate.update("""
                insert into curso.curso (id, titulo, status, id_imagemcabecalho, hashinscricao, datamodificacao)
                values (?, ?, ?, ?, ?, now())
                """, id, "Curso " + id, status, headerImageId, UUID.randomUUID());
    }

    private void vincularTemplateCertificado(Long cursoId, Long arquivoId) {
        jdbcTemplate.update("""
                update curso.curso
                set id_arquivocertificado = ?
                where id = ?
                """, arquivoId, cursoId);
    }

    private void inserirSolicitacao(Long id, Long idServidorInteressado, Long arquivoId) {
        jdbcTemplate.update("""
                insert into curso.solicitacaocurso (
                    id, datacriacao, datamodificacao, uuid_referencia, id_requerente,
                    id_orgao, nomecurso, estimativaparticipantes, id_arquivo
                )
                values (?, now(), now(), ?, ?, 16, ?, 10, ?)
                """, id, UUID.randomUUID(), idServidorInteressado, "Solicitação " + id, arquivoId);
    }

    private void inserirArquivoComprobatorio(Long cursoId, Long arquivoId) {
        jdbcTemplate.update("""
                insert into curso.cursoarquivocomprobatorio (id_curso, id_arquivo, dataregistro)
                values (?, ?, now())
                """, cursoId, arquivoId);
    }

    private void inserirMinistrante(Long id, Long curriculoId, Long fotoId) {
        jdbcTemplate.update("""
                insert into curso.ministrante (id, nome, tipo, id_curriculo, id_foto)
                values (?, ?, 'E', ?, ?)
                """, id, "Ministrante " + id, curriculoId, fotoId);
    }

    private void inserirSolicitacaoMinistrante(Long solicitacaoId, Long fotoId, Long curriculoId) {
        jdbcTemplate.update("""
                insert into curso.solicitacaoministrante (
                    id_solicitacao, nomeministrante, id_foto, id_curriculo
                )
                values (?, ?, ?, ?)
                """, solicitacaoId, "Ministrante Solicitado", fotoId, curriculoId);
    }

    private MockMultipartFile pdfMultipart(String nome) {
        return new MockMultipartFile("file", nome, MediaType.APPLICATION_PDF_VALUE, pdfBytes());
    }

    private byte[] pdfBytes() {
        return "%PDF-1.4\nconteudo".getBytes(StandardCharsets.UTF_8);
    }

    private byte[] pngBytes() {
        return new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
    }

    private ConsultaAutorizacaoResponseDto authResponse(
            Long id,
            String login,
            String... authorities) {
        List<FuncionalidadeAutorizadaResponseDto> permissoes = List.of(authorities).stream()
                .map(authority -> new FuncionalidadeAutorizadaResponseDto(null, authority, authority))
                .toList();
        AcessoDataResponseDto acessoData = new AcessoDataResponseDto(null, permissoes, null, null);
        UsuarioAutorizadoResponseDto usuario = new UsuarioAutorizadoResponseDto(id, login, login);
        return new ConsultaAutorizacaoResponseDto(acessoData, usuario);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor usuario(
            Long id,
            String login,
            String... authorities) {
        List<FuncionalidadeAutorizadaResponseDto> permissoes = List.of(authorities).stream()
                .map(authority -> new FuncionalidadeAutorizadaResponseDto(null, authority, authority))
                .toList();
        AcessoDataResponseDto acessoData = new AcessoDataResponseDto(null, permissoes, null, null);
        UsuarioAutorizadoResponseDto usuario = new UsuarioAutorizadoResponseDto(id, login, login);
        List<SimpleGrantedAuthority> grantedAuthorities = List.of(authorities).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        return authentication(new TokenAuthentication("test-token", usuario, acessoData, null, grantedAuthorities));
    }
}
