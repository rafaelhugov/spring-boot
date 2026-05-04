package mppb.restapi.common.infrastructure.client.mpvirtual.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MPVirtualCriarMinutaResponseDto {
    @JsonAlias({"id", "idMinuta"})
    private Long idMinuta;
}
