package mppb.restapi.cursos.feature.frequencia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para registrar a frequência do participante via QR Code.")
public class RegistroFrequenciaDto {

    @NotBlank(message = "O CPF é obrigatório.")
    @Pattern(regexp = "\\d{11}", message = "O CPF deve conter exatamente 11 dígitos numéricos.")
    @Schema(description = "CPF do participante, usado para identificação.", example = "12345678900")
    private String cpf;

    @NotBlank(message = "O código OTP é obrigatório.")
    @Pattern(regexp = "\\d{6}", message = "O código OTP deve conter 6 dígitos numéricos.")
    @Schema(description = "Código OTP recebido por e-mail.", example = "123456")
    private String codigoOtp;
}