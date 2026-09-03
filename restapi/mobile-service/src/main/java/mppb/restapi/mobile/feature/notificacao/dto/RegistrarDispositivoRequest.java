package mppb.restapi.mobile.feature.notificacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrarDispositivoRequest(
        @Size(max = 64)
        String identificador,

        @NotBlank
        @Size(max = 4096)
        String tokenFcm,

        @NotBlank
        @Pattern(regexp = "ANDROID|IOS", flags = Pattern.Flag.CASE_INSENSITIVE)
        String plataforma) {
}
