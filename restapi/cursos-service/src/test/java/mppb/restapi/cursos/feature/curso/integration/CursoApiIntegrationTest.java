package mppb.restapi.cursos.feature.curso.integration;

import feign.FeignException;
import mppb.restapi.common.infrastructure.client.authapi.AuthServiceClient;
import mppb.restapi.common.infrastructure.client.authapi.dto.AcessoDataResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.FuncionalidadeAutorizadaResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.UsuarioAutorizadoResponseDto;
import mppb.restapi.common.infrastructure.client.mpvirtual.MPVirtualClient;
import mppb.restapi.common.infrastructure.security.authorization.TokenAuthentication;
import mppb.restapi.cursos.CursosApiApplication;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
class CursoApiIntegrationTest {

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
                  curso.frequencia,
                  curso.chamada,
                  curso.inscricaoconfirmacaotoken,
                  curso.inscricao,
                  curso.participante,
                  curso.aula,
                  curso.cursoarquivocomprobatorio,
                  curso.cursoministrante,
                  curso.cursopublicoalvo,
                  curso.ministrante,
                  curso.curso,
                  ged.arquivo,
                  curso.papelministrante,
                  curso.publicoalvo,
                  curso.cursomodalidade,
                  curso.tipoevento,
                  curso.entidadetutora,
                  autorizacao.usuario
                restart identity cascade
                """);
        inserirCadastrosBasicos();
    }

    @Test
    void listarCursosSemAutenticacaoRetorna401() throws Exception {
        mockMvc.perform(get("/api/cursos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listarCursosComTokenInvalidoRetorna401() throws Exception {
        FeignException feignException = mock(FeignException.class);
        when(authServiceClient.consultaAutorizacao(any())).thenThrow(feignException);

        mockMvc.perform(get("/api/cursos")
                        .header(HttpHeaders.AUTHORIZATION, "Token token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioComApenasCursosAcessoNaoListaCursosAdministrativos() throws Exception {
        mockMvc.perform(get("/api/cursos")
                        .with(usuario(501L, "usuario.a", "cursos.acesso")))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarCursosComPaginacaoFiltroOrdenacaoEStatusValido() throws Exception {
        inserirCurso(101L, "Curso Alpha", "C");
        inserirCurso(102L, "Curso Beta", "P");
        inserirCurso(103L, "Outra Formacao", "F");

        mockMvc.perform(get("/api/cursos")
                        .param("titulo", "Curso")
                        .param("status", "PUBLICADO")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sort", "titulo,asc")
                        .with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].titulo").value("Curso Beta"))
                .andExpect(jsonPath("$.content[0].status").value("PUBLICADO"));
    }

    @Test
    void filtroTituloComSqlInjectionNaoRetornaTodosOsCursos() throws Exception {
        inserirCurso(101L, "Curso Seguro", "P");
        inserirCurso(102L, "Curso Protegido", "P");

        mockMvc.perform(get("/api/cursos")
                        .param("titulo", "' OR '1'='1")
                        .with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void listarCursosComPaginaOuTamanhoInvalidosRetorna400() throws Exception {
        mockMvc.perform(get("/api/cursos")
                        .param("page", "-1")
                        .param("size", "0")
                        .with(admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void consultarCursoExistenteRetornaDetalhesDosTresStatus() throws Exception {
        inserirCurso(101L, "Curso em Construcao", "C");
        inserirCurso(102L, "Curso Publicado", "P");
        inserirCurso(103L, "Curso Finalizado", "F");

        mockMvc.perform(get("/api/cursos/101").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_CONSTRUCAO"));
        mockMvc.perform(get("/api/cursos/102").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLICADO"));
        mockMvc.perform(get("/api/cursos/103").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADO"));
    }

    @Test
    void consultarCursoInexistenteRetorna404SemDetalhesInternos() throws Exception {
        mockMvc.perform(get("/api/cursos/999").with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("select"))))
                .andExpect(content().string(not(containsString("SQLException"))));
    }

    @Test
    void criarCursoValidoRetorna201ComStatusInicialEHashGerado() throws Exception {
        mockMvc.perform(post("/api/cursos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cursoPayload(null, "Curso Novo", null))
                        .with(admin()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Curso Novo"))
                .andExpect(jsonPath("$.status").value("EM_CONSTRUCAO"))
                .andExpect(jsonPath("$.hashInscricao").isNotEmpty());

        Integer cursos = jdbcTemplate.queryForObject("select count(*) from curso.curso", Integer.class);
        assertThat(cursos).isEqualTo(1);
    }

    @Test
    void criarCursoComTituloAusenteRetorna400() throws Exception {
        mockMvc.perform(post("/api/cursos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cursoPayload(null, "", null))
                        .with(admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void criarCursoComCargaHorariaOuPercentualInvalidosRetorna400() throws Exception {
        mockMvc.perform(post("/api/cursos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Curso Invalido",
                                  "tipoEvento": {"id": 1},
                                  "entidadeTutora": {"id": 1},
                                  "modalidade": {"id": 1},
                                  "publicosAlvo": [{"id": 1}],
                                  "cargaHoraria": "8 horas",
                                  "percentualChAprovacao": 101
                                }
                                """)
                        .with(admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void criarCursoSemPublicoAlvoRetorna400() throws Exception {
        mockMvc.perform(post("/api/cursos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Curso Sem Publico",
                                  "tipoEvento": {"id": 1},
                                  "entidadeTutora": {"id": 1},
                                  "modalidade": {"id": 1},
                                  "publicosAlvo": []
                                }
                                """)
                        .with(admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void criarCursoComCampoDesconhecidoRetorna400() throws Exception {
        mockMvc.perform(post("/api/cursos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cursoPayload(null, "Curso Campo Extra", "\"campoDesconhecido\": true,"))
                        .with(admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void criarCursoComIdStatusHashOuDatasInternasNoPayloadRetorna400() throws Exception {
        mockMvc.perform(post("/api/cursos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cursoPayload(999L, "Curso Mass Assignment", """
                                "status": "FINALIZADO",
                                "hashInscricao": "11111111-1111-1111-1111-111111111111",
                                "dataCriacao": "2020-01-01 00:00:00",
                                """))
                        .with(admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void atualizarCursoValidoPreservaStatusHashEDataCriacao() throws Exception {
        inserirCurso(101L, "Curso Antigo", "P");
        String hashAntes = jdbcTemplate.queryForObject(
                "select hashinscricao::text from curso.curso where id = 101",
                String.class);
        String dataCriacaoAntes = jdbcTemplate.queryForObject(
                "select datacriacao::text from curso.curso where id = 101",
                String.class);

        mockMvc.perform(put("/api/cursos/101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cursoPayload(101L, "Curso Atualizado", """
                                "status": "FINALIZADO",
                                "hashInscricao": "22222222-2222-2222-2222-222222222222",
                                "dataCriacao": "2020-01-01 00:00:00",
                                """))
                        .with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Curso Atualizado"));

        String status = jdbcTemplate.queryForObject("select status from curso.curso where id = 101", String.class);
        String hashDepois = jdbcTemplate.queryForObject("select hashinscricao::text from curso.curso where id = 101", String.class);
        String dataCriacaoDepois = jdbcTemplate.queryForObject(
                "select datacriacao::text from curso.curso where id = 101",
                String.class);
        assertThat(status).isEqualTo("P");
        assertThat(hashDepois).isEqualTo(hashAntes);
        assertThat(dataCriacaoDepois).isEqualTo(dataCriacaoAntes);
    }

    @Test
    void atualizarCursoInexistenteRetorna404() throws Exception {
        mockMvc.perform(put("/api/cursos/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cursoPayload(999L, "Curso Inexistente", null))
                        .with(admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void atualizarCursoComIdDivergenteRetorna422ENaoAlteraBanco() throws Exception {
        inserirCurso(101L, "Curso Original", "P");

        mockMvc.perform(put("/api/cursos/101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cursoPayload(202L, "Curso Indevido", null))
                        .with(admin()))
                .andExpect(status().isUnprocessableEntity());

        String titulo = jdbcTemplate.queryForObject("select titulo from curso.curso where id = 101", String.class);
        assertThat(titulo).isEqualTo("Curso Original");
    }

    @Test
    void removerCursoValidoRetorna204() throws Exception {
        inserirCurso(101L, "Curso Removivel", "C");

        mockMvc.perform(delete("/api/cursos/101").with(admin()))
                .andExpect(status().isNoContent());

        Integer cursos = jdbcTemplate.queryForObject("select count(*) from curso.curso where id = 101", Integer.class);
        assertThat(cursos).isZero();
    }

    @Test
    void removerCursoInexistenteRetorna404() throws Exception {
        mockMvc.perform(delete("/api/cursos/999").with(admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void removerCursoComDependenciasFalhaSemRemocaoParcial() throws Exception {
        inserirCurso(101L, "Curso Com Inscricao", "P");
        inserirAula(301L, 101L);
        inserirParticipante(401L);
        inserirInscricao(501L, 101L, 401L);

        mockMvc.perform(delete("/api/cursos/101").with(admin()))
                .andExpect(status().isConflict())
                .andExpect(content().string(not(containsString("cpf"))))
                .andExpect(content().string(not(containsString("token"))))
                .andExpect(content().string(not(containsString("curso.inscricao"))))
                .andExpect(content().string(not(containsString("constraint"))));

        Integer cursos = jdbcTemplate.queryForObject("select count(*) from curso.curso where id = 101", Integer.class);
        Integer inscricoes = jdbcTemplate.queryForObject("select count(*) from curso.inscricao where id_curso = 101", Integer.class);
        assertThat(cursos).isEqualTo(1);
        assertThat(inscricoes).isEqualTo(1);
    }

    @Test
    void listarArquivosComprobatoriosRetornaVinculosDoCurso() throws Exception {
        inserirCurso(101L, "Curso Finalizado", "F");
        inserirArquivo(201L, "comprovante.pdf", MediaType.APPLICATION_PDF_VALUE, pdfBytes());
        inserirArquivoComprobatorio(301L, 101L, 201L);

        mockMvc.perform(get("/api/cursos/101/arquivos-comprobatorios").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(301))
                .andExpect(jsonPath("$[0].arquivoId").value(201))
                .andExpect(jsonPath("$[0].nomeArquivo").value("comprovante.pdf"));
    }

    @Test
    void uploadComprobatorioValidoEmCursoFinalizadoCriaArquivoEVinculo() throws Exception {
        inserirCurso(101L, "Curso Finalizado", "F");

        mockMvc.perform(multipart("/api/cursos/101/arquivos-comprobatorios")
                        .file(pdfMultipart("comprovante.pdf"))
                        .with(admin()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.arquivoId").isNumber());

        Integer arquivos = jdbcTemplate.queryForObject("select count(*) from ged.arquivo", Integer.class);
        Integer vinculos = jdbcTemplate.queryForObject(
                "select count(*) from curso.cursoarquivocomprobatorio where id_curso = 101",
                Integer.class);
        assertThat(arquivos).isEqualTo(1);
        assertThat(vinculos).isEqualTo(1);
    }

    @Test
    void uploadComprobatorioRejeitaCursoInexistenteNaoFinalizadoVazioOuTipoInvalido() throws Exception {
        inserirCurso(101L, "Curso Aberto", "P");

        mockMvc.perform(multipart("/api/cursos/999/arquivos-comprobatorios")
                        .file(pdfMultipart("comprovante.pdf"))
                        .with(admin()))
                .andExpect(status().isNotFound());
        mockMvc.perform(multipart("/api/cursos/101/arquivos-comprobatorios")
                        .file(pdfMultipart("comprovante.pdf"))
                        .with(admin()))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(multipart("/api/cursos/101/arquivos-comprobatorios")
                        .file(new MockMultipartFile("file", "vazio.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[0]))
                        .with(admin()))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(multipart("/api/cursos/101/arquivos-comprobatorios")
                        .file(new MockMultipartFile("file", "shell.pdf", "application/x-msdownload", "MZ".getBytes(StandardCharsets.UTF_8)))
                        .with(admin()))
                .andExpect(status().isUnprocessableEntity());

        Integer arquivos = jdbcTemplate.queryForObject("select count(*) from ged.arquivo", Integer.class);
        Integer vinculos = jdbcTemplate.queryForObject("select count(*) from curso.cursoarquivocomprobatorio", Integer.class);
        assertThat(arquivos).isZero();
        assertThat(vinculos).isZero();
    }

    @Test
    void removerArquivoComprobatorioRemoveVinculoEArquivo() throws Exception {
        inserirCurso(101L, "Curso Finalizado", "F");
        inserirArquivo(201L, "comprovante.pdf", MediaType.APPLICATION_PDF_VALUE, pdfBytes());
        inserirArquivoComprobatorio(301L, 101L, 201L);

        mockMvc.perform(delete("/api/cursos/101/arquivos-comprobatorios/301").with(admin()))
                .andExpect(status().isNoContent());

        Integer arquivos = jdbcTemplate.queryForObject("select count(*) from ged.arquivo where id = 201", Integer.class);
        Integer vinculos = jdbcTemplate.queryForObject("select count(*) from curso.cursoarquivocomprobatorio where id = 301", Integer.class);
        assertThat(arquivos).isZero();
        assertThat(vinculos).isZero();
    }

    private void inserirCadastrosBasicos() {
        jdbcTemplate.update("""
                insert into autorizacao.usuario (id, login, nome, ativo, excluido, dataregistro)
                values (1, 'teste', 'Usuario Teste', true, false, now())
                """);
        jdbcTemplate.update("insert into curso.tipoevento (id, descricao) values (1, 'Curso')");
        jdbcTemplate.update("insert into curso.cursomodalidade (id, descricao) values (1, 'Presencial')");
        jdbcTemplate.update("""
                insert into curso.entidadetutora (id, nome, id_usuarioregistro, dataregistro)
                values (1, 'CEAF', 1, now())
                """);
        jdbcTemplate.update("insert into curso.publicoalvo (id, nome, internoexterno) values (1, 'Membros e servidores', 'I')");
        jdbcTemplate.update("insert into curso.papelministrante (id, nome) values (1, 'Instrutor')");
    }

    private void inserirCurso(Long id, String titulo, String status) {
        jdbcTemplate.update("""
                insert into curso.curso (
                    id, titulo, descricao, id_tipoevento, id_entidadetutora, id_modalidade,
                    inscricaodatainicial, inscricaodatafinal, realizacaodatainicial, realizacaodatafinal,
                    cargahoraria, percentualchaprovacao, status, hashinscricao, datacriacao, datamodificacao
                )
                values (?, ?, 'Descricao', 1, 1, 1, now(), now() + interval '1 day',
                        now() + interval '2 days', now() + interval '3 days', '08:00:00', 75, ?, ?, now(), now())
                """, id, titulo, status, UUID.randomUUID());
        jdbcTemplate.update("insert into curso.cursopublicoalvo (id_curso, id_publicoalvo) values (?, 1)", id);
    }

    private void inserirAula(Long id, Long cursoId) {
        jdbcTemplate.update("""
                insert into curso.aula (id, id_curso, descricao, data, horainicio, horafim, quantidadechamadas)
                values (?, ?, 'Aula', current_date, '08:00:00', '10:00:00', 2)
                """, id, cursoId);
    }

    private void inserirParticipante(Long id) {
        jdbcTemplate.update("""
                insert into curso.participante (id, nome, cpf, email, telefone, tipo)
                values (?, 'Participante', '52998224725', 'participante@example.org', '83999999999', 'E')
                """, id);
    }

    private void inserirInscricao(Long id, Long cursoId, Long participanteId) {
        jdbcTemplate.update("""
                insert into curso.inscricao (id, id_curso, id_participante, datainscricao, situacao, pcd)
                values (?, ?, ?, now(), 'C', false)
                """, id, cursoId, participanteId);
    }

    private void inserirArquivo(Long id, String nome, String mimeType, byte[] bytes) {
        jdbcTemplate.update("""
                insert into ged.arquivo (id, nomearquivo, mimetype, tamanho, bytes, dataregistro)
                values (?, ?, ?, ?, ?, now())
                """, id, nome, mimeType, bytes.length, bytes);
    }

    private void inserirArquivoComprobatorio(Long id, Long cursoId, Long arquivoId) {
        jdbcTemplate.update("""
                insert into curso.cursoarquivocomprobatorio (id, id_curso, id_arquivo, dataregistro)
                values (?, ?, ?, now())
                """, id, cursoId, arquivoId);
    }

    private String cursoPayload(Long id, String titulo, String extraFields) {
        String idField = id == null ? "" : "\"id\": " + id + ",";
        String extras = extraFields == null ? "" : extraFields;
        return """
                {
                  %s
                  %s
                  "titulo": "%s",
                  "descricao": "Descricao segura",
                  "tipoEvento": {"id": 1},
                  "entidadeTutora": {"id": 1},
                  "modalidade": {"id": 1},
                  "publicosAlvo": [{"id": 1}],
                  "inscricaoDataInicial": "2026-08-01 08:00:00",
                  "inscricaoDataFinal": "2026-08-02 18:00:00",
                  "realizacaoDataInicial": "2026-08-03 08:00:00",
                  "realizacaoDataFinal": "2026-08-04 18:00:00",
                  "cargaHoraria": "08:00:00",
                  "percentualChAprovacao": 75
                }
                """.formatted(idField, extras, titulo);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
        return usuario(999L, "admin", "cursos.manter.cursos");
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

    private ConsultaAutorizacaoResponseDto authResponse(Long id, String login, String... authorities) {
        List<FuncionalidadeAutorizadaResponseDto> permissoes = List.of(authorities).stream()
                .map(authority -> new FuncionalidadeAutorizadaResponseDto(null, authority, authority))
                .toList();
        AcessoDataResponseDto acessoData = new AcessoDataResponseDto(null, permissoes, null, null);
        UsuarioAutorizadoResponseDto usuario = new UsuarioAutorizadoResponseDto(id, login, login);
        return new ConsultaAutorizacaoResponseDto(acessoData, usuario);
    }

    private MockMultipartFile pdfMultipart(String nome) {
        return new MockMultipartFile("file", nome, MediaType.APPLICATION_PDF_VALUE, pdfBytes());
    }

    private byte[] pdfBytes() {
        return "%PDF-1.4\nconteudo".getBytes(StandardCharsets.UTF_8);
    }
}
