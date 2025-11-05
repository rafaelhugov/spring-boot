package mppb.microservices.fundoapi.feature.common.service;

import jakarta.persistence.EntityManager;
import mppb.microservices.common.core.service.AbstractService;
import mppb.microservices.fundoapi.feature.common.entities.Municipio;
import mppb.microservices.fundoapi.feature.common.repository.MunicipioRepository;
import org.springframework.stereotype.Service;

@Service
public class MunicipioService extends AbstractService<Municipio, Long> {

    private final MunicipioRepository municipioRepository;

    private final EntityManager entityManager;

    public MunicipioService(EntityManager entityManager, MunicipioRepository municipioRepository){
        super(entityManager, municipioRepository);
        this.entityManager = entityManager;
        this.municipioRepository = municipioRepository;
    }

}
