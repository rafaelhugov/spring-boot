package br.gov.mppb.athena.seguranca;

import static org.assertj.core.api.Assertions.assertThat;

import br.gov.mppb.athena.dominio.IdentificadorProjeto;
import br.gov.mppb.athena.dominio.IdentificadorUsuario;
import br.gov.mppb.athena.dominio.PapelProjeto;
import br.gov.mppb.athena.dominio.ParticipanteProjeto;
import br.gov.mppb.athena.dominio.ProjetoInstitucional;
import br.gov.mppb.athena.dominio.SituacaoProjeto;
import br.gov.mppb.athena.dominio.UsuarioInstitucional;
import java.time.OffsetDateTime;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PoliticaSegurancaAthenaTest {

    @Test
    void deveDescreverAutenticacaoInstitucionalEAutorizacaoPorProjetoParaOperacoesFuncionais() {
        UsuarioInstitucional responsavel = usuario("usuario-1", "responsavel", "DITEC");
        ProjetoInstitucional projeto = projetoComParticipante(responsavel, PapelProjeto.RESPONSAVEL_TECNICO);
        ContextoAutorizacaoInstitucional contexto = new ContextoAutorizacaoInstitucional(responsavel, Set.of());

        assertThat(PoliticaSegurancaAthena.autorizar(projeto, contexto, OperacaoProjeto.CONSULTAR).permitido()).isTrue();
        assertThat(PoliticaSegurancaAthena.autorizar(projeto, contexto, OperacaoProjeto.AVALIAR).permitido()).isTrue();
        assertThat(PoliticaSegurancaAthena.autorizar(projeto, contexto, OperacaoProjeto.COMENTAR).permitido()).isTrue();
        assertThat(PoliticaSegurancaAthena.autorizar(projeto, contexto, OperacaoProjeto.CONFIRMAR).permitido()).isTrue();

        ResultadoAutorizacao semIdentidade = PoliticaSegurancaAthena.autorizar(
                projeto,
                new ContextoAutorizacaoInstitucional(null, Set.of()),
                OperacaoProjeto.CONSULTAR
        );

        assertThat(semIdentidade.permitido()).isFalse();
        assertThat(semIdentidade.motivo()).contains("identidade institucional");
    }

    @Test
    void deveNegarOperacaoProibidaParaUsuarioSemAutorizacaoNoBackend() {
        UsuarioInstitucional responsavel = usuario("usuario-1", "responsavel", "DITEC");
        UsuarioInstitucional usuarioSemVinculo = usuario("usuario-2", "sem.vinculo", "GABINETE");
        ProjetoInstitucional projeto = projetoComParticipante(responsavel, PapelProjeto.RESPONSAVEL_TECNICO);

        ResultadoAutorizacao resultado = PoliticaSegurancaAthena.autorizar(
                projeto,
                new ContextoAutorizacaoInstitucional(usuarioSemVinculo, Set.of()),
                OperacaoProjeto.AVALIAR
        );

        assertThat(resultado.permitido()).isFalse();
        assertThat(resultado.motivo()).contains("sem vinculo, papel ou permissao especifica");
    }

    @Test
    void devePermitirSomenteConsultaDocumentalParaParticipanteFormalmenteVinculado() {
        UsuarioInstitucional participante = usuario("usuario-1", "participante", "GABINETE");
        ProjetoInstitucional projeto = projetoComParticipante(participante, PapelProjeto.PARTICIPANTE);
        ContextoAutorizacaoInstitucional contexto = new ContextoAutorizacaoInstitucional(participante, Set.of());

        assertThat(PoliticaSegurancaAthena.autorizar(projeto, contexto, OperacaoProjeto.CONSULTAR).permitido()).isTrue();
        assertThat(PoliticaSegurancaAthena.autorizar(projeto, contexto, OperacaoProjeto.AVALIAR).permitido()).isFalse();
        assertThat(PoliticaSegurancaAthena.autorizar(projeto, contexto, OperacaoProjeto.COMENTAR).permitido()).isFalse();
        assertThat(PoliticaSegurancaAthena.autorizar(projeto, contexto, OperacaoProjeto.CONFIRMAR).permitido()).isFalse();
        assertThat(PoliticaSegurancaAthena.autorizar(projeto, contexto, OperacaoProjeto.ADMINISTRAR).permitido()).isFalse();
    }

    @Test
    void devePermitirOperacaoPorAutorizacaoInstitucionalEspecificaNoProjeto() {
        UsuarioInstitucional responsavel = usuario("usuario-1", "responsavel", "DITEC");
        UsuarioInstitucional autorizado = usuario("usuario-2", "autorizado", "GABINETE");
        ProjetoInstitucional projeto = projetoComParticipante(responsavel, PapelProjeto.RESPONSAVEL_TECNICO);
        String autorizacao = PoliticaSegurancaAthena.autorizacaoEspecifica(projeto, OperacaoProjeto.CONFIRMAR);

        ResultadoAutorizacao resultado = PoliticaSegurancaAthena.autorizar(
                projeto,
                new ContextoAutorizacaoInstitucional(autorizado, Set.of(autorizacao)),
                OperacaoProjeto.CONFIRMAR
        );

        assertThat(resultado.permitido()).isTrue();
        assertThat(resultado.motivo()).contains("autorizacao institucional especifica");
    }

    @Test
    void deveExigirLotacaoAplicavelEAutorizacaoInstitucionalEspecificaParaAdministracao() {
        UsuarioInstitucional desenvolvedorAutorizado = usuario("usuario-1", "dev.autorizado", "DITEC");
        UsuarioInstitucional desenvolvedorSemAutorizacao = usuario("usuario-2", "dev.sem.autorizacao", "DITEC");
        UsuarioInstitucional usuarioDeOutraLotacao = usuario("usuario-3", "autorizado.fora.ditec", "GABINETE");
        ProjetoInstitucional projeto = projetoComParticipante(desenvolvedorAutorizado, PapelProjeto.RESPONSAVEL_TECNICO);

        assertThat(PoliticaSegurancaAthena.autorizar(
                projeto,
                new ContextoAutorizacaoInstitucional(desenvolvedorAutorizado, Set.of(PoliticaSegurancaAthena.AUTORIZACAO_ADMINISTRATIVA)),
                OperacaoProjeto.ADMINISTRAR
        ).permitido()).isTrue();

        assertThat(PoliticaSegurancaAthena.autorizar(
                projeto,
                new ContextoAutorizacaoInstitucional(desenvolvedorSemAutorizacao, Set.of()),
                OperacaoProjeto.ADMINISTRAR
        ).permitido()).isFalse();

        assertThat(PoliticaSegurancaAthena.autorizar(
                projeto,
                new ContextoAutorizacaoInstitucional(usuarioDeOutraLotacao, Set.of(PoliticaSegurancaAthena.AUTORIZACAO_ADMINISTRATIVA)),
                OperacaoProjeto.ADMINISTRAR
        ).permitido()).isFalse();
    }

    @Test
    void deveOmitirCpfDaRepresentacaoVisivelDeParticipante() {
        ParticipanteVisivel visivel = ProtecaoDadosPessoais.participanteVisivel(new DadosParticipanteCorporativo(
                "Usuario Institucional",
                "123456",
                "Servidor",
                "Analista ministerial",
                "usuario.institucional@mppb.mp.br",
                "usuario.institucional",
                "DITEC",
                "000.000.000-00"
        ));

        assertThat(visivel.nome()).isEqualTo("Usuario Institucional");
        assertThat(visivel.matricula()).isEqualTo("123456");
        assertThat(visivel.vinculo()).isEqualTo("Servidor");
        assertThat(visivel.cargoOuFuncao()).isEqualTo("Analista ministerial");
        assertThat(visivel.email()).isEqualTo("usuario.institucional@mppb.mp.br");
        assertThat(visivel.login()).isEqualTo("usuario.institucional");
        assertThat(visivel.unidadeOuLotacao()).isEqualTo("DITEC");
        assertThat(ParticipanteVisivel.class.getRecordComponents())
                .extracting(recordComponent -> recordComponent.getName().toLowerCase())
                .doesNotContain("cpf");
    }

    private static ProjetoInstitucional projetoComParticipante(UsuarioInstitucional usuario, PapelProjeto papel) {
        return new ProjetoInstitucional(
                new IdentificadorProjeto("ATHENA"),
                "Athena",
                "Plataforma institucional multiprojeto documental",
                Set.of("DITEC"),
                Set.of(new ParticipanteProjeto(usuario, papel)),
                SituacaoProjeto.ATIVO,
                OffsetDateTime.parse("2026-08-25T10:15:30-03:00"),
                usuario
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
