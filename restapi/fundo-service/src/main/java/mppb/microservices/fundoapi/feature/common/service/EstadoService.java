package mppb.microservices.fundoapi.feature.common.service;

import jakarta.persistence.EntityManager;
import mppb.microservices.common.core.service.AbstractService;
import mppb.microservices.fundoapi.feature.common.entities.Estado;
import mppb.microservices.fundoapi.feature.common.repository.EstadoRepository;
import org.springframework.stereotype.Service;

@Service
public class EstadoService extends AbstractService<Estado, Long> {

    private final EstadoRepository estadoRepository;

    private final EntityManager entityManager;

    public EstadoService(EntityManager entityManager, EstadoRepository estadoRepository){
        super(entityManager, estadoRepository);
        this.entityManager = entityManager;
        this.estadoRepository = estadoRepository;
    }

}
