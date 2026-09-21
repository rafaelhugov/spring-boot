package br.gov.mppb.athena.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gov.mppb.athena.dominio.IdentificadorProjeto;
import br.gov.mppb.athena.dominio.IdentificadorUsuario;
import br.gov.mppb.athena.dominio.PapelProjeto;
import br.gov.mppb.athena.dominio.ParticipanteProjeto;
import br.gov.mppb.athena.dominio.ProjetoInstitucional;
import br.gov.mppb.athena.dominio.SituacaoProjeto;
import br.gov.mppb.athena.dominio.UsuarioInstitucional;
import br.gov.mppb.athena.seguranca.ContextoAutorizacaoInstitucional;
import br.gov.mppb.athena.seguranca.DadosParticipanteCorporativo;
import br.gov.mppb.athena.seguranca.ParticipanteVisivel;
import br.gov.mppb.athena.seguranca.PoliticaSegurancaAthena;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ServicoConsultaParticipantesCorporativosTest {

    @Test
    void deveDefinirConsultaCorporativaComFiltrosDadosRetornaveisECondicaoDeUsoParaVinculacao() {
        ConsultaParticipantesCorporativos consulta = new ConsultaParticipantesCorporativos(
                "ana",
                "123456",
                "DITEC"
        );
        var fonte = new FonteMemoria(List.of(participanteCorporativo()));
        var servico = new ServicoConsultaParticipantesCorporativos(fonte);

        List<ParticipanteVisivel> participantes = servico.pesquisarParaVinculacao(
                projeto(),
                contextoAdministrador(),
                consulta
        );

        assertThat(fonte.consultaRecebida()).isEqualTo(consulta);
        assertThat(participantes).singleElement().satisfies(participante -> {
            assertThat(participante.nome()).isEqualTo("Ana Institucional");
            assertThat(participante.matricula()).isEqualTo("123456");
            assertThat(participante.vinculo()).isEqualTo("Servidora");
            assertThat(participante.cargoOuFuncao()).isEqualTo("Analista ministerial");
            assertThat(participante.email()).isEqualTo("ana.institucional@mppb.mp.br");
            assertThat(participante.login()).isEqualTo("ana.institucional");
            assertThat(participante.unidadeOuLotacao()).isEqualTo("DITEC");
        });
    }

    @Test
    void deveLimitarRetornoAosDadosInstitucionaisPermitidosSemCpf() {
        var servico = new ServicoConsultaParticipantesCorporativos(
                new FonteMemoria(List.of(participanteCorporativo()))
        );

        List<ParticipanteVisivel> participantes = servico.pesquisarParaVinculacao(
                projeto(),
                contextoAdministrador(),
                ConsultaParticipantesCorporativos.porTermo("ana")
        );

        assertThat(ParticipanteVisivel.class.getRecordComponents())
                .extracting(componente -> componente.getName().toLowerCase())
                .containsExactly("nome", "matricula", "vinculo", "cargooufuncao", "email", "login", "unidadeoulotacao")
                .doesNotContain("cpf");
        assertThat(participantes).singleElement()
                .extracting(ParticipanteVisivel::nome, ParticipanteVisivel::matricula, ParticipanteVisivel::login)
                .containsExactly("Ana Institucional", "123456", "ana.institucional");
    }

    @Test
    void devePesquisarNasViewsCorporativasSemSelecionarCpf() {
        String sql = FonteCorporativaParticipantesInstitucional.consultaSql();

        assertThat(sql)
                .contains("autorizacao.usuarioservidor_vw")
                .contains("diariooficial.lotacao_vw")
                .contains("diariooficial.orgaos_dos_usuarios_ativos_vw")
                .doesNotContainIgnoringCase("cpf");
    }

    @Test
    void deveCompatibilizarConsultaComAutorizacaoEProtecaoDeDadosDoAthena() {
        var servico = new ServicoConsultaParticipantesCorporativos(new FonteMemoria(List.of(participanteCorporativo())));

        assertThatThrownBy(() -> servico.pesquisarParaVinculacao(
                projeto(),
                contextoSemAutorizacaoAdministrativa(),
                ConsultaParticipantesCorporativos.porTermo("ana")
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("consulta corporativa de participantes nao autorizada.");
    }

    @Test
    void deveExigirFiltroParaEvitarConsultaCorporativaIndiscriminada() {
        assertThatThrownBy(() -> new ConsultaParticipantesCorporativos(" ", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ao menos um filtro corporativo deve ser informado.");
    }

    @Test
    void deveExigirTamanhoMinimoParaFiltroCorporativo() {
        assertThatThrownBy(() -> new ConsultaParticipantesCorporativos("ab", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("termo de pesquisa deve ter ao menos 3 caracteres.");
    }

    private static DadosParticipanteCorporativo participanteCorporativo() {
        return new DadosParticipanteCorporativo(
                "Ana Institucional",
                "123456",
                "Servidora",
                "Analista ministerial",
                "ana.institucional@mppb.mp.br",
                "ana.institucional",
                "DITEC",
                "000.000.000-00"
        );
    }

    private static ContextoAutorizacaoInstitucional contextoAdministrador() {
        return new ContextoAutorizacaoInstitucional(
                usuario("usuario-1", "admin.athena", "DITEC"),
                Set.of(PoliticaSegurancaAthena.AUTORIZACAO_ADMINISTRATIVA)
        );
    }

    private static ContextoAutorizacaoInstitucional contextoSemAutorizacaoAdministrativa() {
        return new ContextoAutorizacaoInstitucional(
                usuario("usuario-2", "usuario.comum", "DITEC"),
                Set.of()
        );
    }

    private static ProjetoInstitucional projeto() {
        UsuarioInstitucional criador = usuario("usuario-1", "admin.athena", "DITEC");
        return new ProjetoInstitucional(
                new IdentificadorProjeto("ATHENA"),
                "Athena",
                "Plataforma institucional multiprojeto documental",
                Set.of("DITEC"),
                Set.of(new ParticipanteProjeto(criador, PapelProjeto.RESPONSAVEL_TECNICO)),
                SituacaoProjeto.ATIVO,
                OffsetDateTime.parse("2026-08-25T10:15:30-03:00"),
                criador
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

    private static final class FonteMemoria implements FonteCorporativaParticipantes {
        private final List<DadosParticipanteCorporativo> participantes;
        private ConsultaParticipantesCorporativos consultaRecebida;

        private FonteMemoria(List<DadosParticipanteCorporativo> participantes) {
            this.participantes = participantes;
        }

        @Override
        public List<DadosParticipanteCorporativo> pesquisar(ConsultaParticipantesCorporativos consulta) {
            this.consultaRecebida = consulta;
            return participantes;
        }

        private ConsultaParticipantesCorporativos consultaRecebida() {
            return consultaRecebida;
        }
    }
}
