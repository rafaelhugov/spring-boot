package br.gov.mppb.athena.seguranca;

public record ParticipanteVisivel(
        String nome,
        String matricula,
        String vinculo,
        String cargoOuFuncao,
        String email,
        String login,
        String unidadeOuLotacao
) {
}
