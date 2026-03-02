package mppb.restapi.mobile.feature.frequencia.repository;

import mppb.restapi.common.core.repository.BaseRepository;
import mppb.restapi.mobile.feature.frequencia.entity.Aula;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AulaRepository extends BaseRepository<Aula, Long> {

    // Busca todas as aulas de um curso, ordenadas por data e hora
    @Query("SELECT a FROM Aula a LEFT JOIN FETCH a.chamadas WHERE a.curso.id = :cursoId ORDER BY a.data ASC, a.horaInicio ASC")
    List<Aula> findAllByCursoId(Long cursoId);

}