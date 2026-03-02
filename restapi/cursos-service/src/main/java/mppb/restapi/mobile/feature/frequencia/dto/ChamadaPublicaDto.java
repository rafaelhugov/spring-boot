package mppb.restapi.mobile.feature.frequencia.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO específico para a tela pública de registro de frequência.
 * Contém apenas as informações essenciais para identificação do evento (Aula + Curso).
 */
@Data
@Builder
public class ChamadaPublicaDto {

    // Dados do Curso
    private String cursoTitulo;

    // Dados da Aula
    private String aulaDescricao;
    private LocalDate data;
    private LocalTime horaInicio;
    private LocalTime horaFim;
}