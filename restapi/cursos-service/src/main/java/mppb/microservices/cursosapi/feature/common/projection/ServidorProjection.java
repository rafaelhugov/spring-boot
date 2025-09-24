package mppb.microservices.cursosapi.feature.common.projection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServidorProjection {
    private Integer id;
    private String matricula;
    private String nome;
    private String cargo;
    private String tipoCargoNome;

}
