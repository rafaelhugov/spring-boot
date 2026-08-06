package br.gov.mppb.ncap.feature.inspecao;

import br.gov.mppb.ncap.feature.entidade.Entidade;
import br.gov.mppb.ncap.feature.entidade.EntidadeRepository;
import br.gov.mppb.ncap.feature.formtemplate.FormTemplate;
import br.gov.mppb.ncap.feature.formtemplate.Secao;
import br.gov.mppb.ncap.feature.formtemplate.Bloco;
import br.gov.mppb.ncap.feature.formtemplate.Pergunta;
import mppb.restapi.common.core.error.BusinessRuleException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:inspecao_srv_test;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS administrador\\\\;CREATE SCHEMA IF NOT EXISTS autorizacao\\\\;CREATE SCHEMA IF NOT EXISTS tj\\\\;CREATE SCHEMA IF NOT EXISTS ncap\\\\;SET SCHEMA_SEARCH_PATH ncap,public"
})
@ActiveProfiles({"test", "insecure"})
@Transactional
public class InspecaoServiceTest {

    @Autowired
    private InspecaoService service;

    @Autowired
    private InspecaoRepository repository;

    @Autowired
    private EntidadeRepository entidadeRepository;

    @Autowired
    private EntityManager entityManager;

    private Entidade entidade;

    @BeforeEach
    void setUp() {
        entidade = Entidade.builder()
                .nomecnmp("1ª Delegacia de Teste")
                .tipoentidade("DELEGACIA")
                .idComarca(1L)
                .municipio("João Pessoa")
                .ativa(true)
                .build();
        entidade = entidadeRepository.save(entidade);
    }

    @Test
    void deveSalvarInspecaoPresencialComSucesso() {
        Inspecao inspecao = Inspecao.builder()
                .entidade(entidade)
                .idPromotorresponsavel(100L)
                .dataagendamento(LocalDate.of(2026, 3, 10))
                .semestre("2026.1")
                .tipoVisita(TipoVisita.PRESENCIAL)
                .build();

        Inspecao salva = service.save(inspecao);

        assertThat(salva.getId()).isNotNull();
        assertThat(salva.getIdServidorCriador()).isEqualTo(1L); // Fallback ID no perfil insecure
        assertThat(salva.getStatus()).isEqualTo(InspecaoStatus.AGENDADA);
        assertThat(salva.getTokenacesso()).isNotNull();
    }

    @Test
    void deveRejeitarVisitaVirtualNoPrimeiroSemestre() {
        Inspecao inspecao = Inspecao.builder()
                .entidade(entidade)
                .idPromotorresponsavel(100L)
                .dataagendamento(LocalDate.of(2026, 3, 10))
                .semestre("2026.1")
                .tipoVisita(TipoVisita.VIRTUAL)
                .justificativaRemota(JustificativaRemota.PRESENCA_FISICA_IMPOSSIBILITADA)
                .justificativaDetalhe("Justificativa detalhada de teste")
                .build();

        assertThatThrownBy(() -> service.save(inspecao))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Visitas do 1º semestre devem ser presenciais");
    }

    @Test
    void deveRejeitarVisitaVirtualNoSegundoSemestreSemJustificativa() {
        Inspecao inspecao = Inspecao.builder()
                .entidade(entidade)
                .idPromotorresponsavel(100L)
                .dataagendamento(LocalDate.of(2026, 9, 15))
                .semestre("2026.2")
                .tipoVisita(TipoVisita.VIRTUAL)
                .build();

        assertThatThrownBy(() -> service.save(inspecao))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Visitas virtuais exigem a seleção de uma justificativa");
    }

    @Test
    void deveSalvarVisitaVirtualNoSegundoSemestreComJustificativa() {
        Inspecao inspecao = Inspecao.builder()
                .entidade(entidade)
                .idPromotorresponsavel(100L)
                .dataagendamento(LocalDate.of(2026, 9, 15))
                .semestre("2026.2")
                .tipoVisita(TipoVisita.VIRTUAL)
                .justificativaRemota(JustificativaRemota.PRESENCA_FISICA_IMPOSSIBILITADA)
                .justificativaDetalhe("Justificativa detalhada")
                .build();

        Inspecao salva = service.save(inspecao);

        assertThat(salva.getId()).isNotNull();
        assertThat(salva.getTipoVisita()).isEqualTo(TipoVisita.VIRTUAL);
    }

    @Test
    void deveRejeitarDuplicidadeDeAgendamentoNoMesmoSemestre() {
        Inspecao inspecao1 = Inspecao.builder()
                .entidade(entidade)
                .idPromotorresponsavel(100L)
                .dataagendamento(LocalDate.of(2026, 3, 10))
                .semestre("2026.1")
                .tipoVisita(TipoVisita.PRESENCIAL)
                .build();
        service.save(inspecao1);

        Inspecao inspecao2 = Inspecao.builder()
                .entidade(entidade)
                .idPromotorresponsavel(101L)
                .dataagendamento(LocalDate.of(2026, 4, 15))
                .semestre("2026.1")
                .tipoVisita(TipoVisita.PRESENCIAL)
                .build();

        assertThatThrownBy(() -> service.save(inspecao2))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Já existe um agendamento de inspeção em andamento ou concluído");
    }

    @Test
    void deveAssociarFormTemplateEInicializarRespostasComSucesso() {
        // 1. Criar um template de formulário de teste
        FormTemplate template = FormTemplate.builder()
                .id(999L)
                .resolucao("Resolucao Teste")
                .tipoentidade("DELEGACIA")
                .ativo(true)
                .build();
        entityManager.persist(template);

        // 2. Criar uma seção de teste
        Secao secao = Secao.builder()
                .template(template)
                .nome("Seção Teste")
                .ordem(1)
                .build();
        entityManager.persist(secao);

        // 3. Criar um bloco de teste
        Bloco bloco = Bloco.builder()
                .secao(secao)
                .nome("Bloco Teste")
                .ordem(1)
                .build();
        entityManager.persist(bloco);

        // 4. Criar uma pergunta de teste
        Pergunta pergunta = Pergunta.builder()
                .bloco(bloco)
                .label("Pergunta Teste")
                .campotipo("TEXTO")
                .ordem(1)
                .build();
        entityManager.persist(pergunta);

        entityManager.flush();

        // 5. Agendar a inspeção para a entidade (que possui tipoentidade 'DELEGACIA')
        Inspecao inspecao = Inspecao.builder()
                .entidade(entidade)
                .idPromotorresponsavel(100L)
                .dataagendamento(LocalDate.of(2026, 3, 10))
                .semestre("2026.1")
                .tipoVisita(TipoVisita.PRESENCIAL)
                .build();

        Inspecao salva = service.save(inspecao);

        // 6. Verificar se associou o form template correto
        assertThat(salva.getIdFormtemplate()).isEqualTo(999L);

        // 7. Verificar se inicializou a resposta no banco de dados
        Number countRespostas = (Number) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM ncap.respostasinspecao WHERE id_inspecao = :idInspecao AND id_pergunta = :idPergunta")
                .setParameter("idInspecao", salva.getId())
                .setParameter("idPergunta", pergunta.getId())
                .getSingleResult();

        assertThat(countRespostas.intValue()).isEqualTo(1);
    }
}
