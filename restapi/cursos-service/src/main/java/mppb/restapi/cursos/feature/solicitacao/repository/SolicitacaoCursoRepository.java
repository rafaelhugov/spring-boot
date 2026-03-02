package mppb.restapi.cursos.feature.solicitacao.repository;

import mppb.restapi.common.core.repository.BaseRepository;
import mppb.restapi.cursos.feature.solicitacao.entity.SolicitacaoCurso;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SolicitacaoCursoRepository extends BaseRepository<SolicitacaoCurso, Long> {

    @Query("SELECT s FROM SolicitacaoCurso s LEFT JOIN FETCH s.agendas LEFT JOIN FETCH s.ministrantes WHERE s.id = :id")
    Optional<SolicitacaoCurso> findByIdWithDetails(@Param("id") Long id);

    Optional<SolicitacaoCurso> findByUuidReferencia(UUID uuidReferencia);
}
