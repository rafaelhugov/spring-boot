//package mppb.restapi.common.core.converter.config;
//
//import mppb.restapi.common.core.converter.DtoConverter;
//import mppb.restapi.common.core.converter.EntityConverterImpl;
//import org.modelmapper.ModelMapper;
//import org.springframework.beans.factory.FactoryBean;
//import org.springframework.beans.factory.InitializingBean;
//import org.springframework.core.ResolvableType;
//import org.springframework.util.Assert;
//
//public class EntityConverterFactoryBean implements FactoryBean<DtoConverter<?, ?>>, InitializingBean {
//
//
//    private ModelMapper modelMapper;
//    private Class<?> dtoClass;
//    private Class<?> entityClass;
//
//    private DtoConverter<?, ?> instance;
//
//    // Getters e Setters são necessários para que o Spring possa injetar as propriedades
//    public void setModelMapper(ModelMapper modelMapper) {
//        this.modelMapper = modelMapper;
//    }
//
//    public void setDtoClass(Class<?> dtoClass) {
//        this.dtoClass = dtoClass;
//    }
//
//    public void setEntityClass(Class<?> entityClass) {
//        this.entityClass = entityClass;
//    }
//
//    @Override
//    public void afterPropertiesSet() throws Exception {
//        // Garante que todas as propriedades necessárias foram setadas antes de criar o objeto
//        Assert.notNull(modelMapper, "ModelMapper must not be null");
//        Assert.notNull(dtoClass, "dtoClass must not be null");
//        Assert.notNull(entityClass, "entityClass must not be null");
//
//        // Cria a instância do conversor aqui
//        // O cast aqui é seguro porque estamos controlando a criação.
//        this.instance = new EntityConverterImpl<>(modelMapper, dtoClass, entityClass);
//    }
//
//    @Override
//    public DtoConverter<?, ?> getObject() throws Exception {
//        return this.instance;
//    }
//
//    @Override
//    public Class<?> getObjectType() {
//        // Esta é a parte mais importante!
//        // Informamos ao Spring o tipo exato do bean que esta fábrica produz, incluindo os genéricos.
//        // Isso permite a injeção de dependência type-safe (ex: @Autowired DtoConverter<CursoDto, Curso>)
//        if (dtoClass == null || entityClass == null) {
//            return DtoConverter.class;
//        }
//        ResolvableType type = ResolvableType.forClassWithGenerics(DtoConverter.class, dtoClass, entityClass);
//        return type.toClass();
//    }
//
//    @Override
//    public boolean isSingleton() {
//        return true;
//    }
//}