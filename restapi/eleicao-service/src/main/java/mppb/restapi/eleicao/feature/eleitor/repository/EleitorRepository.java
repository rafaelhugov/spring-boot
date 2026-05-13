package mppb.restapi.eleicao.feature.eleitor.repository;

import mppb.restapi.common.core.repository.BaseRepository;
import mppb.restapi.eleicao.feature.eleitor.entity.Eleitor;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EleitorRepository extends BaseRepository<Eleitor, Long>, JpaSpecificationExecutor<Eleitor> {
    Optional<Eleitor> findByEleicaoIdAndIdServidor(Long eleicaoId, Long idServidor);
    long countByEleicaoId(Long eleicaoId);
}
