package mppb.restapi.cursos.feature.curso.dto;

import lombok.Data;

@Data
public class OficioCorregedoriaRequestDto {
    private String numeroOficio;
    private String anoOficio;
    private String assunto;
    private String texto;
}
