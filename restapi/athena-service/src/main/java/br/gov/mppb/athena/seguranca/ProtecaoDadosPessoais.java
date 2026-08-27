package br.gov.mppb.athena.seguranca;

public final class ProtecaoDadosPessoais {

    private ProtecaoDadosPessoais() {
    }

    public static ParticipanteVisivel participanteVisivel(DadosParticipanteCorporativo participante) {
        if (participante == null) {
            throw new IllegalArgumentException("participante deve ser informado.");
        }
        return new ParticipanteVisivel(
                participante.nome(),
                participante.matricula(),
                participante.vinculo(),
                participante.cargoOuFuncao(),
                participante.email(),
                participante.login(),
                participante.unidadeOuLotacao()
        );
    }
}
