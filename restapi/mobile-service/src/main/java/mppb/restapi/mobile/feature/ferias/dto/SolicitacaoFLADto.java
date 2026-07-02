package mppb.restapi.mobile.feature.ferias.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitacaoFLADto {
    private Long id;
    private Date dataInicio;
    private Date dataFim;
    private String status;
    private String tipo;
    private String justificativa;
    private String statusExtenso;
    private String tipoExtenso;
}
