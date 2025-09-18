package mppb.microservices.fundoapi.feature.common.service;

import jakarta.persistence.EntityManager;
import mppb.microservices.common.service.AbstractService;
import mppb.microservices.fundoapi.feature.common.entities.Pessoa;
import mppb.microservices.fundoapi.feature.common.repository.PessoaRepository;
import org.springframework.stereotype.Service;

@Service
public class PessoaService extends AbstractService<Pessoa, Long> {

    private final PessoaRepository pessoaRepository;

    private final EntityManager entityManager;

    public PessoaService(EntityManager entityManager, PessoaRepository pessoaRepository){
        super(entityManager, pessoaRepository);
        this.entityManager = entityManager;
        this.pessoaRepository = pessoaRepository;
    }

}
