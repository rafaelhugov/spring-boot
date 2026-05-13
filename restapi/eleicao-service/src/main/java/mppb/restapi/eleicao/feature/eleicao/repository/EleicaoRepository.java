package mppb.restapi.eleicao.feature.eleicao.repository;

import mppb.restapi.common.core.repository.BaseRepository;
import mppb.restapi.eleicao.feature.eleicao.entity.Eleicao;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface EleicaoRepository extends BaseRepository<Eleicao, Long>, JpaSpecificationExecutor<Eleicao> {
}
