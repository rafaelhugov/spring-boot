package mppb.restapi.cursos.feature.cadastro.service;

import jakarta.persistence.EntityManager;
import mppb.restapi.common.core.service.AbstractService;
import mppb.restapi.cursos.feature.cadastro.entities.EntidadeTutora;
import mppb.restapi.cursos.feature.cadastro.repository.EntidadeTutoraRepository;
import org.springframework.stereotype.Service;

@Service
public class EntidadeTutoraService extends AbstractService<EntidadeTutora, Long> {

    public EntidadeTutoraService(EntityManager entityManager, EntidadeTutoraRepository entidadeTutoraRepository) {
        super(entityManager, entidadeTutoraRepository);
    }

}
