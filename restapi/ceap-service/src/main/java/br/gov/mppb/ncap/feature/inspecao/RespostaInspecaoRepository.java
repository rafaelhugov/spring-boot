package br.gov.mppb.ncap.feature.inspecao;

import mppb.restapi.common.core.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RespostaInspecaoRepository extends BaseRepository<RespostaInspecao, Long> {

    List<RespostaInspecao> findByInspecaoId(Long inspecaoId);

    Optional<RespostaInspecao> findByInspecaoIdAndPerguntaId(Long inspecaoId, Long perguntaId);
}
