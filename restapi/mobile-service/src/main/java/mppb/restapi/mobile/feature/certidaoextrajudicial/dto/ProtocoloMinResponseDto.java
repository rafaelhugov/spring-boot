package mppb.restapi.mobile.feature.certidaoextrajudicial.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProtocoloMinResponseDto {
    private String numero;
    private String classe;
    private String assunto;
}
