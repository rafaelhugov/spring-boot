package mppb.restapi.cursos.feature.carteirafuncional.dto;

import lombok.Data;

@Data
public class ArquivoTO {
    private Long id;
    private byte[] bytes;
    private String pathrepositorio;
    private Boolean estanorepositorio;
}
