package mppb.restapi.mobile.feature.mensagem.dto;

import java.util.List;

public record AcompanhamentoMensagemResponse(
        MensagemEnviadaResponse resumo,
        List<DestinatarioStatusResponse> destinatarios,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas) {
}

