package mppb.microservices.fundoapi.feature.common.service;

import jakarta.persistence.EntityManager;
import mppb.microservices.common.core.service.AbstractService;
import mppb.microservices.fundoapi.feature.common.entities.Comarca;
import mppb.microservices.fundoapi.feature.common.repository.ComarcaRepository;
import org.springframework.stereotype.Service;

@Service
public class ComarcaService extends AbstractService<Comarca, Long> {

    private final ComarcaRepository comarcaRepository;

    private final EntityManager entityManager;

    public ComarcaService(EntityManager entityManager, ComarcaRepository comarcaRepository){
        super(entityManager, comarcaRepository);
        this.entityManager = entityManager;
        this.comarcaRepository = comarcaRepository;
    }

}
