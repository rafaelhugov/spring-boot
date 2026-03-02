package mppb.restapi.mobile.feature.cadastro.service;

import jakarta.persistence.EntityManager;
import mppb.restapi.common.core.service.AbstractService;
import mppb.restapi.mobile.feature.cadastro.entities.EntidadeTutora;
import mppb.restapi.mobile.feature.cadastro.repository.EntidadeTutoraRepository;
import mppb.restapi.mobile.feature.lookup.specification.EntidadeTutoraSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EntidadeTutoraService extends AbstractService<EntidadeTutora, Long> {

    private EntidadeTutoraRepository entidadeTutoraRepository;

    private final EntidadeTutoraSpecification specification;

    public EntidadeTutoraService(EntityManager entityManager, EntidadeTutoraRepository entidadeTutoraRepository, EntidadeTutoraSpecification specification) {
        super(entityManager, entidadeTutoraRepository);
        this.entidadeTutoraRepository = entidadeTutoraRepository;
        this.specification = specification;
    }

    public Page<EntidadeTutora> findAll(String nome, Pageable pageable) {
        return entidadeTutoraRepository.findAll(specification.comFiltros(nome), pageable);
    }


}
