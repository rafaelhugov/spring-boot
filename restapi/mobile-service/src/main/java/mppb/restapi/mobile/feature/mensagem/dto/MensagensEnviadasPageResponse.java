package mppb.restapi.mobile.feature.mensagem.dto;

import java.util.List;

public record MensagensEnviadasPageResponse(
        List<MensagemEnviadaResponse> itens,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas) {
}

