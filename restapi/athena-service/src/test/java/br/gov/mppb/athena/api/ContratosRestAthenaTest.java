package br.gov.mppb.athena.api;

import static org.assertj.core.api.Assertions.assertThat;

import br.gov.mppb.athena.dominio.IdentificadorProjeto;
import br.gov.mppb.athena.dominio.IdentificadorUsuario;
import br.gov.mppb.athena.dominio.PapelProjeto;
import br.gov.mppb.athena.dominio.ParticipanteProjeto;
import br.gov.mppb.athena.dominio.ProjetoInstitucional;
import br.gov.mppb.athena.dominio.SituacaoProjeto;
import br.gov.mppb.athena.dominio.UsuarioInstitucional;
import br.gov.mppb.athena.seguranca.ContextoAutorizacaoInstitucional;
import br.gov.mppb.athena.seguranca.OperacaoProjeto;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ContratosRestAthenaTest {

    @Test
    void deveCobrirContratosRestDeProjetoDocumentoItemInteracoesEParticipantes() {
        assertThat(gruposContratados())
                .containsExactlyInAnyOrder(
                        GrupoOperacaoApi.PROJETO,
                        GrupoOperacaoApi.DOCUMENTO,
                        GrupoOperacaoApi.ITEM_DOCUMENTAL,
                        GrupoOperacaoApi.AVALIACAO,
                        GrupoOperacaoApi.COMENTARIO,
                        GrupoOperacaoApi.PEDIDO_AJUSTE,
                        GrupoOperacaoApi.CONFIRMACAO,
                        GrupoOperacaoApi.PARTICIPANTE
                );

        assertThat(ContratosRestAthena.todos())
                .extracting(ContratoRestAthena::caminho)
                .contains(
                        "/api/projetos",
                        "/api/projetos/{projetoId}",
                        "/api/projetos/{projetoId}/documentos",
                        "/api/projetos/{projetoId}/documentos/{documentoId}",
                        "/api/projetos/{projetoId}/documentos/{documentoId}/itens/{itemId}",
                        "/api/projetos/{projetoId}/documentos/{documentoId}/avaliacoes",
                        "/api/projetos/{projetoId}/documentos/{documentoId}/comentarios",
                        "/api/projetos/{projetoId}/documentos/{documentoId}/ajustes",
                        "/api/projetos/{projetoId}/documentos/{documentoId}/confirmacoes",
                        "/api/projetos/{projetoId}/participantes/pesquisa"
                );
    }

    @Test
    void deveDescreverEntradasSaidasErrosFuncionaisEAutorizacaoPorProjeto() {
        assertThat(ContratosRestAthena.todos())
                .allSatisfy(contrato -> {
                    assertThat(contrato.entradas()).isNotEmpty();
                    assertThat(contrato.saidas()).isNotEmpty();
                    assertThat(contrato.errosFuncionais())
                            .extracting(ErroFuncionalApi::codigo)
                            .contains(
                                    "REQUISICAO_INVALIDA",
                                    "IDENTIDADE_INSTITUCIONAL_AUSENTE",
                                    "OPERACAO_NAO_AUTORIZADA",
                                    "RECURSO_NAO_ENCONTRADO",
                                    "REGRA_FUNCIONAL_VIOLADA"
                            );
                    assertThat(contrato.fontesRastreabilidade())
                            .contains("BL.AC.001", "BL.AC.002", "BL.AC.003", "BL.AC.006", "SOL.API.001", "SOL.SEGURANCA.001");
                });

        assertThat(ContratosRestAthena.porGrupo(GrupoOperacaoApi.PROJETO))
                .extracting(ContratoRestAthena::operacaoAutorizacao)
                .contains(OperacaoProjeto.ADMINISTRAR, OperacaoProjeto.CONSULTAR);
        assertThat(ContratosRestAthena.porGrupo(GrupoOperacaoApi.PARTICIPANTE))
                .singleElement()
                .extracting(ContratoRestAthena::operacaoAutorizacao)
                .isEqualTo(OperacaoProjeto.ADMINISTRAR);
    }

    @Test
    void deveOmitirCpfERestringirRelacoesApenasAsExplicitamenteRecebidas() {
        assertThat(ContratosRestAthena.todos())
                .allSatisfy(contrato -> {
                    assertThat(contrato.respostaOmiteCpf()).isTrue();
                    assertThat(contrato.respostaLimitaRelacoesExplicitamenteRecebidas()).isTrue();
                    assertThat(camposNormalizados(contrato.saidas())).doesNotContain("cpf");
                });

        assertThat(ContratosRestAthena.porGrupo(GrupoOperacaoApi.DOCUMENTO))
                .allSatisfy(contrato -> assertThat(contrato.saidas()).contains("relacoesRecebidas"));
        assertThat(ContratosRestAthena.porGrupo(GrupoOperacaoApi.ITEM_DOCUMENTAL))
                .singleElement()
                .satisfies(contrato -> assertThat(contrato.saidas()).contains("relacoesRecebidas"));
    }

    @Test
    void deveNegarContratoParaUsuarioSemAutorizacaoNoBackend() {
        ContratoRestAthena contratoAvaliacao = ContratosRestAthena.porGrupo(GrupoOperacaoApi.AVALIACAO).get(0);
        ProjetoInstitucional projeto = projeto();
        ContextoAutorizacaoInstitucional contextoSemVinculo = new ContextoAutorizacaoInstitucional(
                usuario("usuario-2", "sem.vinculo", "GABINETE"),
                Set.of()
        );

        assertThat(contratoAvaliacao.autorizar(projeto, contextoSemVinculo).permitido()).isFalse();
        assertThat(contratoAvaliacao.autorizar(projeto, contextoSemVinculo).motivo())
                .contains("sem vinculo, papel ou permissao especifica");
    }

    private static Set<GrupoOperacaoApi> gruposContratados() {
        return ContratosRestAthena.todos().stream()
                .map(ContratoRestAthena::grupo)
                .collect(Collectors.toSet());
    }

    private static Set<String> camposNormalizados(Set<String> campos) {
        return campos.stream()
                .map(campo -> campo.toLowerCase().replace("_", ""))
                .collect(Collectors.toSet());
    }

    private static ProjetoInstitucional projeto() {
        UsuarioInstitucional responsavel = usuario("usuario-1", "responsavel", "DITEC");
        return new ProjetoInstitucional(
                new IdentificadorProjeto("ATHENA"),
                "Athena",
                "Plataforma institucional multiprojeto documental",
                Set.of("DITEC"),
                Set.of(new ParticipanteProjeto(responsavel, PapelProjeto.RESPONSAVEL_TECNICO)),
                SituacaoProjeto.ATIVO,
                OffsetDateTime.parse("2026-08-25T10:15:30-03:00"),
                responsavel
        );
    }

    private static UsuarioInstitucional usuario(String id, String login, String unidade) {
        return new UsuarioInstitucional(
                new IdentificadorUsuario(id),
                "Usuario Institucional",
                login,
                unidade
        );
    }
}
