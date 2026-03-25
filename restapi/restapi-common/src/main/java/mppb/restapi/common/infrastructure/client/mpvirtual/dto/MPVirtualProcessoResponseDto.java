package mppb.restapi.common.infrastructure.client.mpvirtual.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MPVirtualProcessoResponseDto {
    @JsonProperty("numero")
    private String numeroProcesso; // e.g. "001.2023.000123"

    @JsonProperty("id")
    private Long idProcesso;

    private String status;
    private String mensagem;
}
