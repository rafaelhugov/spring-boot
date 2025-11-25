package mppb.microservices.fundoapi.feature.cobranca.service;

import jakarta.persistence.EntityManager;
import mppb.microservices.common.core.service.AbstractService;
import mppb.microservices.fundoapi.feature.cobranca.entity.Natureza;
import mppb.microservices.fundoapi.feature.cobranca.repository.NaturezaRepository;
import org.springframework.stereotype.Service;

@Service
public class NaturezaService extends AbstractService<Natureza, Long> {

    private final NaturezaRepository naturezaRepository;

    private final EntityManager entityManager;

    public NaturezaService(EntityManager entityManager, NaturezaRepository naturezaRepository){
        super(entityManager, naturezaRepository);
        this.entityManager = entityManager;
        this.naturezaRepository = naturezaRepository;
    }

}
