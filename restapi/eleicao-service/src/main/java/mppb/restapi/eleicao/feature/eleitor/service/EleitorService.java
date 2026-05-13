package mppb.restapi.eleicao.feature.eleitor.service;

import jakarta.persistence.EntityManager;
import mppb.restapi.common.core.service.AbstractService;
import mppb.restapi.eleicao.feature.eleitor.entity.Eleitor;
import mppb.restapi.eleicao.feature.eleitor.repository.EleitorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class EleitorService extends AbstractService<Eleitor, Long> {

    private final EleitorRepository eleitorRepository;

    public EleitorService(EntityManager entityManager, EleitorRepository eleitorRepository) {
        super(entityManager, eleitorRepository);
        this.eleitorRepository = eleitorRepository;
    }

    public Optional<Eleitor> findByEleicaoAndServidor(Long eleicaoId, Long idServidor) {
        return eleitorRepository.findByEleicaoIdAndIdServidor(eleicaoId, idServidor);
    }
}
