package mppb.restapi.mobile.feature.videos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mppb.restapi.mobile.feature.videos.model.TipoVideo;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoDto {
    private String id;
    private String titulo;
    private String descricao;
    private String link;
    private String thumbnailUrl;
    private String dataPublicacao;
    private String dataPublicacaoIso;
    private TipoVideo tipo;
    private boolean isAoVivo;
    private String duracao;
    private String visualizacoes;
}
