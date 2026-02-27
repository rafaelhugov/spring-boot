//package mppb.restapi.common.core.converter.enums;
//
//
//import java.lang.annotation.ElementType;
//import java.lang.annotation.Retention;
//import java.lang.annotation.RetentionPolicy;
//import java.lang.annotation.Target;
//
///**
// * Anotação para marcar uma classe DTO e associá-la a uma classe de Entidade.
// * Usada para auto-configurar beans de DtoConverter.
// */
//@Target(ElementType.TYPE) // Pode ser usada apenas em classes/interfaces
//@Retention(RetentionPolicy.RUNTIME) // Disponível em tempo de execução para reflection
//public @interface DtoFor {
//    /**
//     * A classe da Entidade à qual este DTO está associado.
//     */
//    Class<?> value();
//}