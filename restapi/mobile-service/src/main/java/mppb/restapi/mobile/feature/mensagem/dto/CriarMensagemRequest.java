package mppb.restapi.mobile.feature.mensagem.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mppb.restapi.mobile.feature.mensagem.enums.ModalidadeEnvio;
import mppb.restapi.mobile.feature.mensagem.enums.PrioridadeMensagem;

public record CriarMensagemRequest(
        @Size(max = 160, message = "O título deve ter no máximo 160 caracteres.")
        String titulo,
        String conteudo,
        @NotNull(message = "A prioridade é obrigatória.")
        PrioridadeMensagem prioridade,
        @NotNull(message = "A modalidade de envio é obrigatória.")
        ModalidadeEnvio modalidade,
        boolean exigeCiencia,
        CriteriosDestinatariosDto criterios,
        List<Long> anexoIds,
        LocalDateTime dataAgendamento,
        LocalDateTime dataExpiracao) {
}

