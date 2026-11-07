package mppb.restapi.mobile.feature.instagram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstagramPostDto {
    private String id;
    private String titulo;
    private String caption;

    @JsonProperty("media_url")
    private String mediaUrl;

    private String permalink;

    @JsonProperty("media_type")
    private String mediaType;

    @JsonProperty("data_formatada")
    private String dataFormatada;

    @JsonProperty("dias_atras")
    private Integer diasAtras;

    private Integer likes;
    private Integer comments;
    private String views;
    private Boolean isFixado;
}
