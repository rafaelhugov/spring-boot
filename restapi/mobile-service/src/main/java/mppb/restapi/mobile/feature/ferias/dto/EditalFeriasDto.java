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
public class EditalFeriasDto {
    private Long id;
    private String edital;
    private Date dataInicioGozo;
    private Date dataFimGozo;
    private String tipo;
}
