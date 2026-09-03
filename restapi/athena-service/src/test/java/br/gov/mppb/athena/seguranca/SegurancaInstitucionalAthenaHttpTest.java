package br.gov.mppb.athena.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.gov.mppb.athena.AthenaApplication;
import br.gov.mppb.athena.persistencia.DadosDocumentoPersistente;
import br.gov.mppb.athena.persistencia.DadosItemDocumentalPersistente;
import br.gov.mppb.athena.persistencia.DadosProjetoPersistente;
import br.gov.mppb.athena.persistencia.DadosUsuarioPersistente;
import br.gov.mppb.athena.persistencia.DadosVersaoDocumentoPersistente;
import br.gov.mppb.athena.persistencia.DadosVinculoProjetoPersistente;
import br.gov.mppb.athena.persistencia.PersistenciaTransacionalAthena;
import br.gov.mppb.athena.persistencia.RegistroHistoricoDocumental;
import feign.FeignException;
import feign.Request;
import feign.Response;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import mppb.restapi.common.infrastructure.client.authapi.AuthServiceClient;
import mppb.restapi.common.infrastructure.client.authapi.dto.AcessoDataResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.AutorizaRequestDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.AutorizaResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoRequestDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.FuncionalidadeAutorizadaResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.UsuarioAutorizadoResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(
        classes = {AthenaApplication.class, SegurancaInstitucionalAthenaHttpTest.ConfiguracaoSeguranca.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "cors.enabled=true",
                "cors.access-control-headers.allow-origin=http://localhost:5173,http://127.0.0.1:5173",
                "cors.access-control-headers.allow-credentials=false",
                "spring.profiles.active=dev"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SegurancaInstitucionalAthenaHttpTest {

    @LocalServerPort
    private int porta;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PersistenciaTransacionalAthena persistencia;

    @MockBean
    private AuthServiceClient authServiceClient;

    @BeforeEach
    void prepararDados() {
        prepararAuthApi();
        criarEsquemaPersistente();
        persistencia.registrarHistoricoDocumental(registroBase("responsavel", "DITEC"));
    }

    @Test
    void deveResponder401SemTokenOuComTokenInvalido() {
        assertThat(get("/api/projetos/ATHENA", new HttpHeaders()).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Token token-invalido");
        assertThat(get("/api/projetos/ATHENA", headers).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void deveResponder403ParaUsuarioAutenticadoSemPermissaoOuVinculo() {
        assertThat(get("/api/projetos/ATHENA", token("token-sem-vinculo")).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void devePermitirAcessoParaUsuarioAutenticadoAutorizado() {
        ResponseEntity<Map> resposta = get("/api/projetos/ATHENA", token("token-responsavel"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsEntry("identificador", "ATHENA");
    }

    @Test
    void deveIgnorarHeaderAthenaAutorizacoesForjado() {
        HttpHeaders headers = token("token-responsavel");
        headers.set("X-Athena-Autorizacoes", "ATHENA_ADMINISTRAR");
        headers.set("X-Athena-Unidade", "DITEC");

        assertThat(get("/api/projetos/ATHENA/participantes/pesquisa?termo=ana", headers).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deveAplicarCorsDevSomenteParaOrigemPermitida() {
        HttpHeaders origemPermitida = new HttpHeaders();
        origemPermitida.setOrigin("http://localhost:5173");
        origemPermitida.setAccessControlRequestMethod(HttpMethod.GET);
        ResponseEntity<String> permitido = options("/api/projetos/ATHENA", origemPermitida);

        assertThat(permitido.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(permitido.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:5173");

        HttpHeaders origemNegada = new HttpHeaders();
        origemNegada.setOrigin("http://malicioso.example");
        origemNegada.setAccessControlRequestMethod(HttpMethod.GET);
        ResponseEntity<String> negado = options("/api/projetos/ATHENA", origemNegada);

        assertThat(negado.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private ResponseEntity<Map> get(String caminho, HttpHeaders headers) {
        return rest.exchange("http://localhost:" + porta + caminho, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    }

    private ResponseEntity<String> options(String caminho, HttpHeaders headers) {
        return rest.exchange("http://localhost:" + porta + caminho, HttpMethod.OPTIONS, new HttpEntity<>(headers), String.class);
    }

    private HttpHeaders token(String valor) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Token " + valor);
        return headers;
    }

    private void prepararAuthApi() {
        when(authServiceClient.consultaAutorizacao(any())).thenAnswer(invocation -> {
            ConsultaAutorizacaoRequestDto request = invocation.getArgument(0);
            return switch (request.getToken()) {
                case "token-responsavel" -> respostaAuthApi("responsavel", List.of("athena.ler"));
                case "token-sem-vinculo" -> respostaAuthApi("sem.vinculo", List.of("athena.ler"));
                default -> throw authApiUnauthorized();
            };
        });
    }

    private ConsultaAutorizacaoResponseDto respostaAuthApi(String login, List<String> servicos) {
        return new ConsultaAutorizacaoResponseDto(
                new AcessoDataResponseDto(
                        List.of(),
                        servicos.stream()
                                .map(servico -> new FuncionalidadeAutorizadaResponseDto(1L, servico, servico))
                                .toList(),
                        List.of(),
                        List.of()
                ),
                new UsuarioAutorizadoResponseDto(1L, "Usuario " + login, login)
        );
    }

    private FeignException authApiUnauthorized() {
        return FeignException.errorStatus(
                "consultaAutorizacao",
                Response.builder()
                        .request(Request.create(Request.HttpMethod.POST, "/acesso/consulta-autorizacao",
                                Map.of(), null, StandardCharsets.UTF_8, null))
                        .status(401)
                        .reason("Unauthorized")
                        .build()
        );
    }

    private RegistroHistoricoDocumental registroBase(String login, String unidade) {
        OffsetDateTime agora = OffsetDateTime.parse("2026-08-30T00:30:00-03:00");
        UUID idUsuario = UUID.randomUUID();
        UUID idProjeto = UUID.randomUUID();
        UUID idDocumento = UUID.randomUUID();
        UUID idVersao = UUID.randomUUID();
        UUID idItem = UUID.randomUUID();
        return new RegistroHistoricoDocumental(
                new DadosUsuarioPersistente(idUsuario, "usuario-" + login, "Responsavel Athena", login, unidade),
                new DadosProjetoPersistente(idProjeto, "ATHENA", "Athena", "Projeto Athena", "ATIVO", agora, idUsuario),
                new DadosVinculoProjetoPersistente(UUID.randomUUID(), idProjeto, idUsuario, "RESPONSAVEL_TECNICO", unidade, agora, idUsuario),
                new DadosDocumentoPersistente(idDocumento, "DOC-SEG", idProjeto, "Documento", "SEGURANCA", "TESTE", "Teste", "Teste", agora, idUsuario),
                new DadosVersaoDocumentoPersistente(idVersao, idDocumento, 1, "MARKDOWN", "Conteudo", "hash", agora, idUsuario, "teste", null, false, null),
                List.of(new DadosItemDocumentalPersistente(idItem, idVersao, "ITEM-SEG", "Item", "Conteudo", 1, List.of("SEG"))),
                agora,
                UUID.randomUUID()
        );
    }

    private void criarEsquemaPersistente() {
        Collection<String> tabelas = List.of(
                "eventoauditoria", "confirmacao", "pedidoajuste", "comentario", "avaliacao", "relacaorecebida",
                "itemdocumental", "versaodocumento", "documento", "vinculoprojeto", "projeto", "usuarioinstitucional"
        );
        jdbc.execute("CREATE SCHEMA IF NOT EXISTS athena");
        for (String tabela : tabelas) {
            jdbc.execute("DROP TABLE IF EXISTS athena." + tabela);
        }
        jdbc.execute("""
                CREATE TABLE athena.usuarioinstitucional (
                    id UUID PRIMARY KEY,
                    identificador VARCHAR(120) NOT NULL UNIQUE,
                    nome VARCHAR(200) NOT NULL,
                    login VARCHAR(120) NOT NULL UNIQUE,
                    unidade VARCHAR(80) NOT NULL,
                    datacadastro TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.projeto (
                    id UUID PRIMARY KEY,
                    identificador VARCHAR(80) NOT NULL UNIQUE,
                    nome VARCHAR(200) NOT NULL,
                    descricao CLOB NOT NULL,
                    situacao VARCHAR(40) NOT NULL,
                    datacriacao TIMESTAMP WITH TIME ZONE NOT NULL,
                    id_usuariocriacao UUID NOT NULL,
                    dataalteracao TIMESTAMP WITH TIME ZONE,
                    id_usuarioalteracao UUID
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.vinculoprojeto (
                    id UUID PRIMARY KEY,
                    id_projeto UUID NOT NULL,
                    id_usuario UUID NOT NULL,
                    papel VARCHAR(60) NOT NULL,
                    unidade VARCHAR(80) NOT NULL,
                    datainicio TIMESTAMP WITH TIME ZONE NOT NULL,
                    datafim TIMESTAMP WITH TIME ZONE,
                    id_usuarioregistro UUID NOT NULL,
                    UNIQUE (id_projeto, id_usuario, papel, datainicio)
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.documento (
                    id UUID PRIMARY KEY,
                    identificador VARCHAR(120) NOT NULL,
                    id_projeto UUID NOT NULL,
                    titulo VARCHAR(240) NOT NULL,
                    grupo VARCHAR(120) NOT NULL,
                    categoria VARCHAR(120) NOT NULL,
                    finalidade VARCHAR(240) NOT NULL,
                    apresentacao VARCHAR(240) NOT NULL,
                    datacriacao TIMESTAMP WITH TIME ZONE NOT NULL,
                    id_usuariocriacao UUID NOT NULL,
                    UNIQUE (id_projeto, identificador)
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.versaodocumento (
                    id UUID PRIMARY KEY,
                    id_documento UUID NOT NULL,
                    numero INTEGER NOT NULL CHECK (numero > 0),
                    formato VARCHAR(40) NOT NULL,
                    conteudooriginal CLOB NOT NULL,
                    hashconteudo VARCHAR(128) NOT NULL,
                    datarecebimento TIMESTAMP WITH TIME ZONE NOT NULL,
                    id_usuariorecebimento UUID NOT NULL,
                    origemexterna VARCHAR(240),
                    id_versao_anterior UUID,
                    confirmada BOOLEAN NOT NULL DEFAULT FALSE,
                    id_pedido_ajuste_origem UUID,
                    CHECK ((numero = 1 AND id_versao_anterior IS NULL) OR (numero > 1 AND id_versao_anterior IS NOT NULL)),
                    UNIQUE (id_documento, numero)
                )
                """);
        jdbc.execute("""
                CREATE TABLE athena.itemdocumental (
                    id UUID PRIMARY KEY,
                    id_versao UUID NOT NULL,
                    identificador VARCHAR(160) NOT NULL,
                    titulo VARCHAR(240) NOT NULL,
                    conteudo CLOB NOT NULL,
                    ordem INTEGER NOT NULL CHECK (ordem > 0),
                    fontesrastreabilidade VARCHAR ARRAY NOT NULL,
                    UNIQUE (id_versao, identificador)
                )
                """);
        jdbc.execute("CREATE TABLE athena.relacaorecebida (id UUID PRIMARY KEY, id_documento UUID, id_item UUID, tipo VARCHAR(80) NOT NULL, recursorelacionado VARCHAR(160) NOT NULL)");
        jdbc.execute("CREATE TABLE athena.avaliacao (id UUID PRIMARY KEY, id_item UUID NOT NULL, id_responsavel UUID NOT NULL, decisao VARCHAR(80) NOT NULL, justificativa CLOB NOT NULL, dataavaliacao TIMESTAMP WITH TIME ZONE NOT NULL)");
        jdbc.execute("CREATE TABLE athena.comentario (id UUID PRIMARY KEY, id_item UUID NOT NULL, id_autor UUID NOT NULL, conteudo CLOB NOT NULL, dataregistro TIMESTAMP WITH TIME ZONE NOT NULL, origem VARCHAR(120) NOT NULL)");
        jdbc.execute("CREATE TABLE athena.pedidoajuste (id UUID PRIMARY KEY, id_item UUID NOT NULL, id_solicitante UUID NOT NULL, descricao CLOB NOT NULL, justificativa CLOB NOT NULL, situacao VARCHAR(60) NOT NULL, datasolicitacao TIMESTAMP WITH TIME ZONE NOT NULL, dataconclusao TIMESTAMP WITH TIME ZONE, responsaveltecnico VARCHAR(120) NOT NULL, datatratamento TIMESTAMP WITH TIME ZONE, decisaotratamento VARCHAR(60), justificativatratamento CLOB, id_versao_resultante UUID)");
        jdbc.execute("CREATE TABLE athena.confirmacao (id UUID PRIMARY KEY, id_item UUID NOT NULL, id_responsavel UUID NOT NULL, decisao VARCHAR(80) NOT NULL, justificativa CLOB NOT NULL, dataconfirmacao TIMESTAMP WITH TIME ZONE NOT NULL)");
        jdbc.execute("CREATE TABLE athena.eventoauditoria (id UUID PRIMARY KEY, id_projeto UUID, id_documento UUID, id_versao UUID, id_item UUID, id_usuario UUID, tipo VARCHAR(100) NOT NULL, decisao VARCHAR(100), justificativa CLOB, dados CLOB DEFAULT '{}' NOT NULL, dataocorrencia TIMESTAMP WITH TIME ZONE NOT NULL, id_usuarioregistro UUID NOT NULL)");
    }

    @TestConfiguration
    static class ConfiguracaoSeguranca {
        @Bean
        DataSource dataSourceSeguranca() {
            DriverManagerDataSource dataSource = new DriverManagerDataSource(
                    "jdbc:h2:mem:athena-seguranca;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                    "sa",
                    ""
            );
            dataSource.setDriverClassName("org.h2.Driver");
            return dataSource;
        }

    }
}
