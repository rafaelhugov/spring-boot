package mppb.restapi.eleicao.feature.eleicao.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class EleicaoDto {
    private Long id;
    private String titulo;
    private LocalDate dataEleicao;
    private LocalDateTime dataInicial;
    private LocalDateTime dataFinal;

    @Pattern(regexp = "T|C", message = "O tipo de candidato deve ser 'T' (Tópico) ou 'C' (Candidato)")
    private String tipoCandidato;

    private Integer votosPorEleitor;
    private Integer ordem;
    private Integer numeroEleitoresTotal;
    private Integer numeroEleitoresVotaram;
    private Integer numeroVotosComputados;
}
