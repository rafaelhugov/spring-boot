package mppb.restapi.mobile.feature.noticias.model;

import java.time.LocalDateTime;

import mppb.restapi.mobile.feature.noticias.dto.NoticiaDto;

public record NoticiaPublicada(
        String chaveExterna,
        String titulo,
        String link,
        String resumo,
        String imagemUrl,
        LocalDateTime dataPublicacao,
        String dataPublicacaoFormatada,
        String categoria) {

    public NoticiaDto toDto() {
        return NoticiaDto.builder()
                .titulo(titulo)
                .link(link)
                .resumo(resumo)
                .imagemUrl(imagemUrl)
                .dataPublicacao(dataPublicacaoFormatada)
                .categoria(categoria)
                .build();
    }
}
