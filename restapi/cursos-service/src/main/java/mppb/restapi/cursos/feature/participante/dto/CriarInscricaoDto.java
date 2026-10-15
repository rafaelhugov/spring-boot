package mppb.restapi.cursos.feature.participante.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * DTO para representar a requisição de criação de uma nova inscrição.
 * Contém todos os campos possíveis para identificar ou criar um participante,
 * alinhado com a lógica do findOrCreateParticipante.
 */
@Data
public class CriarInscricaoDto {

    // --- Campo Chave para a Lógica ---
    @NotBlank(message = "O tipo de participante (INTERNO/EXTERNO) é obrigatório.")
    private String tipo;

    // --- Campos para identificar um participante ---
    private Integer idServidor;    // Para um participante INTERNO já existente.
    private Long participanteId; // Para um participante EXTERNO já existente.

    // --- Campos para CRIAR um novo participante EXTERNO ---
    private String nome;

    @Pattern(regexp = "^$|\\d{11}", message = "CPF deve conter 11 dígitos.")
    private String cpf;

    @Email(message = "Formato de e-mail inválido.")
    private String email;

    private String telefone;

    private Boolean pcd;

    private String necessidadeEspecial;

    private Boolean declaracaoCriteriosPublicoAlvo;
}
