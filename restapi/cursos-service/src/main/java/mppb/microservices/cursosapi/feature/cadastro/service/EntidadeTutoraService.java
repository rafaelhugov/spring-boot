package mppb.microservices.cursosapi.feature.cadastro.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import mppb.microservices.common.core.service.AbstractService;
import mppb.microservices.cursosapi.feature.cadastro.entities.EntidadeTutora;
import mppb.microservices.cursosapi.feature.cadastro.repository.EntidadeTutoraRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EntidadeTutoraService extends AbstractService<EntidadeTutora, Long> {

    public EntidadeTutoraService(EntityManager entityManager, EntidadeTutoraRepository entidadeTutoraRepository) {
        super(entityManager, entidadeTutoraRepository);
    }

}
