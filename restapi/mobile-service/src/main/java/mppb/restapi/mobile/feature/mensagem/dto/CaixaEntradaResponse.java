package mppb.restapi.mobile.feature.mensagem.dto;

import java.util.List;

public record CaixaEntradaResponse(
        List<MensagemRecebidaResponse> itens,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas,
        long totalNaoLidas) {
}

