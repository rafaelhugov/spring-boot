package mppb.restapi.mobile.feature.concessaofolga.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class OperacaoSolicitacaoRequestDto {

    private String dataAdiamento;
    private String dataAntecipacao;
    private String dataInterrupcao;
    private List<SuspensaoParcialPeriodoDto> datasSuspensoes = new ArrayList<>();
}
