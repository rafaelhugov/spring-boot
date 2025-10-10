package mppb.restapi.cursos.feature.common.projection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public interface ServidorProjection {

    Integer getId();
    String getNome();
    String getCargoNome();
    String getTipoCargoNome();
    String getServidorMatricula();
    String getSituacao();

}
