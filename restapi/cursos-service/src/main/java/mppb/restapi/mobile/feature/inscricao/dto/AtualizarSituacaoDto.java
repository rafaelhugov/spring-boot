package mppb.restapi.mobile.feature.inscricao.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO para a requisição de atualização da situação de uma inscrição.
 */
@Data
public class AtualizarSituacaoDto {

    @NotBlank(message = "A nova situação não pode ser vazia.")
    private String situacao;
}

