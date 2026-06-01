package mppb.restapi.cursos.feature.frequencia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO de resposta após solicitar envio de OTP.")
public class SolicitacaoOtpResponseDto {

    @Schema(description = "Mensagem informando que o e-mail foi enviado (com e-mail mascarado).", example = "E-mail enviado para ra***@gmail.com")
    private String mensagem;
}
