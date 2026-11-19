package mppb.restapi.mobile.feature.mensagem.dto;

import java.time.LocalDateTime;

import mppb.restapi.mobile.feature.mensagem.entity.MensagemDestinatario;
import mppb.restapi.mobile.feature.mensagem.enums.StatusDestinatario;

public record DestinatarioStatusResponse(
        String matricula,
        String nome,
        String setor,
        StatusDestinatario status,
        LocalDateTime dataEntrega,
        LocalDateTime dataLeitura,
        LocalDateTime dataCiencia) {

    public static DestinatarioStatusResponse from(MensagemDestinatario dest) {
        return new DestinatarioStatusResponse(
                dest.getDestinatarioMatricula(),
                dest.getDestinatarioNome(),
                dest.getDestinatarioSetor(),
                dest.getStatus(),
                dest.getDataEntrega(),
                dest.getDataLeitura(),
                dest.getDataCiencia());
    }
}

