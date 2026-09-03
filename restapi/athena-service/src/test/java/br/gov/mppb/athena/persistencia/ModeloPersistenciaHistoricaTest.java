package br.gov.mppb.athena.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class ModeloPersistenciaHistoricaTest {

    private static final Path MIGRACAO_MODELO_HISTORICO = Path.of(
            "src/main/resources/db/migration/V001__modelo_historico_athena.sql"
    );
    private static final Path MIGRACAO_IMUTABILIDADE_VERSAO = Path.of(
            "src/main/resources/db/migration/V002__imutabilidade_versao_documental.sql"
    );

    @Test
    void deveCobrirEntidadesRastreaveisExigidasNoEsquemaAthena() throws IOException {
        String ddl = ddlModeloInicialNormalizado();
        Set<String> tabelas = tabelasCriadas(ddl);

        assertThat(ddl).contains("create schema if not exists athena");
        assertThat(tabelas).containsExactlyInAnyOrder(
                "usuarioinstitucional",
                "projeto",
                "vinculoprojeto",
                "documento",
                "versaodocumento",
                "itemdocumental",
                "avaliacao",
                "comentario",
                "pedidoajuste",
                "confirmacao",
                "relacaorecebida",
                "eventoauditoria"
        );
    }

    @Test
    void devePreservarHistoricoParaReconstrucaoDocumentalEAvaliativa() throws IOException {
        String ddl = ddlModeloInicialNormalizado();
        String ddlImutabilidade = ddlImutabilidadeNormalizado();

        assertThat(ddl)
                .contains("unique (id_documento, numero)")
                .contains("conteudooriginal text not null")
                .contains("hashconteudo varchar(128) not null")
                .contains("fontesrastreabilidade text[] not null")
                .contains("decisao varchar")
                .contains("justificativa text")
                .contains("dataocorrencia timestamptz not null")
                .contains("dados jsonb not null default '{}'::jsonb")
                .doesNotContain("on delete cascade");

        assertThat(ddlImutabilidade)
                .contains("add column id_versao_anterior uuid references athena.versaodocumento(id) on delete restrict")
                .contains("add column confirmada boolean not null default false")
                .contains("add column id_pedido_ajuste_origem uuid")
                .contains("add column id_versao_resultante uuid references athena.versaodocumento(id) on delete restrict")
                .contains("create trigger trgimpediralteracaoversaoconfirmada")
                .contains("before update or delete on athena.versaodocumento")
                .contains("tg_op = 'delete'")
                .contains("old.confirmada is true")
                .contains("return old")
                .contains("new.confirmada is distinct from true")
                .doesNotContain("on delete cascade");
        assertThat(ddl).containsPattern("create table athena\\.eventoauditoria[\\s\\S]*id_usuarioregistro uuid not null");
        assertThat(ddl).containsPattern("create table athena\\.avaliacao[\\s\\S]*id_responsavel uuid not null");
        assertThat(ddl).containsPattern("create table athena\\.confirmacao[\\s\\S]*id_responsavel uuid not null");
        assertThat(ddl).containsPattern("create table athena\\.pedidoajuste[\\s\\S]*id_solicitante uuid not null");
    }

    @Test
    void deveDefinirNomenclaturaPostgreSqlMppbComoRestricaoVerificavel() throws IOException {
        String ddl = ddlModeloInicialNormalizado();
        Set<String> tabelas = tabelasCriadas(ddl);
        Set<String> colunas = colunasCriadas(ddl);

        assertThat(tabelas)
                .isNotEmpty()
                .allSatisfy(identificador -> assertThat(identificador)
                        .matches("[a-z][a-z0-9]*")
                        .doesNotContain("_"));
        assertThat(colunas)
                .isNotEmpty()
                .allSatisfy(coluna -> assertThat(coluna)
                        .matches("[a-z][a-z0-9]*(?:_[a-z][a-z0-9]*)*"));
        assertThat(colunas).contains(
                "id_projeto",
                "id_documento",
                "id_usuario",
                "id_responsavel",
                "id_usuariocriacao",
                "id_usuarioregistro",
                "id_usuariorecebimento",
                "conteudooriginal",
                "hashconteudo",
                "fontesrastreabilidade",
                "datarecebimento",
                "dataavaliacao",
                "dataregistro",
                "datasolicitacao",
                "dataconclusao",
                "dataconfirmacao",
                "dataocorrencia",
                "recursorelacionado",
                "origemexterna"
        );
        assertThat(ddlImutabilidadeNormalizado()).contains(
                "id_versao_anterior",
                "id_pedido_ajuste_origem",
                "id_versao_resultante",
                "responsaveltecnico",
                "datatratamento",
                "decisaotratamento",
                "justificativatratamento"
        );
        assertThat(identificadoresVerificaveis(ddl + "\n" + ddlImutabilidadeNormalizado()))
                .doesNotContain(
                        "idprojeto",
                        "iddocumento",
                        "idusuario",
                        "idresponsavel",
                        "idusuariocriacao",
                        "idusuarioregistro",
                        "idusuariorecebimento",
                        "idversaoanterior",
                        "idpedidoajusteorigem",
                        "idversaoresultante"
                );
    }

    @Test
    void deveNomearChavesEstrangeirasComPrefixoIdConceito() throws IOException {
        String ddl = ddlModeloInicialNormalizado() + "\n" + ddlImutabilidadeNormalizado();

        assertThat(chavesEstrangeiras(ddl)).contains(
                "projeto.id_usuariocriacao->usuarioinstitucional",
                "projeto.id_usuarioalteracao->usuarioinstitucional",
                "vinculoprojeto.id_projeto->projeto",
                "vinculoprojeto.id_usuario->usuarioinstitucional",
                "vinculoprojeto.id_usuarioregistro->usuarioinstitucional",
                "documento.id_projeto->projeto",
                "documento.id_usuariocriacao->usuarioinstitucional",
                "versaodocumento.id_documento->documento",
                "versaodocumento.id_usuariorecebimento->usuarioinstitucional",
                "versaodocumento.id_versao_anterior->versaodocumento",
                "versaodocumento.id_pedido_ajuste_origem->pedidoajuste",
                "itemdocumental.id_versao->versaodocumento",
                "avaliacao.id_item->itemdocumental",
                "avaliacao.id_responsavel->usuarioinstitucional",
                "comentario.id_item->itemdocumental",
                "comentario.id_autor->usuarioinstitucional",
                "pedidoajuste.id_item->itemdocumental",
                "pedidoajuste.id_solicitante->usuarioinstitucional",
                "pedidoajuste.id_versao_resultante->versaodocumento",
                "confirmacao.id_item->itemdocumental",
                "confirmacao.id_responsavel->usuarioinstitucional",
                "relacaorecebida.id_documento->documento",
                "relacaorecebida.id_item->itemdocumental",
                "relacaorecebida.id_usuariorecebimento->usuarioinstitucional",
                "eventoauditoria.id_projeto->projeto",
                "eventoauditoria.id_documento->documento",
                "eventoauditoria.id_versao->versaodocumento",
                "eventoauditoria.id_item->itemdocumental",
                "eventoauditoria.id_usuario->usuarioinstitucional",
                "eventoauditoria.id_usuarioregistro->usuarioinstitucional"
        );
        assertThat(identificadoresVerificaveis(ddl))
                .doesNotContain(
                        "projetoid",
                        "usuarioid",
                        "documentoid",
                        "versaoid",
                        "itemid",
                        "responsavelid",
                        "autorid",
                        "solicitanteid",
                        "usuariocriacaoid",
                        "usuarioalteracaoid",
                        "usuarioregistroid",
                        "usuariorecebimentoid",
                        "versaoanteriorid",
                        "pedidoajusteorigemid",
                        "versaoresultanteid"
                );
    }

    private static String ddlModeloInicialNormalizado() throws IOException {
        return Files.readString(MIGRACAO_MODELO_HISTORICO).toLowerCase(Locale.ROOT);
    }

    private static String ddlImutabilidadeNormalizado() throws IOException {
        return Files.readString(MIGRACAO_IMUTABILIDADE_VERSAO).toLowerCase(Locale.ROOT);
    }

    private static Set<String> tabelasCriadas(String ddl) {
        return Pattern.compile("create table athena\\.([a-z][a-z0-9]*)")
                .matcher(ddl)
                .results()
                .map(resultado -> resultado.group(1))
                .collect(Collectors.toSet());
    }

    private static Set<String> identificadoresVerificaveis(String ddl) {
        Set<String> tabelas = tabelasCriadas(ddl);
        tabelas.addAll(colunasCriadas(ddl));
        return tabelas;
    }

    private static Set<String> colunasCriadas(String ddl) {
        return Pattern.compile("create table athena\\.[a-z][a-z0-9]* \\((.*?)\\);", Pattern.DOTALL)
                .matcher(ddl)
                .results()
                .map(MatchResult::group)
                .flatMap(bloco -> Arrays.stream(bloco.split("\\R")))
                .map(String::strip)
                .filter(linha -> linha.matches("[a-z][a-z0-9_]*\\s+.*"))
                .map(linha -> linha.split("\\s+")[0])
                .collect(Collectors.toSet());
    }

    private static Set<String> chavesEstrangeiras(String ddl) {
        Set<String> chaves = Pattern.compile("create table athena\\.([a-z][a-z0-9]*) \\((.*?)\\);", Pattern.DOTALL)
                .matcher(ddl)
                .results()
                .flatMap(tabela -> Pattern.compile("^\\s*([a-z][a-z0-9_]*)\\s+uuid(?:\\s+[^,]*)?references athena\\.([a-z][a-z0-9]*)\\(id\\)", Pattern.MULTILINE)
                        .matcher(tabela.group(2))
                        .results()
                        .map(coluna -> tabela.group(1) + "." + coluna.group(1) + "->" + coluna.group(2)))
                .collect(Collectors.toSet());
        chaves.addAll(Pattern.compile("alter table athena\\.([a-z][a-z0-9]*)\\s+([\\s\\S]*?);")
                .matcher(ddl)
                .results()
                .flatMap(tabela -> Pattern.compile("add column ([a-z][a-z0-9_]*) uuid references athena\\.([a-z][a-z0-9]*)\\(id\\)")
                        .matcher(tabela.group(2))
                        .results()
                        .map(coluna -> tabela.group(1) + "." + coluna.group(1) + "->" + coluna.group(2)))
                .collect(Collectors.toSet()));
        chaves.addAll(Pattern.compile("alter table athena\\.([a-z][a-z0-9]*)[\\s\\S]*?foreign key \\(([a-z][a-z0-9_]*)\\) references athena\\.([a-z][a-z0-9]*)\\(id\\)")
                .matcher(ddl)
                .results()
                .map(coluna -> coluna.group(1) + "." + coluna.group(2) + "->" + coluna.group(3))
                .collect(Collectors.toSet()));
        return chaves;
    }
}
