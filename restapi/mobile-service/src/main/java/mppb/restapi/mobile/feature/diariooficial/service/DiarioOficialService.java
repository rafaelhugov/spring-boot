package mppb.restapi.mobile.feature.diariooficial.service;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.service.AbstractService;
import mppb.restapi.mobile.feature.diariooficial.DiarioOficialRepository;
import mppb.restapi.mobile.feature.diariooficial.entity.DiarioOficial;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@Transactional
public class DiarioOficialService  extends AbstractService<DiarioOficial, Long> {

    private final DiarioOficialRepository diarioOficialRepository;

    public DiarioOficialService(EntityManager entityManager, DiarioOficialRepository diarioOficialRepository) {
        super(entityManager, diarioOficialRepository);
        this.diarioOficialRepository = diarioOficialRepository;
    }

    public List<DiarioOficial> recuperarDiariosOficiaisRecentes() {
        return diarioOficialRepository.recuperarDiariosOficiaisRecentes();
    }
}
