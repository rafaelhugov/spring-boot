package br.gov.mppb.athena.integracao;

import br.gov.mppb.athena.dominio.ProjetoInstitucional;
import br.gov.mppb.athena.seguranca.ContextoAutorizacaoInstitucional;
import br.gov.mppb.athena.seguranca.OperacaoProjeto;
import br.gov.mppb.athena.seguranca.ParticipanteVisivel;
import br.gov.mppb.athena.seguranca.PoliticaSegurancaAthena;
import br.gov.mppb.athena.seguranca.ProtecaoDadosPessoais;
import java.util.List;

public final class ServicoConsultaParticipantesCorporativos {

    private final FonteCorporativaParticipantes fonteCorporativa;

    public ServicoConsultaParticipantesCorporativos(FonteCorporativaParticipantes fonteCorporativa) {
        if (fonteCorporativa == null) {
            throw new IllegalArgumentException("fonte corporativa de participantes deve ser informada.");
        }
        this.fonteCorporativa = fonteCorporativa;
    }

    public List<ParticipanteVisivel> pesquisarParaVinculacao(
            ProjetoInstitucional projeto,
            ContextoAutorizacaoInstitucional contexto,
            ConsultaParticipantesCorporativos consulta
    ) {
        if (consulta == null) {
            throw new IllegalArgumentException("consulta corporativa de participantes deve ser informada.");
        }
        var autorizacao = PoliticaSegurancaAthena.autorizar(projeto, contexto, OperacaoProjeto.ADMINISTRAR);
        if (!autorizacao.permitido()) {
            throw new IllegalStateException("consulta corporativa de participantes nao autorizada.");
        }

        return fonteCorporativa.pesquisar(consulta)
                .stream()
                .map(ProtecaoDadosPessoais::participanteVisivel)
                .toList();
    }
}
