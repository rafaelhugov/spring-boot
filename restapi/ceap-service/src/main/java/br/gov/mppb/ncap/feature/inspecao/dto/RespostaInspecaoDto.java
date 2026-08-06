package br.gov.mppb.ncap.feature.inspecao.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespostaInspecaoDto {

    private Long id;

    @NotNull(message = "A inspeção é obrigatória.")
    private Long idInspecao;

    @NotNull(message = "A pergunta é obrigatória.")
    private Long idPergunta;

    private String valorresposta;

    private String observacaoprevisita;

    private String observacaocorregedoria;
}
