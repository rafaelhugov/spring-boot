package mppb.restapi.common.core.converter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Interface que define o contrato para conversores entre um DTO (Data Transfer Object) e uma Entidade.
 *
 * @param <D> O tipo do DTO.
 * @param <E> O tipo da Entidade.
 */
public interface DtoConverter<D, E> {

    /**
     * Converte uma Entidade para seu respectivo DTO.
     *
     * @param entity A entidade a ser convertida.
     * @return O DTO resultante.
     */
    D toDto(E entity);

    /**
     * Converte um DTO para sua respectiva Entidade.
     *
     * @param dto O DTO a ser convertido.
     * @return A Entidade resultante.
     */
    E toEntity(D dto);

    /**
     * Converte uma lista de Entidades para uma lista de DTOs.
     * <p>
     * A implementação padrão itera sobre a lista e aplica o método {@link #toDto(Object)}.
     *
     * @param entities A lista de entidades a ser convertida.
     * @return A lista de DTOs resultante.
     */
    default List<D> toDto(List<E> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Converte uma lista de DTOs para uma lista de Entidades.
     * <p>
     * A implementação padrão itera sobre a lista e aplica o método {@link #toEntity(Object)}.
     *
     * @param dtos A lista de DTOs a ser convertida.
     * @return A lista de Entidades resultante.
     */
    default List<E> toEntity(List<D> dtos) {
        if (dtos == null) {
            return null;
        }
        return dtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }
}