package mppb.microservices.fundoapi.feature.cobranca.service;

import jakarta.persistence.EntityManager;
import mppb.microservices.common.core.service.AbstractService;
import mppb.microservices.fundoapi.feature.cobranca.dto.AreaDto;
import mppb.microservices.fundoapi.feature.cobranca.entity.Area;
import mppb.microservices.fundoapi.feature.cobranca.projection.AreaSelectProjection;
import mppb.microservices.fundoapi.feature.cobranca.repository.AreaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AreaService extends AbstractService<Area, Long> {

    private final AreaRepository areaRepository;

    private final EntityManager entityManager;

    public AreaService(EntityManager entityManager, AreaRepository areaRepository){
        super(entityManager, areaRepository);
        this.entityManager = entityManager;
        this.areaRepository = areaRepository;
    }

    public Page<Area> findByNomeContainingIgnoreCase(String termo, Pageable pageable) {
        return areaRepository.findByNomeContainingIgnoreCase(termo, pageable);
    }

    public List<AreaSelectProjection> findSelectAll() {
        return areaRepository.findSelectAll();
    }

    public List<AreaDto> findSelectNative() {
        return entityManager.createNativeQuery("SELECT id, nome FROM fundodd.area", "AreaDtoMapping")
                .getResultList();
    }

    public List<AreaDto> findSelectJpql() {
        return areaRepository.findSelectJpql();
    }
}
