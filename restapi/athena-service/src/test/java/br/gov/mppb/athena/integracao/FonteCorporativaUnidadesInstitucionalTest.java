package br.gov.mppb.athena.integracao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FonteCorporativaUnidadesInstitucionalTest {

    @Test
    void deveUsarIdPessoaNomeESiglaDasTabelasPublicas() {
        assertThat(FonteCorporativaUnidadesInstitucional.consultaSql())
                .contains("public.orgao", "public.pessoa", "o.id_pessoa", "p.nome", "o.sigla")
                .doesNotContain("diariooficial.lotacao_vw");
    }
}
