package mppb.restapi.mobile.feature.concessaofolga.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class AusenciaCalendarioDto {
    private String id;
    private String tipo;
    private String descricao;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private String status;
    private String observacao;
}
