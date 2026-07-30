package mppb.restapi.mobile.feature.concessaofolga.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ConcessaoFolgaDto {
    private Long id;
    private String tipo;
    private String descricaoOriginal;
    private Integer diasConcedidos;
    private Integer saldoRestante;
    private Integer periodo;
    private Integer anoReferencia;
    private LocalDate inicioPeriodoAquisitivo;
    private LocalDate finalPeriodoAquisitivo;
    private List<SolicitacaoConcessaoDto> solicitacoes = new ArrayList<>();
}
