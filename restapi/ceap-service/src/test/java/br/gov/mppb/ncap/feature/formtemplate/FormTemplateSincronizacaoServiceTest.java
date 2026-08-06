package br.gov.mppb.ncap.feature.formtemplate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:template_sync_test;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS administrador\\\\;CREATE SCHEMA IF NOT EXISTS autorizacao\\\\;CREATE SCHEMA IF NOT EXISTS tj\\\\;CREATE SCHEMA IF NOT EXISTS ncap\\\\;SET SCHEMA_SEARCH_PATH ncap,public",
    "api.cnmp.forms=1108"
})
@ActiveProfiles({"test", "insecure"})
@Transactional
class FormTemplateSincronizacaoServiceTest {

    @Autowired
    private FormTemplateSincronizacaoService syncService;

    @Autowired
    private FormTemplateRepository formTemplateRepository;

    @Test
    void deveSincronizarTemplatesCriandoEstruturaCompletaESecoesEBlocosEPerguntas() {
        formTemplateRepository.deleteAll();

        // Executar a sincronização
        syncService.sincronizarTemplates();

        // Buscar template 1108
        Optional<FormTemplate> templateOpt = formTemplateRepository.findById(1108L);
        assertThat(templateOpt).isPresent();

        FormTemplate template = templateOpt.get();
        assertThat(template.getResolucao()).isEqualTo("Resolução 279");
        assertThat(template.getTipoentidade()).isEqualTo("POLICIA_CIVIL");
        assertThat(template.getAtivo()).isTrue();

        // Validar as seções
        List<Secao> secoes = template.getSecoes();
        assertThat(secoes).hasSize(1);
        Secao secao1 = secoes.get(0);
        assertThat(secao1.getNome()).isEqualTo("Informações Gerais");
        assertThat(secao1.getOrdem()).isEqualTo(1);

        // Validar os blocos
        List<Bloco> blocos = secao1.getBlocos();
        assertThat(blocos).hasSize(1);
        Bloco blocoGeral = blocos.get(0);
        assertThat(blocoGeral.getNome()).isEqualTo("Geral");
        assertThat(blocoGeral.getOrdem()).isEqualTo(1);

        // Validar as perguntas
        List<Pergunta> perguntas = blocoGeral.getPerguntas();
        assertThat(perguntas).hasSize(3);

        // Pergunta 1: TXT_OBSERVACOES_FINAIS_REPRESENTANTE_MINISTERIO_PUBLICO
        Pergunta p1 = perguntas.stream()
                .filter(p -> "TXT_OBSERVACOES_FINAIS_REPRESENTANTE_MINISTERIO_PUBLICO".equals(p.getIdcnmp()))
                .findFirst()
                .orElseThrow();
        assertThat(p1.getCampotipo()).isEqualTo("TEXTO");
        assertThat(p1.getObrigatorio()).isTrue();

        // Pergunta 2: PERG_EFETIVO_TOTAL
        Pergunta p2 = perguntas.stream()
                .filter(p -> "PERG_EFETIVO_TOTAL".equals(p.getIdcnmp()))
                .findFirst()
                .orElseThrow();
        assertThat(p2.getCampotipo()).isEqualTo("INTEIRO");

        // Pergunta 3: PERG_POSSUI_VIATURA (Com opções de resposta)
        Pergunta p3 = perguntas.stream()
                .filter(p -> "PERG_POSSUI_VIATURA".equals(p.getIdcnmp()))
                .findFirst()
                .orElseThrow();
        assertThat(p3.getCampotipo()).isEqualTo("RADIO");
        assertThat(p3.getOpcoes()).hasSize(2);

        List<OpcaoResposta> opcoes = p3.getOpcoes();
        assertThat(opcoes.get(0).getValor()).isEqualTo("SIM");
        assertThat(opcoes.get(0).getDescricao()).isEqualTo("Sim");
        assertThat(opcoes.get(1).getValor()).isEqualTo("NAO");
        assertThat(opcoes.get(1).getDescricao()).isEqualTo("Não");
    }
}
