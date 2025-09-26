package mppb.restapi.common.core.converter;

import lombok.RequiredArgsConstructor;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.spi.DestinationSetter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Function;

@RequiredArgsConstructor
public  class EntityConverterImpl<D, E> implements DtoConverter<D, E> {

    protected final ModelMapper modelMapper;

    private final Class<D> dtoClass;
    private final Class<E> entityClass;

    @SuppressWarnings("unchecked")
    public EntityConverterImpl(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;

        // Magia da reflexão acontece aqui! ✨
        Type genericSuperclass = getClass().getGenericSuperclass();
        ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
        Type[] typeArgs = parameterizedType.getActualTypeArguments();

        this.dtoClass = (Class<D>) typeArgs[0];
        this.entityClass = (Class<E>) typeArgs[1];
    }

    public D toDto(E entity) {
        return modelMapper.map(entity, dtoClass);
    }

    public List<D> toDto(List<E> entities) {
        return entities.stream()
                .map(this::toDto)
                .toList();
    }

    public E toEntity(D dto) {
        return modelMapper.map(dto, entityClass);
    }

    public List<E> toEntity(List<D> dtos) {
        return dtos.stream()
                .map(this::toEntity)
                .toList();
    }

    protected <S, T, E extends Enum<E>> void mapEnumField(
            ModelMapper mapper,
            Class<S> sourceClass,
            Class<T> targetClass,
            Function<S, E> getEnumValue,
            DestinationSetter<T, String> setCodigo,
            DestinationSetter<T, String> setDescricao,
            Function<E, String> getDescricaoFunc
    ) {
        Converter<E, String> toCodigo = ctx -> {
            E src = ctx.getSource();
            return src != null ? src.name() : null;
        };
        Converter<E, String> toDescricao = ctx -> {
            E src = ctx.getSource();
            return src != null ? getDescricaoFunc.apply(src) : null;
        };

        mapper.typeMap(sourceClass, targetClass).addMappings(mapperx -> {
            mapperx.using(toCodigo).map(src -> getEnumValue.apply(src), setCodigo);
            mapperx.using(toDescricao).map(src -> getEnumValue.apply(src), setDescricao);
        });
    }


}
