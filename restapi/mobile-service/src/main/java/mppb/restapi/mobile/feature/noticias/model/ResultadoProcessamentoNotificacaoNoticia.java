package mppb.restapi.mobile.feature.noticias.model;

import mppb.restapi.mobile.feature.notificacao.model.ResultadoEnvioFcm;

public record ResultadoProcessamentoNotificacaoNoticia(
        Long id,
        String statusAnterior,
        String statusFinal,
        int quantidadeTentativasAnterior,
        String mensagemErro,
        ResultadoEnvioFcm resultadoEnvio) {
}
