package mppb.restapi.mobile.feature.mensagem.dto;

import mppb.restapi.mobile.feature.mensagem.entity.MensagemAnexo;
import mppb.restapi.mobile.feature.mensagem.enums.CategoriaAnexo;

public record MensagemAnexoResponse(
        Long id,
        CategoriaAnexo categoria,
        String nomeOriginal,
        String mimeType,
        Long tamanhoBytes,
        Integer duracaoSegundos,
        boolean possuiThumbnail) {

    public static MensagemAnexoResponse from(MensagemAnexo anexo) {
        return new MensagemAnexoResponse(
                anexo.getId(),
                anexo.getCategoria(),
                anexo.getNomeOriginal(),
                anexo.getMimeType(),
                anexo.getTamanhoBytes(),
                anexo.getDuracaoSegundos(),
                anexo.getCaminhoThumbnail() != null && !anexo.getCaminhoThumbnail().isBlank());
    }
}

