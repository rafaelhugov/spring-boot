package mppb.restapi.cursos.feature.participante.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO para receber os dados do formulário de inscrição público.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class InscricaoPublicaDto {


    // Se este ID for enviado, significa que o participante já existe e estamos a reutilizá-lo.
    private Long participanteId;

    // Campos para criar um novo participante, se o ID não for enviado.
    // As anotações @NotBlank foram removidas daqui para permitir validação condicional no serviço.
    private String nome;

    @NotBlank(message = "O CPF é obrigatório.")
    @Size(min = 11, max = 11, message = "CPF deve conter 11 dígitos.")
    private String cpf;

    @Email(message = "Formato de e-mail inválido.")
    private String email;

    private String telefone;
}

