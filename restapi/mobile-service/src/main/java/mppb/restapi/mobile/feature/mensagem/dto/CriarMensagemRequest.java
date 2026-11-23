package mppb.restapi.mobile.feature.mensagem.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
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
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSS][.SS][.S]")
        LocalDateTime dataAgendamento,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSS][.SS][.S]")
        LocalDateTime dataExpiracao) {
}

