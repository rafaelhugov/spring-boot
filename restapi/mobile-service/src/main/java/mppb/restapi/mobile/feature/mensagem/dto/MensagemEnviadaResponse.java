package mppb.restapi.mobile.feature.mensagem.dto;

import java.time.LocalDateTime;
import java.util.List;

import mppb.restapi.mobile.feature.mensagem.enums.ModalidadeEnvio;
import mppb.restapi.mobile.feature.mensagem.enums.PrioridadeMensagem;
import mppb.restapi.mobile.feature.mensagem.enums.StatusPublicacao;

public record MensagemEnviadaResponse(
        Long id,
        String titulo,
        String conteudo,
        PrioridadeMensagem prioridade,
        ModalidadeEnvio modalidade,
        boolean exigeCiencia,
        StatusPublicacao statusPublicacao,
        LocalDateTime dataAgendamento,
        LocalDateTime dataEnvio,
        LocalDateTime dataExpiracao,
        boolean arquivadaRemetente,
        long totalDestinatarios,
        long totalEntregues,
        long totalLidos,
        long totalCientes,
        long totalPendentes,
        List<MensagemAnexoResponse> anexos) {
}

