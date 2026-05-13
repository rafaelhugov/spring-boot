package mppb.restapi.eleicao.feature.candidato.repository;

import mppb.restapi.common.core.repository.BaseRepository;
import mppb.restapi.eleicao.feature.candidato.entity.Candidato;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidatoRepository extends BaseRepository<Candidato, Long>, JpaSpecificationExecutor<Candidato> {
    List<Candidato> findByEleicaoId(Long eleicaoId);
}
