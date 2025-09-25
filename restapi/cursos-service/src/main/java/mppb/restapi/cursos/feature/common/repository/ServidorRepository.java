package mppb.restapi.cursos.feature.common.repository;

import mppb.restapi.cursos.feature.common.entities.Servidor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import mppb.restapi.cursos.feature.common.projection.ServidorProjection;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServidorRepository extends JpaRepository<Servidor, Long> {

    @Query(
            value = """
            SELECT
               servidor_id       AS servidorId,
               servidor_matricula AS servidorMatricula,
               servidor_nome     AS servidorNome,
               cargo_nome        AS cargoNome,
               tipocargo_nome    AS tipocargoNome
            FROM diariooficial.mapa_por_membro_vw
            WHERE lower(funcaosemacentos(servidor_nome)) LIKE lower(concat('%', replace(funcaosemacentos(:nome), ' ', '%'), '%'))
            GROUP BY servidor_id, servidor_matricula, servidor_nome, cargo_nome, tipocargo_nome
            ORDER BY servidor_nome
            LIMIT 10
            """,
            nativeQuery = true
    )
     List<ServidorProjection> buscarServidoresPorNome(@Param("nome") String nome);

    @Query(
            value = """
            SELECT
               servidor_id       AS servidorId,
               servidor_matricula AS servidorMatricula,
               servidor_nome     AS servidorNome,
               cargo_nome        AS cargoNome,
               tipocargo_nome    AS tipocargoNome
            FROM diariooficial.mapa_por_membro_vw
            WHERE servidor_id = :id
            LIMIT 1
            """,
            nativeQuery = true
    )
    Optional<ServidorProjection> buscarServidorPorId(@Param("id") Integer id);

}