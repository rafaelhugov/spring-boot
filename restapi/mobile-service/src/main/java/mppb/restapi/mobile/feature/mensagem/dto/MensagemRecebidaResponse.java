package mppb.restapi.mobile.feature.mensagem.dto;

import java.time.LocalDateTime;
import java.util.List;

import mppb.restapi.mobile.feature.mensagem.enums.ModalidadeEnvio;
import mppb.restapi.mobile.feature.mensagem.enums.PrioridadeMensagem;
import mppb.restapi.mobile.feature.mensagem.enums.StatusDestinatario;

public record MensagemRecebidaResponse(
        Long id,
        String titulo,
        String conteudo,
        PrioridadeMensagem prioridade,
        ModalidadeEnvio modalidade,
        boolean exigeCiencia,
        String remetenteNome,
        LocalDateTime dataEnvio,
        LocalDateTime dataExpiracao,
        StatusDestinatario status,
        LocalDateTime dataEntrega,
        LocalDateTime dataLeitura,
        LocalDateTime dataCiencia,
        boolean arquivada,
        List<MensagemAnexoResponse> anexos) {
}

