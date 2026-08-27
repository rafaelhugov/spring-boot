package br.gov.mppb.athena.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gov.mppb.athena.dominio.Documento;
import br.gov.mppb.athena.dominio.FormatoDocumento;
import br.gov.mppb.athena.dominio.IdentificadorProjeto;
import br.gov.mppb.athena.dominio.ItemDocumental;
import br.gov.mppb.athena.dominio.MetadadosDocumento;
import br.gov.mppb.athena.dominio.PermissaoDocumento;
import br.gov.mppb.athena.dominio.RastreabilidadeDocumento;
import br.gov.mppb.athena.dominio.RelacaoDocumental;
import br.gov.mppb.athena.dominio.VersaoDocumento;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ServicoIngestaoDocumentalExternaTest {

    private final ServicoIngestaoDocumentalExterna servico = new ServicoIngestaoDocumentalExterna();

    @Test
    void deveReceberContratoComConteudoOriginalRepresentacaoInternaMetadadosRelacoesPermissoesERastreabilidade() {
        PedidoIngestaoDocumentoExterno pedido = pedido(FormatoDocumento.YAML, "linha-base.md");

        Documento documento = servico.registrarPrimeiraVersao(pedido);

        assertThat(documento.identificador()).isEqualTo("DOC-ESPEC-001");
        assertThat(documento.projeto()).isEqualTo(new IdentificadorProjeto("ATHENA"));
        assertThat(documento.metadados().grupo()).isEqualTo("Especificacao");
        assertThat(documento.metadados().categoria()).isEqualTo("Linha de base");
        assertThat(documento.metadados().finalidade()).isEqualTo("Consulta institucional");
        assertThat(documento.metadados().apresentacao()).isEqualTo("Arvore documental");
        assertThat(documento.metadados().permissoes()).containsExactlyInAnyOrder(
                PermissaoDocumento.CONSULTAR,
                PermissaoDocumento.AVALIAR
        );
        assertThat(documento.metadados().relacoesRecebidas())
                .containsExactly(new RelacaoDocumental("rastreia", "BL.AC.004"));
        assertThat(documento.metadados().rastreabilidade().fontes())
                .containsExactlyInAnyOrder("BL.AC.004", "SOL.INTEGRACAO.001");
        assertThat(documento.versoes()).singleElement().satisfies(versao -> {
            assertThat(versao.formato()).isEqualTo(FormatoDocumento.YAML);
            assertThat(versao.conteudoOriginal()).isEqualTo("conteudo original em YAML");
            assertThat(versao.itens()).containsExactly(itemRepresentacaoInterna());
        });
    }

    @Test
    void deveRegistrarNovaVersaoSemSobrescreverHistoricoAnterior() {
        Documento primeiraVersao = servico.registrarPrimeiraVersao(pedido(FormatoDocumento.MARKDOWN, "documento.md"));

        Documento documentoVersionado = servico.registrarNovaVersao(
                primeiraVersao,
                pedido(FormatoDocumento.JSON, "documento.json", "conteudo original em JSON")
        );

        assertThat(primeiraVersao.versoes()).singleElement()
                .extracting(VersaoDocumento::numero, VersaoDocumento::conteudoOriginal)
                .containsExactly(1, "conteudo original em MARKDOWN");
        assertThat(documentoVersionado.versoes()).hasSize(2);
        assertThat(documentoVersionado.versoes().get(0).conteudoOriginal()).isEqualTo("conteudo original em MARKDOWN");
        assertThat(documentoVersionado.versoes().get(1)).satisfies(novaVersao -> {
            assertThat(novaVersao.numero()).isEqualTo(2);
            assertThat(novaVersao.formato()).isEqualTo(FormatoDocumento.JSON);
            assertThat(novaVersao.conteudoOriginal()).isEqualTo("conteudo original em JSON");
        });
    }

    @Test
    void deveSuportarMarkdownJsonEYamlPorFormatoExplicitoSemInferirPeloNomeFisicoDoArquivo() {
        assertThat(servico.registrarPrimeiraVersao(pedido(FormatoDocumento.MARKDOWN, "entrada.yaml"))
                .versoes().get(0).formato()).isEqualTo(FormatoDocumento.MARKDOWN);
        assertThat(servico.registrarPrimeiraVersao(pedido(FormatoDocumento.JSON, "entrada.md"))
                .versoes().get(0).formato()).isEqualTo(FormatoDocumento.JSON);
        assertThat(servico.registrarPrimeiraVersao(pedido(FormatoDocumento.YAML, "entrada.json"))
                .versoes().get(0).formato()).isEqualTo(FormatoDocumento.YAML);
    }

    @Test
    void deveRejeitarPedidoSemMetadadosOuRepresentacaoInternaExplicitos() {
        assertThatThrownBy(() -> new PedidoIngestaoDocumentoExterno(
                "DOC-ESPEC-001",
                new IdentificadorProjeto("ATHENA"),
                "Linha de base de especificacao",
                null,
                FormatoDocumento.YAML,
                "conteudo original",
                List.of(itemRepresentacaoInterna()),
                OffsetDateTime.parse("2026-08-25T10:20:30-03:00"),
                "linha-base.yaml"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("metadados do documento devem ser informados.");

        assertThatThrownBy(() -> new PedidoIngestaoDocumentoExterno(
                "DOC-ESPEC-001",
                new IdentificadorProjeto("ATHENA"),
                "Linha de base de especificacao",
                metadados(),
                FormatoDocumento.YAML,
                "conteudo original",
                List.of(),
                OffsetDateTime.parse("2026-08-25T10:20:30-03:00"),
                "linha-base.yaml"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("representacao interna do documento deve ser informada.");
    }

    private static PedidoIngestaoDocumentoExterno pedido(FormatoDocumento formato, String nomeFisicoArquivo) {
        return pedido(formato, nomeFisicoArquivo, "conteudo original em " + formato.name());
    }

    private static PedidoIngestaoDocumentoExterno pedido(
            FormatoDocumento formato,
            String nomeFisicoArquivo,
            String conteudoOriginal
    ) {
        return new PedidoIngestaoDocumentoExterno(
                "DOC-ESPEC-001",
                new IdentificadorProjeto("ATHENA"),
                "Linha de base de especificacao",
                metadados(),
                formato,
                conteudoOriginal,
                List.of(itemRepresentacaoInterna()),
                OffsetDateTime.parse("2026-08-25T10:20:30-03:00"),
                nomeFisicoArquivo
        );
    }

    private static MetadadosDocumento metadados() {
        return new MetadadosDocumento(
                "Especificacao",
                "Linha de base",
                "Consulta institucional",
                "Arvore documental",
                Set.of(PermissaoDocumento.CONSULTAR, PermissaoDocumento.AVALIAR),
                Set.of(new RelacaoDocumental("rastreia", "BL.AC.004")),
                new RastreabilidadeDocumento(Set.of("BL.AC.004", "SOL.INTEGRACAO.001"))
        );
    }

    private static ItemDocumental itemRepresentacaoInterna() {
        return new ItemDocumental(
                "SOL.INTEGRACAO.001",
                "Ingestao de documentos externos",
                "Representacao interna para exibicao.",
                Set.of(new RelacaoDocumental("rastreia", "BL.AC.004")),
                new RastreabilidadeDocumento(Set.of("SOL.INTEGRACAO.001"))
        );
    }
}
