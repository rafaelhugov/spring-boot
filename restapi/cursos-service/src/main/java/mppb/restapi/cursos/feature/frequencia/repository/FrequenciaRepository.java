package mppb.restapi.cursos.feature.frequencia.repository;

import mppb.restapi.common.core.repository.BaseRepository;
import mppb.restapi.cursos.feature.frequencia.entity.Chamada;
import mppb.restapi.cursos.feature.frequencia.entity.Frequencia;
import mppb.restapi.cursos.feature.inscricao.entity.Inscricao;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface FrequenciaRepository extends BaseRepository<Frequencia, Long> {

    // Verifica se um aluno já registou presença numa chamada específica
    Optional<Frequencia> findByChamadaAndInscricao(Chamada chamada, Inscricao inscricao);

    boolean existsByChamadaAndInscricao(Chamada chamada, Inscricao inscricao);

    List<Frequencia> findAllByChamadaAulaId(Long aulaId);

    @Modifying
    @Query("DELETE FROM Frequencia f WHERE f.chamada.id IN (SELECT c.id FROM Chamada c WHERE c.aula.id = :aulaId)")
    int deleteByAulaId(@Param("aulaId") Long aulaId);

    @Modifying
    @Query("DELETE FROM Frequencia f WHERE f.chamada.id = :chamadaId")
    int deleteByChamadaId(@Param("chamadaId") Long chamadaId);
}
