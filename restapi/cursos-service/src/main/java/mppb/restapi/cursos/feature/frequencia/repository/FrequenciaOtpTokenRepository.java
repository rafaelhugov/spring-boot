package mppb.restapi.cursos.feature.frequencia.repository;

import mppb.restapi.common.core.repository.BaseRepository;
import mppb.restapi.cursos.feature.frequencia.entity.Chamada;
import mppb.restapi.cursos.feature.frequencia.entity.FrequenciaOtpToken;
import mppb.restapi.cursos.feature.inscricao.entity.Inscricao;

import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FrequenciaOtpTokenRepository extends BaseRepository<FrequenciaOtpToken, Long> {

    Optional<FrequenciaOtpToken> findFirstByChamadaAndInscricaoOrderByDataCriacaoDesc(Chamada chamada, Inscricao inscricao);

}
