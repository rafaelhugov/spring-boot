package mppb.restapi.common.infrastructure.client.mpvirtual.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MPVirtualMovimentarRequestDto {
    private Long idMovimento;
    private Long idMinuta;
}
