package mppb.restapi.mobile.feature.biometria.dto;

import java.time.LocalDateTime;

public record AutenticarBiometriaResponse(
        String token,
        LocalDateTime credencialExpiraEm) {
}
