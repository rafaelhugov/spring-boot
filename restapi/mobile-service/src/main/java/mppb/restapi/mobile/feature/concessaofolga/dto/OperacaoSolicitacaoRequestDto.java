package mppb.restapi.mobile.feature.concessaofolga.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class OperacaoSolicitacaoRequestDto {

    private String dataAdiamento;
    private String dataAntecipacao;
    private String dataInterrupcao;
    private List<SuspensaoParcialPeriodoDto> datasSuspensoes = new ArrayList<>();
}
