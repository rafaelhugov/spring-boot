package mppb.restapi.mobile.feature.notificacao.dto;

import jakarta.validation.constraints.Size;

public record DiagnosticoDispositivoRequest(
        @Size(max = 64)
        String etapa,

        @Size(max = 200)
        String mensagem,

        @Size(max = 32)
        String permissao,

        Boolean possuiToken,

        Integer tamanhoToken,

        Integer statusBackend,

        @Size(max = 120)
        String erro) {
}
