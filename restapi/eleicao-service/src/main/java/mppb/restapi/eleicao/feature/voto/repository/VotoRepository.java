package mppb.restapi.eleicao.feature.voto.repository;

import mppb.restapi.common.core.repository.BaseRepository;
import mppb.restapi.eleicao.feature.voto.entity.Voto;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VotoRepository extends BaseRepository<Voto, Long>, JpaSpecificationExecutor<Voto> {
    long countByEleicaoId(Long eleicaoId);
    long countByEleicaoIdAndTipo(Long eleicaoId, String tipo);
    List<Voto> findByEleicaoIdAndTipo(Long eleicaoId, String tipo);
}
