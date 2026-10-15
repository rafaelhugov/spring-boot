package mppb.restapi.mobile.feature.biometria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AtivarBiometriaRequest(
        @NotBlank @Size(max = 64) String dispositivoId,
        @NotBlank @Pattern(regexp = "ANDROID|IOS") String plataforma) {
}
