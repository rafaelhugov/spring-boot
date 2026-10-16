package mppb.restapi.mobile.feature.biometria.dto;

import java.time.LocalDateTime;

public record AtivarBiometriaResponse(
        String credencial,
        LocalDateTime expiraEm) {
}
