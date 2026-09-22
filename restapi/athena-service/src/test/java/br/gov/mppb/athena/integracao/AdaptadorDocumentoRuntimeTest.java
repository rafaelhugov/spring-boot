package br.gov.mppb.athena.integracao;

import static org.assertj.core.api.Assertions.assertThat;

import br.gov.mppb.athena.dominio.FormatoDocumento;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AdaptadorDocumentoRuntimeTest {

    private static final Path DIRETORIO_ESPECIFICACAO = Path.of(
            "../../../ai-first/projetos/athena/especificacao").toAbsolutePath().normalize();
    private final AdaptadorDocumentoRuntime adaptador = new AdaptadorDocumentoRuntime();

    @Test
    void deveInterpretarEntendimentoCanonicoYamlPreservandoSemanticaGovernada() throws Exception {
        String conteudo = Files.readString(DIRETORIO_ESPECIFICACAO.resolve("entendimento-projeto-v002.yaml"));

        DocumentoRuntimeInterpretado documento = adaptador.interpretar(FormatoDocumento.YAML, conteudo, Map.of());

        assertThat(documento.tipoContrato()).isEqualTo("ENTENDIMENTO_PROJETO");
        assertThat(documento.versaoContrato()).isEqualTo("1.0");
        assertThat(documento.metadados()).containsEntry("versao_documento", 2);
        assertThat(documento.itens()).hasSize(47);
        assertThat(documento.itens().getFirst())
                .extracting(ItemRuntimeInterpretado::identificador, ItemRuntimeInterpretado::categoria,
                        ItemRuntimeInterpretado::estadoConhecimento, ItemRuntimeInterpretado::estadoGovernanca,
                        ItemRuntimeInterpretado::versaoItem)
                .containsExactly("ENT.PROBLEMA.001", "PROBLEMA", "CONFIRMADO", "APROVADO", 1);
        assertThat(documento.itens().getFirst().fontesUpstream()).containsExactly("FONTE.001");
    }

    @Test
    void deveInterpretarApresentacaoMarkdownDoMesmoEntendimento() throws Exception {
        String conteudo = Files.readString(DIRETORIO_ESPECIFICACAO.resolve("entendimento-projeto-v002.md"));

        DocumentoRuntimeInterpretado documento = adaptador.interpretar(FormatoDocumento.MARKDOWN, conteudo, Map.of());

        assertThat(documento.itens()).hasSize(47);
        assertThat(documento.itens().getFirst().identificador()).isEqualTo("ENT.PROBLEMA.001");
        assertThat(documento.itens().getFirst().titulo())
                .isEqualTo("Usuarios dependem de arquivos brutos para compreender projetos");
        assertThat(documento.itens().getFirst().descricao()).contains("Usuarios autorizados precisam consultar");
        assertThat(documento.itens().getFirst().fontesUpstream()).containsExactly("FONTE.001");
    }

    @Test
    void deveAceitarManifestoRuntimeComVersaoHashERastreabilidade() {
        String manifesto = """
                contrato:
                  tipo: MANIFESTO_VALIDACAO_HUMANA
                  versao: '1.0'
                itens:
                - id_item: ENT.OBJETIVO.001
                  categoria: OBJETIVO
                  titulo: Objetivo
                  descricao: Descricao governada
                  versao_item: 2
                  hash_conteudo: abc123
                  origem_upstream:
                  - FONTE.001
                """;

        DocumentoRuntimeInterpretado documento = adaptador.interpretar(FormatoDocumento.YAML, manifesto, Map.of());

        assertThat(documento.itens().getFirst().versaoItem()).isEqualTo(2);
        assertThat(documento.itens().getFirst().hashConteudo()).isEqualTo("abc123");
        assertThat(documento.itens().getFirst().fontesUpstream()).containsExactly("FONTE.001");
    }
}
