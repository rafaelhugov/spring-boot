package mppb.restapi.cursos.feature.inscricao.enums;

import lombok.Getter;

import java.util.stream.Stream;

/**
 * Enum para representar os possíveis status de uma inscrição em um curso.
 * A lógica de conversão de/para o banco de dados foi movida para o SituacaoInscricaoEnumConverter.
 */
@Getter
public enum SituacaoInscricaoEnum {

    INSCRITO('I', "INSCRITO"),
    APROVADO('A', "APROVADO"),
    REPROVADO('R', "REPROVADO"),
    CANCELADO('C', "CANCELADO"),
    LISTA_DE_ESPERA('E', "LISTA DE ESPERA");

    private final Character codigo;
    private final String descricao;

    SituacaoInscricaoEnum(Character codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    /**
     * Método de busca estático para ser usado pelo serviço.
     * @param codigo O código a ser procurado.
     * @return A enum correspondente.
     * @throws IllegalArgumentException se o código não for encontrado.
     */
    public static SituacaoInscricaoEnum fromCodigo(Character codigo) {
        if (codigo == null) {
            throw new IllegalArgumentException("Código de situação não pode ser nulo.");
        }
        return Stream.of(SituacaoInscricaoEnum.values())
                .filter(s -> s.getCodigo().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Código de situação inválido: " + codigo));
    }
}

