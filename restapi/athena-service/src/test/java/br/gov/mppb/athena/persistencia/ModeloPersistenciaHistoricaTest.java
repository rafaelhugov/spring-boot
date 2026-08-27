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

    @Test
    void deveCobrirEntidadesRastreaveisExigidasNoEsquemaAthena() throws IOException {
        String ddl = ddlNormalizado();
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
        String ddl = ddlNormalizado();

        assertThat(ddl)
                .contains("unique (documentoid, numero)")
                .contains("conteudooriginal text not null")
                .contains("hashconteudo varchar(128) not null")
                .contains("fontesrastreabilidade text[] not null")
                .contains("decisao varchar")
                .contains("justificativa text")
                .contains("ocorridoem timestamptz not null")
                .contains("dados jsonb not null default '{}'::jsonb")
                .doesNotContain("on delete cascade");

        assertThat(ddl).containsPattern("create table athena\\.eventoauditoria[\\s\\S]*registradoporid uuid not null");
        assertThat(ddl).containsPattern("create table athena\\.avaliacao[\\s\\S]*responsavelid uuid not null");
        assertThat(ddl).containsPattern("create table athena\\.confirmacao[\\s\\S]*responsavelid uuid not null");
        assertThat(ddl).containsPattern("create table athena\\.pedidoajuste[\\s\\S]*solicitanteid uuid not null");
    }

    @Test
    void deveDefinirNomenclaturaPostgreSqlMppbComoRestricaoVerificavel() throws IOException {
        String ddl = ddlNormalizado();
        Set<String> identificadores = identificadoresVerificaveis(ddl);

        assertThat(identificadores)
                .isNotEmpty()
                .allSatisfy(identificador -> assertThat(identificador)
                        .matches("[a-z][a-z0-9]*")
                        .doesNotContain("_"));
    }

    private static String ddlNormalizado() throws IOException {
        return Files.readString(MIGRACAO_MODELO_HISTORICO).toLowerCase(Locale.ROOT);
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
        Set<String> colunas = Pattern.compile("create table athena\\.[a-z][a-z0-9]* \\((.*?)\\);", Pattern.DOTALL)
                .matcher(ddl)
                .results()
                .map(MatchResult::group)
                .flatMap(bloco -> Arrays.stream(bloco.split("\\R")))
                .map(String::strip)
                .filter(linha -> linha.matches("[a-z][a-z0-9]*\\s+.*"))
                .map(linha -> linha.split("\\s+")[0])
                .collect(Collectors.toSet());

        tabelas.addAll(colunas);
        return tabelas;
    }
}
