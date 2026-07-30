package mppb.restapi.mobile.feature.concessaofolga.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class SolicitacaoConcessaoDto {
    private Long id;
    private String tipo;
    private String tipoOriginal;
    private LocalDate inicio;
    private LocalDate fim;
    private String status;
    private String statusOriginal;
    private Integer diasInformados;
}
