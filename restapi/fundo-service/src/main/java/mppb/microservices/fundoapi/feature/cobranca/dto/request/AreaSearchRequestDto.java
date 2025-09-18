package mppb.microservices.fundoapi.feature.cobranca.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class AreaSearchRequestDto {

    @NotBlank
    private String termo;
}
