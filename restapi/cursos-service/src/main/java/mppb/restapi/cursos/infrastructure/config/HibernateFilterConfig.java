//package mppb.restapi.cursos.infrastructure.config;
//
//import jakarta.annotation.PostConstruct;
//import jakarta.persistence.EntityManager;
//import jakarta.transaction.Transactional;
//import org.hibernate.Session;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class HibernateFilterConfig {
//
//    @Autowired
//    private EntityManager entityManager;
//
//    @PostConstruct
//    @Transactional
//    public void enableFilters() {
//        Session session = entityManager.unwrap(Session.class);
//        session.enableFilter("filtroExcluidos");
//    }
//}
