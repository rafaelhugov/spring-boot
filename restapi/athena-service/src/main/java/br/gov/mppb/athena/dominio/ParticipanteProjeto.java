package br.gov.mppb.athena.dominio;

public record ParticipanteProjeto(
        UsuarioInstitucional usuario,
        PapelProjeto papel
) {

    public ParticipanteProjeto {
        if (usuario == null) {
            throw new IllegalArgumentException("usuario participante deve ser informado.");
        }
        if (papel == null) {
            throw new IllegalArgumentException("papel do participante deve ser informado.");
        }
    }
}
