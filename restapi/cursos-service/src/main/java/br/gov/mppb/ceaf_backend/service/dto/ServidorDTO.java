package br.gov.mppb.ceaf_backend.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServidorDTO {
    private String tipoCargoNome; // NOVO CAMPO
    private Integer id;
    private String matricula;
    private String nome;
    private String cargo;
}
