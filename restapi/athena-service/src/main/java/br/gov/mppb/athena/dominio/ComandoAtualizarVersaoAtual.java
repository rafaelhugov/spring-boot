package br.gov.mppb.athena.dominio;

import java.util.UUID;

public record ComandoAtualizarVersaoAtual(
        UUID idDocumento,
        UUID idVersaoNova,
        UUID idDocumentoVersao,
        UUID idVersaoAtualEsperada,
        long versaoControleEsperada
) {

    public ComandoAtualizarVersaoAtual {
        if (idDocumento == null || idVersaoNova == null || idDocumentoVersao == null) {
            throw new IllegalArgumentException("documento e versao alvo devem ser informados.");
        }
        if (versaoControleEsperada < 0) {
            throw new IllegalArgumentException("versao de controle esperada nao pode ser negativa.");
        }
    }
}
