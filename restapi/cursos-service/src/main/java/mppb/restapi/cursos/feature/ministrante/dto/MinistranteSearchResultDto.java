package mppb.restapi.cursos.feature.ministrante.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para representar um resultado unificado da busca por ministrantes,
 * que pode ser tanto um servidor interno (ainda não cadastrado como
 * ministrante)
 * quanto um ministrante externo já existente.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MinistranteSearchResultDto {

    // Dados de identificação
    private Long ministranteId;
    private Integer idServidor;

    // Dados comuns
    private String nome;
    private String tipo;

    // Dados de ministrante EXTERNO
    private String cpf;
    private String nomeOrgaoExterno;

    // Dados de ministrante INTERNO
    private String cargo;
    private String servidorMatricula;
    private String situacao;
    private String email;
    private String telefone;

    // Arquivos
    private Long idFoto;
    private Long idCurriculo;
}
