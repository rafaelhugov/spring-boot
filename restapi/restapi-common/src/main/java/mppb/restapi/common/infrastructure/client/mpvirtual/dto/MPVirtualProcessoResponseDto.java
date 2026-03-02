package mppb.restapi.common.infrastructure.client.mpvirtual.dto;

import lombok.Data;

@Data
public class MPVirtualProcessoResponseDto {
    private String numeroProcesso; // e.g. "001.2023.000123"
    private Long idProcesso;
    private String status;
    private String mensagem;
}
