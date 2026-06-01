package mppb.restapi.cursos.feature.noticias.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticiaDto {
    private String titulo;
    private String link;
    private String resumo;
    private String imagemUrl;
    private String dataPublicacao;
    private String categoria;
}
