package mppb.restapi.cursos.feature.frequencia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class AulaDto {
    private Long id;

    @NotBlank(message = "A descrição da aula é obrigatória.")
    private String descricao;

    @NotNull(message = "A data é obrigatória.")
    private LocalDate data;

    @NotNull(message = "A hora de início é obrigatória.")
    private LocalTime horaInicio;

    @NotNull(message = "A hora de fim é obrigatória.")
    private LocalTime horaFim;

    private boolean ativa;
    private List<ChamadaDto> chamadas;
}