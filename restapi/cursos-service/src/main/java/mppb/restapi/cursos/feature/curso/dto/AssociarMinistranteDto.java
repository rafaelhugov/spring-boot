package mppb.restapi.cursos.feature.curso.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class AssociarMinistranteDto {

    // --- Identificadores ---
    private Long ministranteId; // Usado para associar um ministrante EXTERNO já existente
    private Integer idServidor; // Usado para associar um ministrante INTERNO
    private Long papelId; // O ID do papel que o ministrante terá no curso

    // --- Campos para criação de NOVO ministrante EXTERNO ---
    private String nome;
    private String cpf;
    private String nomeOrgaoExterno;

    // --- CAMPO ADICIONADO PARA CORRIGIR O ERRO ---
    // Este campo diz ao backend como processar a requisição (INTERNO vs EXTERNO)
    private String tipo;

    private String email;
    private String telefone;
    private Long idFoto;
    private Long idCurriculo;

}
