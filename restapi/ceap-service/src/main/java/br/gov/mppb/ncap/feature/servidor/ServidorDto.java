package br.gov.mppb.ncap.feature.servidor;

import lombok.*;

/**
 * DTO de retorno para o frontend ao buscar servidores/membros do MPPB.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServidorDto {

    private Long id;
    private String nome;
    private String cargoNome;
    /** "Servidor" ou "Membro". */
    private String tipoCargoNome;
    private String servidorMatricula;
    private String situacao;
    private String email;
}
