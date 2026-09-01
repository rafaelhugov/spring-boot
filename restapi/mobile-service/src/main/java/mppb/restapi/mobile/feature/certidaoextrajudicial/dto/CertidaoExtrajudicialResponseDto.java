package mppb.restapi.mobile.feature.certidaoextrajudicial.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CertidaoExtrajudicialResponseDto {
    private Long id;
    private String codigoVerificador;
    private String tipoDocumento;
    private String documentoFormatado;
    private String nome;
    private LocalDateTime data;
    private Boolean negativa;
    private String status;
    private List<ProtocoloMinResponseDto> protocolos;
}
