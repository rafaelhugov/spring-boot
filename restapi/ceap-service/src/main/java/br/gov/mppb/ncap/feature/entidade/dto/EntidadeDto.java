package br.gov.mppb.ncap.feature.entidade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.*;
import mppb.restapi.common.core.validation.groups.Create;
import mppb.restapi.common.core.validation.groups.Update;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntidadeDto {

    @NotNull(groups = {Update.class})
    @Null(groups = {Create.class})
    private Long id;

    private Long idCnmp;

    @NotBlank(message = "O nome CNMP é obrigatório.")
    @Size(max = 255, message = "O nome CNMP não pode exceder 255 caracteres.")
    private String nomecnmp;

    @Size(max = 255, message = "A descrição MPPB não pode exceder 255 caracteres.")
    private String descricaomppb;

    @NotBlank(message = "O tipo da entidade é obrigatório.")
    @Size(max = 50, message = "O tipo de entidade não pode exceder 50 caracteres.")
    private String tipoentidade;

    @NotNull(message = "A comarca é obrigatória.")
    private Long idComarca;

    /** Somente leitura — populado via @Formula na entidade Entidade. */
    private String nomeComarca;

    @NotBlank(message = "O município é obrigatório.")
    @Size(max = 255, message = "O município não pode exceder 255 caracteres.")
    private String municipio;

    private Boolean ativa;

    private Instant criadoem;
}
