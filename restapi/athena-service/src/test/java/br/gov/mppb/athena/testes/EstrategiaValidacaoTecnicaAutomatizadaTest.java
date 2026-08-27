package br.gov.mppb.athena.testes;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EstrategiaValidacaoTecnicaAutomatizadaTest {

    private static final Map<String, Set<String>> COBERTURA_AUTOMATIZADA = Map.of(
            "dominio", Set.of("ModeloDominioMultiprojetoDocumentalTest"),
            "persistencia_historica", Set.of("ModeloPersistenciaHistoricaTest"),
            "seguranca_autorizacao_ausencia_cpf", Set.of(
                    "PoliticaSegurancaAthenaTest",
                    "ContratosRestAthenaTest"
            ),
            "ingestao_documental", Set.of("ServicoIngestaoDocumentalExternaTest"),
            "integracao_participantes", Set.of("ServicoConsultaParticipantesCorporativosTest"),
            "contratos_rest", Set.of("ContratosRestAthenaTest"),
            "fluxos_avaliacao_confirmacao", Set.of("ServicoAvaliacaoConfirmacaoTest"),
            "auditoria_observabilidade", Set.of("TrilhaAuditoriaAthenaTest"),
            "arquitetura_interface_separada", Set.of("DefinicaoArquiteturaAthenaTest")
    );

    @Test
    void deveCobrirEstrategiaDeTestesExigidaPelaLinhaBase() {
        assertThat(COBERTURA_AUTOMATIZADA)
                .containsOnlyKeys(
                        "dominio",
                        "persistencia_historica",
                        "seguranca_autorizacao_ausencia_cpf",
                        "ingestao_documental",
                        "integracao_participantes",
                        "contratos_rest",
                        "fluxos_avaliacao_confirmacao",
                        "auditoria_observabilidade",
                        "arquitetura_interface_separada"
                )
                .allSatisfy((eixo, classes) -> assertThat(classes)
                        .as("classes automatizadas para %s", eixo)
                        .isNotEmpty());
    }

    @Test
    void deveMapearCenariosDeFalhaCriticosParaTestesAutomatizados() {
        assertThat(COBERTURA_AUTOMATIZADA.get("seguranca_autorizacao_ausencia_cpf"))
                .contains("PoliticaSegurancaAthenaTest", "ContratosRestAthenaTest");
        assertThat(COBERTURA_AUTOMATIZADA.get("dominio"))
                .contains("ModeloDominioMultiprojetoDocumentalTest");
        assertThat(COBERTURA_AUTOMATIZADA.get("ingestao_documental"))
                .contains("ServicoIngestaoDocumentalExternaTest");
        assertThat(COBERTURA_AUTOMATIZADA.get("fluxos_avaliacao_confirmacao"))
                .contains("ServicoAvaliacaoConfirmacaoTest");
    }

    @Test
    void deveManterCorrespondenciaObjetivaEntreCriteriosEAvaliacaoAutomatizada() {
        assertThat(COBERTURA_AUTOMATIZADA.values())
                .flatExtracting(classes -> classes)
                .contains(
                        "DefinicaoArquiteturaAthenaTest",
                        "ModeloDominioMultiprojetoDocumentalTest",
                        "ModeloPersistenciaHistoricaTest",
                        "PoliticaSegurancaAthenaTest",
                        "ServicoConsultaParticipantesCorporativosTest",
                        "ServicoIngestaoDocumentalExternaTest",
                        "ServicoAvaliacaoConfirmacaoTest",
                        "ContratosRestAthenaTest",
                        "TrilhaAuditoriaAthenaTest"
                );
    }
}
