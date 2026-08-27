package br.gov.mppb.athena.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ModeloDominioMultiprojetoDocumentalTest {

    @Test
    void deveDefinirConceitosCentraisComVinculosEInvariantesFuncionais() {
        UsuarioInstitucional criador = usuario();
        ProjetoInstitucional projeto = new ProjetoInstitucional(
                new IdentificadorProjeto("ATHENA"),
                "Athena",
                "Plataforma institucional multiprojeto documental",
                Set.of("DITEC"),
                Set.of(new ParticipanteProjeto(criador, PapelProjeto.RESPONSAVEL_TECNICO)),
                SituacaoProjeto.ATIVO,
                OffsetDateTime.parse("2026-08-25T10:15:30-03:00"),
                criador
        );
        MetadadosDocumento metadados = metadados(Set.of(new RelacaoDocumental("origem", "BL.AC.004")));
        ItemDocumental item = new ItemDocumental(
                "ENT.ESCOPO.002",
                "Organizar documentos por projeto e grupo",
                "Documento organizado por metadados explicitos.",
                Set.of(new RelacaoDocumental("rastreia", "BL.AC.004")),
                new RastreabilidadeDocumento(Set.of("BL.AC.004"))
        );
        VersaoDocumento versao = new VersaoDocumento(
                1,
                FormatoDocumento.YAML,
                "conteudo original",
                OffsetDateTime.parse("2026-08-25T10:20:30-03:00"),
                List.of(item)
        );
        Documento documento = new Documento(
                "DOC-001",
                projeto.identificador(),
                "Linha de base de especificacao",
                metadados,
                List.of(versao)
        );

        assertThat(documento.projeto()).isEqualTo(projeto.identificador());
        assertThat(projeto.participantes()).extracting(ParticipanteProjeto::papel)
                .containsExactly(PapelProjeto.RESPONSAVEL_TECNICO);
        assertThat(documento.metadados().grupo()).isEqualTo("Especificacao");
        assertThat(documento.versoes()).singleElement().satisfies(versaoRegistrada -> {
            assertThat(versaoRegistrada.formato()).isEqualTo(FormatoDocumento.YAML);
            assertThat(versaoRegistrada.itens()).containsExactly(item);
        });
    }

    @Test
    void deveOrganizarDocumentoSomentePorMetadadosExplicitosSemInferirRelacoes() {
        MetadadosDocumento metadadosSemRelacao = metadados(Set.of());
        ItemDocumental itemSemRelacao = new ItemDocumental(
                "ITEM-SEM-RELACAO",
                "Item sem relacao informada",
                "Conteudo recebido sem vinculo documental explicito.",
                Set.of(),
                new RastreabilidadeDocumento(Set.of("BL.AC.006"))
        );

        assertThat(metadadosSemRelacao.grupo()).isEqualTo("Especificacao");
        assertThat(metadadosSemRelacao.categoria()).isEqualTo("Linha de base");
        assertThat(metadadosSemRelacao.finalidade()).isEqualTo("Consulta institucional");
        assertThat(metadadosSemRelacao.possuiRelacoesExplicitamenteRecebidas()).isFalse();
        assertThat(itemSemRelacao.possuiRelacoesExplicitamenteRecebidas()).isFalse();
    }

    @Test
    void deveRejeitarOrganizacaoSemMetadadosObrigatorios() {
        assertThatThrownBy(() -> new MetadadosDocumento(
                " ",
                "Linha de base",
                "Consulta institucional",
                "Arvore documental",
                Set.of(PermissaoDocumento.CONSULTAR),
                Set.of(),
                new RastreabilidadeDocumento(Set.of("BL.AC.004"))
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("grupo do documento deve ser informado.");
    }

    @Test
    void deveRegistrarRestricaoContraTerminologiaDeProjetoPilotoNoModelo() {
        assertThatThrownBy(() -> new IdentificadorProjeto("SISPLAN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("identificador do projeto nao pode incorporar terminologia de projeto piloto.");

        assertThatThrownBy(() -> new ProjetoInstitucional(
                new IdentificadorProjeto("ATHENA"),
                "Projeto SISPLAN",
                "Descricao institucional",
                Set.of("DITEC"),
                Set.of(new ParticipanteProjeto(usuario(), PapelProjeto.PARTICIPANTE)),
                SituacaoProjeto.ATIVO,
                OffsetDateTime.parse("2026-08-25T10:15:30-03:00"),
                usuario()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("nome do projeto nao pode incorporar terminologia de projeto piloto.");
    }

    private static UsuarioInstitucional usuario() {
        return new UsuarioInstitucional(
                new IdentificadorUsuario("usuario-1"),
                "Usuario Institucional",
                "usuario.institucional",
                "DITEC"
        );
    }

    private static MetadadosDocumento metadados(Set<RelacaoDocumental> relacoes) {
        return new MetadadosDocumento(
                "Especificacao",
                "Linha de base",
                "Consulta institucional",
                "Arvore documental",
                Set.of(PermissaoDocumento.CONSULTAR, PermissaoDocumento.AVALIAR),
                relacoes,
                new RastreabilidadeDocumento(Set.of("BL.AC.004", "BL.AC.006"))
        );
    }
}
