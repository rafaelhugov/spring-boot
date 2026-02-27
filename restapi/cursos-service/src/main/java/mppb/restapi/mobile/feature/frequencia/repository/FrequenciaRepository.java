package mppb.restapi.mobile.feature.frequencia.repository;

import mppb.restapi.common.core.repository.BaseRepository;
import mppb.restapi.mobile.feature.frequencia.entity.Chamada;
import mppb.restapi.mobile.feature.frequencia.entity.Frequencia;
import mppb.restapi.mobile.feature.inscricao.entity.Inscricao;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface FrequenciaRepository extends BaseRepository<Frequencia, Long> {

    // Verifica se um aluno já registou presença numa chamada específica
    Optional<Frequencia> findByChamadaAndInscricao(Chamada chamada, Inscricao inscricao);

    boolean existsByChamadaAndInscricao(Chamada chamada, Inscricao inscricao);

    List<Frequencia> findAllByChamadaAulaId(Integer aulaId);
}