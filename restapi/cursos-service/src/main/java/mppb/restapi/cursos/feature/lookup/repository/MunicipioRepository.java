package mppb.restapi.cursos.feature.lookup.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import mppb.restapi.cursos.feature.lookup.entities.Municipio;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MunicipioRepository extends JpaRepository<Municipio, Long> {

    @Query("SELECT m FROM Municipio m JOIN m.estado e WHERE e.sigla = :sigla ORDER BY m.nome ASC")
    List<Municipio> findByEstadoSiglaOrderByNomeAsc(@Param("sigla") String sigla);
}
