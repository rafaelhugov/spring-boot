package mppb.restapi.mobile.feature.biometria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AutenticarBiometriaRequest(
        @NotBlank @Size(max = 512) String credencial) {
}
