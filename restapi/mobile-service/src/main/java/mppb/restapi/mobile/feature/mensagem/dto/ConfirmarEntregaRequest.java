package mppb.restapi.mobile.feature.mensagem.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record ConfirmarEntregaRequest(
        @NotEmpty(message = "Informe ao menos um identificador de mensagem.")
        List<Long> mensagemIds) {
}

