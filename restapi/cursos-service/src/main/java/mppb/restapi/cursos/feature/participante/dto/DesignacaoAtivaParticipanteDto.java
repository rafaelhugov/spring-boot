package mppb.restapi.cursos.feature.participante.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DesignacaoAtivaParticipanteDto {

    private Integer designacaoId;
    private String tipoLetra;
    private String tipoDescricao;
    private String funcaoNome;
    private String orgaoNome;
    private String orgaoExecucaoNome;
    private String atoNumero;
    private LocalDate dataInicial;
    private LocalDate dataFinal;
    private LocalDate dataPublicacao;
}
