package br.gov.mppb.athena.dominio;

import java.util.Objects;

public final class ServicoVersaoAtualDocumento {

    public EstadoVersaoAtualDocumento atualizar(
            EstadoVersaoAtualDocumento estado,
            ComandoAtualizarVersaoAtual comando,
            Runnable persistirNovaVersao
    ) {
        Objects.requireNonNull(estado, "estado deve ser informado.");
        Objects.requireNonNull(comando, "comando deve ser informado.");
        Objects.requireNonNull(persistirNovaVersao, "persistencia deve ser informada.");
        if (!estado.idDocumento().equals(comando.idDocumento())) {
            throw new IllegalArgumentException("estado e comando devem tratar o mesmo documento.");
        }
        if (!comando.idDocumento().equals(comando.idDocumentoVersao())) {
            throw new IllegalArgumentException("versao atual deve pertencer ao proprio documento.");
        }
        if (estado.versaoControle() != comando.versaoControleEsperada()
                || !Objects.equals(estado.idVersaoAtual(), comando.idVersaoAtualEsperada())) {
            throw new IllegalStateException("referencia ou versao de controle divergente.");
        }

        persistirNovaVersao.run();
        return new EstadoVersaoAtualDocumento(
                estado.idDocumento(),
                comando.idVersaoNova(),
                estado.versaoControle() + 1
        );
    }
}
