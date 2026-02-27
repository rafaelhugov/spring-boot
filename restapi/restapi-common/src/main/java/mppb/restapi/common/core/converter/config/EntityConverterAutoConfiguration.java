//package mppb.restapi.common.core.converter.config;
//
//import mppb.restapi.common.core.converter.enums.DtoFor;
//import org.springframework.beans.BeansException;
//import org.springframework.beans.factory.config.BeanDefinition;
//import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
//import org.springframework.beans.factory.config.RuntimeBeanReference;
//import org.springframework.beans.factory.support.BeanDefinitionRegistry;
//import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
//import org.springframework.beans.factory.support.GenericBeanDefinition;
//import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
//import org.springframework.core.type.filter.AnnotationTypeFilter;
//import org.springframework.stereotype.Component;
//import org.springframework.util.StringUtils;
//
//@Component
//public class EntityConverterAutoConfiguration implements BeanDefinitionRegistryPostProcessor {
//
//    private static final String BASE_PACKAGE = "mppb.restapi";
//
//    @Override
//    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
//        System.out.println(">>> Iniciando auto-configuração de EntityConverters (versão FactoryBean)...");
//
//        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
//        scanner.addIncludeFilter(new AnnotationTypeFilter(DtoFor.class));
//
//        for (BeanDefinition bd : scanner.findCandidateComponents(BASE_PACKAGE)) {
//            try {
//                Class<?> dtoClass = Class.forName(bd.getBeanClassName());
//                DtoFor annotation = dtoClass.getAnnotation(DtoFor.class);
//                Class<?> entityClass = annotation.value();
//
//                System.out.printf(">>> Encontrado par DTO/Entity: [%s, %s]%n", dtoClass.getSimpleName(), entityClass.getSimpleName());
//
//                // 1. Cria a definição para a nossa FÁBRICA de beans
//                GenericBeanDefinition factoryBeanDefinition = new GenericBeanDefinition();
//                factoryBeanDefinition.setBeanClass(EntityConverterFactoryBean.class); // Aponta para a fábrica
//
//                // 2. Injeta as propriedades necessárias na fábrica
//                factoryBeanDefinition.getPropertyValues().add("dtoClass", dtoClass);
//                factoryBeanDefinition.getPropertyValues().add("entityClass", entityClass);
//
//                // 3. Injeta a dependência do ModelMapper na fábrica.
//                // Assumimos que você tem um bean ModelMapper com o nome "modelMapper".
//                // Se o nome for outro, altere aqui.
//                factoryBeanDefinition.getPropertyValues().add("modelMapper", new RuntimeBeanReference("modelMapper"));
//
//
//                // 4. Gera um nome único para o bean e o registra
//                String beanName = String.format("%sTo%sConverter", StringUtils.uncapitalize(dtoClass.getSimpleName()), entityClass.getSimpleName());
//                registry.registerBeanDefinition(beanName, factoryBeanDefinition);
//
//                System.out.printf(">>> Bean Factory '%s' para DtoConverter<%s, %s> registrada com sucesso!%n",
//                        beanName, dtoClass.getSimpleName(), entityClass.getSimpleName());
//
//            } catch (ClassNotFoundException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    @Override
//    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
//        // Não é necessário implementar
//    }
//}
