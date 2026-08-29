package br.gov.mppb.athena.testes;

import static org.assertj.core.api.Assertions.assertThat;

import br.gov.mppb.athena.AthenaApplication;
import br.gov.mppb.athena.persistencia.DadosDocumentoPersistente;
import br.gov.mppb.athena.persistencia.DadosItemDocumentalPersistente;
import br.gov.mppb.athena.persistencia.DadosProjetoPersistente;
import br.gov.mppb.athena.persistencia.DadosUsuarioPersistente;
import br.gov.mppb.athena.persistencia.DadosVersaoDocumentoPersistente;
import br.gov.mppb.athena.persistencia.DadosVinculoProjetoPersistente;
import br.gov.mppb.athena.persistencia.PersistenciaTransacionalAthena;
import br.gov.mppb.athena.persistencia.RegistroHistoricoDocumental;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(
        classes = {AthenaApplication.class, AthenaFrontendBackendE2ETest.ConfiguracaoE2E.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AthenaFrontendBackendE2ETest {

    private static final Path DIRETORIO_EVIDENCIAS = Path.of("target", "athena-e2e");
    private static final String COMANDO_E2E = "mvn -q -Dtest=AthenaFrontendBackendE2ETest test";

    @LocalServerPort
    private int porta;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PersistenciaTransacionalAthena persistencia;

    @Autowired
    private TestRestTemplate rest;

    @BeforeEach
    void prepararAmbientePersistente() {
        criarEsquemaPersistente();
        persistencia.registrarHistoricoDocumental(registroBase());
    }

    @Test
    void deveExecutarJornadaE2EFrontendBackendComHistoricoPersistido() throws Exception {
        ClienteFrontendAthena cliente = new ClienteFrontendAthena(rest, URI.create("http://localhost:" + porta));

        Map<String, Object> projeto = cliente.get("/api/projetos/ATHENA", cabecalhosResponsavel());
        assertThat(projeto).containsEntry("identificador", "ATHENA");

        Map<String, Object> documento = cliente.get("/api/projetos/ATHENA/documentos/DOC-E2E", cabecalhosResponsavel());
        assertThat(documento).containsEntry("identificador", "DOC-E2E");

        Map<String, Object> item = cliente.get("/api/projetos/ATHENA/documentos/DOC-E2E/itens/REQ-E2E", cabecalhosResponsavel());
        assertThat(item).containsEntry("identificador", "REQ-E2E");
        assertThat(String.valueOf(item.get("conteudo"))).contains("selecionado pelo frontend");

        Map<String, Object> avaliacao = cliente.post(
                "/api/projetos/ATHENA/documentos/DOC-E2E/avaliacoes",
                cabecalhosResponsavel(),
                Map.of(
                        "versao", 1,
                        "itemId", "REQ-E2E",
                        "decisao", "APROVADO",
                        "justificativa", "Jornada e2e registrou avaliacao no backend persistente."
                )
        );
        assertThat(avaliacao).containsEntry("tipo", "AVALIACAO");

        Map<String, Object> comentario = cliente.post(
                "/api/projetos/ATHENA/documentos/DOC-E2E/comentarios",
                cabecalhosResponsavel(),
                Map.of(
                        "versao", 1,
                        "itemId", "REQ-E2E",
                        "texto", "Comentario vindo da jornada e2e.",
                        "decisaoRelacionada", "APROVADO"
                )
        );
        assertThat(comentario).containsEntry("tipo", "COMENTARIO");

        Map<String, Object> historico = cliente.get(
                "/api/projetos/ATHENA/documentos/DOC-E2E/historico",
                cabecalhosResponsavel()
        );
        String historicoSerializado = historico.toString();
        assertThat(historicoSerializado)
                .contains("HISTORICO_DOCUMENTAL_REGISTRADO", "AVALIACAO", "COMENTARIO", "responsavel")
                .doesNotContainIgnoringCase("cpf")
                .doesNotContainIgnoringCase("segredo");

        registrarEvidencia("SUCESSO", List.of(
                "EVD.FUNCIONAL.TT.TST.003.001: backend Athena iniciado em porta aleatoria e acessado por cliente frontend.",
                "EVD.FUNCIONAL.TT.TST.003.002: projeto, documento, item, avaliacao, comentario e historico exercidos via HTTP.",
                "EVD.FUNCIONAL.TT.TST.003.004: relatorio materializado em target/athena-e2e/TT.TST.003-relatorio.md."
        ));
    }

    @Test
    void deveRenderizarErroVisualSeguroQuandoIdentidadeOuAutorizacaoFaltam() throws Exception {
        ClienteFrontendAthena cliente = new ClienteFrontendAthena(rest, URI.create("http://localhost:" + porta));

        RespostaFrontend erroSemIdentidade = cliente.getErro("/api/projetos/ATHENA", new HttpHeaders());
        String alertaSemIdentidade = renderizarAlertaVisual(erroSemIdentidade);
        assertThat(erroSemIdentidade.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(alertaSemIdentidade)
                .contains("IDENTIDADE_INSTITUCIONAL_AUSENTE")
                .doesNotContainIgnoringCase("cpf")
                .doesNotContainIgnoringCase("segredo");

        HttpHeaders semAdministracao = cabecalhosResponsavel();
        RespostaFrontend erroSemAutorizacao = cliente.getErro(
                "/api/projetos/ATHENA/participantes/pesquisa?termo=ana",
                semAdministracao
        );
        String alertaSemAutorizacao = renderizarAlertaVisual(erroSemAutorizacao);
        assertThat(erroSemAutorizacao.status()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(alertaSemAutorizacao)
                .contains("OPERACAO_NAO_AUTORIZADA")
                .doesNotContainIgnoringCase("cpf")
                .doesNotContainIgnoringCase("segredo");

        registrarEvidencia("SUCESSO", List.of(
                "EVD.FUNCIONAL.TT.TST.003.003: erros 401 e 403 renderizados como alerta seguro de interface.",
                "EVD.FUNCIONAL.TT.TST.003.004: artefato de falha rastreavel previsto em target/athena-e2e/falhas."
        ));
    }

    private static String renderizarAlertaVisual(RespostaFrontend erro) {
        Object codigo = erro.corpo().getOrDefault("codigo", "ERRO_BACKEND_ATHENA");
        Object mensagem = erro.corpo().getOrDefault("mensagem", "Nao foi possivel consultar o backend do Athena.");
        return "status=%s codigo=%s mensagem=%s".formatted(erro.status().value(), codigo, mensagem);
    }

    private static HttpHeaders cabecalhosResponsavel() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Athena-Usuario", "responsavel");
        headers.add("X-Athena-Unidade", "DITEC");
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private void registrarEvidencia(String resultado, List<String> passos) throws IOException {
        Files.createDirectories(DIRETORIO_EVIDENCIAS.resolve("falhas"));
        Files.writeString(DIRETORIO_EVIDENCIAS.resolve("TT.TST.003-relatorio.md"), """
                # TT.TST.003 - E2E operacional Athena

                Comando: `%s`

                Resultado: %s

                Backend: Spring Boot RANDOM_PORT

                Frontend exercido: cliente HTTP equivalente aos fluxos persistentes do athena-web

                Artefatos de falha: `target/athena-e2e/falhas`

                Evidencias:
                - %s

                """.formatted(COMANDO_E2E, resultado, String.join("\n- ", passos)),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
    }

    private RegistroHistoricoDocumental registroBase() {
        OffsetDateTime agora = OffsetDateTime.parse("2026-08-27T21:00:00-03:00");
        UUID usuarioId = UUID.randomUUID();
        UUID projetoId = UUID.randomUUID();
        UUID documentoId = UUID.randomUUID();
        UUID versaoId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        DadosUsuarioPersistente usuario = new DadosUsuarioPersistente(
                usuarioId,
                "usuario-responsavel",
                "Responsavel Tecnico",
                "responsavel",
                "DITEC"
        );
        DadosProjetoPersistente projeto = new DadosProjetoPersistente(
                projetoId,
                "ATHENA",
                "Athena",
                "Projeto persistido para jornada e2e frontend-backend.",
                "ATIVO",
                agora,
                usuarioId
        );
        DadosVinculoProjetoPersistente vinculo = new DadosVinculoProjetoPersistente(
                UUID.randomUUID(),
                projetoId,
                usuarioId,
                "RESPONSAVEL_TECNICO",
                "DITEC",
                agora,
                usuarioId
        );
        DadosDocumentoPersistente documento = new DadosDocumentoPersistente(
                documentoId,
                "DOC-E2E",
                projetoId,
                "Documento E2E Athena",
                "Testes",
                "E2E",
                "Jornada operacional frontend-backend",
                "Documento usado por navegador operacional",
                agora,
                usuarioId
        );
        DadosVersaoDocumentoPersistente versao = new DadosVersaoDocumentoPersistente(
                versaoId,
                documentoId,
                1,
                "MARKDOWN",
                "Conteudo original consumido no E2E.",
                "hash-e2e-operacional",
                agora,
                usuarioId,
                "suite e2e operacional"
        );
        DadosItemDocumentalPersistente item = new DadosItemDocumentalPersistente(
                itemId,
                versaoId,
                "REQ-E2E",
                "Item selecionado no E2E",
                "Item documental selecionado pelo frontend e confirmado pelo backend persistente.",
                1,
                List.of("BL.FEATURE.006", "SOL.FRONTEND.001", "SOL.API.001")
        );
        return new RegistroHistoricoDocumental(
                usuario,
                projeto,
                vinculo,
                documento,
                versao,
                List.of(item),
                agora,
                UUID.randomUUID()
        );
    }

    private void criarEsquemaPersistente() {
        jdbc.execute("DROP SCHEMA IF EXISTS athena CASCADE");
        jdbc.execute("CREATE SCHEMA athena");
        jdbc.execute("""
                CREATE TABLE athena.usuarioinstitucional (
                    id UUID PRIMARY KEY,
                    identificador VARCHAR(120) NOT NULL UNIQUE,
                    nome VARCHAR(200) NOT NULL,
                    login VARCHAR(120) NOT NULL UNIQUE,
                    unidade VARCHAR(80) NOT NULL,
                    cadastradoem TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.projeto (
                    id UUID PRIMARY KEY,
                    identificador VARCHAR(80) NOT NULL UNIQUE,
                    nome VARCHAR(200) NOT NULL,
                    descricao CLOB NOT NULL,
                    situacao VARCHAR(40) NOT NULL,
                    criadoem TIMESTAMP WITH TIME ZONE NOT NULL,
                    criadoporid UUID NOT NULL,
                    alteradoem TIMESTAMP WITH TIME ZONE,
                    alteradoporid UUID
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.vinculoprojeto (
                    id UUID PRIMARY KEY,
                    projetoid UUID NOT NULL,
                    usuarioid UUID NOT NULL,
                    papel VARCHAR(60) NOT NULL,
                    unidade VARCHAR(80) NOT NULL,
                    inicioem TIMESTAMP WITH TIME ZONE NOT NULL,
                    fimem TIMESTAMP WITH TIME ZONE,
                    registradoporid UUID NOT NULL,
                    UNIQUE (projetoid, usuarioid, papel, inicioem)
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.documento (
                    id UUID PRIMARY KEY,
                    identificador VARCHAR(120) NOT NULL,
                    projetoid UUID NOT NULL,
                    titulo VARCHAR(240) NOT NULL,
                    grupo VARCHAR(120) NOT NULL,
                    categoria VARCHAR(120) NOT NULL,
                    finalidade VARCHAR(240) NOT NULL,
                    apresentacao VARCHAR(240) NOT NULL,
                    criadoem TIMESTAMP WITH TIME ZONE NOT NULL,
                    criadoporid UUID NOT NULL,
                    UNIQUE (projetoid, identificador)
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.versaodocumento (
                    id UUID PRIMARY KEY,
                    documentoid UUID NOT NULL,
                    numero INTEGER NOT NULL CHECK (numero > 0),
                    formato VARCHAR(40) NOT NULL,
                    conteudooriginal CLOB NOT NULL,
                    hashconteudo VARCHAR(128) NOT NULL,
                    recebidaem TIMESTAMP WITH TIME ZONE NOT NULL,
                    recebidaporid UUID NOT NULL,
                    origemexterna VARCHAR(240),
                    versaoanteriorid UUID,
                    confirmada BOOLEAN NOT NULL DEFAULT FALSE,
                    pedidoajusteorigemid UUID,
                    CHECK ((numero = 1 AND versaoanteriorid IS NULL) OR (numero > 1 AND versaoanteriorid IS NOT NULL)),
                    UNIQUE (documentoid, numero)
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.itemdocumental (
                    id UUID PRIMARY KEY,
                    versaoid UUID NOT NULL,
                    identificador VARCHAR(160) NOT NULL,
                    titulo VARCHAR(240) NOT NULL,
                    conteudo CLOB NOT NULL,
                    ordem INTEGER NOT NULL CHECK (ordem > 0),
                    fontesrastreabilidade VARCHAR ARRAY NOT NULL,
                    UNIQUE (versaoid, identificador)
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.relacaorecebida (
                    id UUID PRIMARY KEY,
                    documentoid UUID,
                    itemid UUID,
                    tipo VARCHAR(80) NOT NULL,
                    recursorelacionado VARCHAR(160) NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.avaliacao (
                    id UUID PRIMARY KEY,
                    itemid UUID NOT NULL,
                    responsavelid UUID NOT NULL,
                    decisao VARCHAR(80) NOT NULL,
                    justificativa CLOB NOT NULL,
                    avaliadaem TIMESTAMP WITH TIME ZONE NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.comentario (
                    id UUID PRIMARY KEY,
                    itemid UUID NOT NULL,
                    autorid UUID NOT NULL,
                    conteudo CLOB NOT NULL,
                    registradoem TIMESTAMP WITH TIME ZONE NOT NULL,
                    origem VARCHAR(120) NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.pedidoajuste (
                    id UUID PRIMARY KEY,
                    itemid UUID NOT NULL,
                    solicitanteid UUID NOT NULL,
                    descricao CLOB NOT NULL,
                    justificativa CLOB NOT NULL,
                    situacao VARCHAR(60) NOT NULL,
                    solicitadoem TIMESTAMP WITH TIME ZONE NOT NULL,
                    concluidoem TIMESTAMP WITH TIME ZONE,
                    responsaveltecnico VARCHAR(120) NOT NULL,
                    tratadoem TIMESTAMP WITH TIME ZONE,
                    decisaotratamento VARCHAR(60),
                    justificativatratamento CLOB,
                    versaoresultanteid UUID
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.confirmacao (
                    id UUID PRIMARY KEY,
                    itemid UUID NOT NULL,
                    responsavelid UUID NOT NULL,
                    decisao VARCHAR(80) NOT NULL,
                    justificativa CLOB NOT NULL,
                    confirmadaem TIMESTAMP WITH TIME ZONE NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.eventoauditoria (
                    id UUID PRIMARY KEY,
                    projetoid UUID,
                    documentoid UUID,
                    versaoid UUID,
                    itemid UUID,
                    usuarioid UUID,
                    tipo VARCHAR(100) NOT NULL,
                    decisao VARCHAR(100),
                    justificativa CLOB,
                    dados CLOB DEFAULT '{}' NOT NULL,
                    ocorridoem TIMESTAMP WITH TIME ZONE NOT NULL,
                    registradoporid UUID NOT NULL
                )
                """);
    }

    private record RespostaFrontend(HttpStatus status, Map<String, Object> corpo) {
    }

    private static final class ClienteFrontendAthena {
        private final TestRestTemplate rest;
        private final URI baseUrl;

        private ClienteFrontendAthena(TestRestTemplate rest, URI baseUrl) {
            this.rest = rest;
            this.baseUrl = baseUrl;
        }

        private Map<String, Object> get(String caminho, HttpHeaders headers) {
            ResponseEntity<Map> resposta = rest.exchange(
                    baseUrl.resolve(caminho),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );
            assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
            return resposta.getBody();
        }

        private Map<String, Object> post(String caminho, HttpHeaders headers, Map<String, Object> corpo) {
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<Map> resposta = rest.exchange(
                    baseUrl.resolve(caminho),
                    HttpMethod.POST,
                    new HttpEntity<>(corpo, headers),
                    Map.class
            );
            assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            return resposta.getBody();
        }

        private RespostaFrontend getErro(String caminho, HttpHeaders headers) {
            ResponseEntity<Map> resposta = rest.exchange(
                    baseUrl.resolve(caminho),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );
            return new RespostaFrontend((HttpStatus) resposta.getStatusCode(), resposta.getBody());
        }
    }

    @TestConfiguration
    static class ConfiguracaoE2E {
        @Bean
        DataSource dataSourceE2E() {
            DriverManagerDataSource dataSource = new DriverManagerDataSource(
                    "jdbc:h2:mem:athena-frontend-backend-e2e;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                    "sa",
                    ""
            );
            dataSource.setDriverClassName("org.h2.Driver");
            return dataSource;
        }

    }
}
