package mppb.restapi.cursos.feature.common.repository;

import mppb.restapi.common.core.repository.BaseRepository;
import mppb.restapi.cursos.feature.common.entities.Servidor;
import org.springframework.data.jpa.repository.Query;
import mppb.restapi.cursos.feature.common.projection.ServidorProjection;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
// TODO substituir view por consulta direta ao RH, pois a view é para designacoes
@Repository
public interface ServidorRepository extends BaseRepository<Servidor, Long> {

//    @Query(
//            value = """
//            SELECT
//               servidor_id             AS id,
//               servidor_matricula      AS servidorMatricula,
//               servidor_nome           AS nome,
//               cargo_nome              AS cargoNome,
//               tipocargo_nome          AS tipocargoNome,
//               CASE WHEN ativo = 'A' THEN 'ATIVO' ELSE 'INATIVO' END AS situacao
//            FROM diariooficial.mapa_por_membro_vw
//            WHERE lower(funcaosemacentos(servidor_nome)) LIKE lower(concat('%', replace(funcaosemacentos(:nome), ' ', '%'), '%'))
//            GROUP BY servidor_id, servidor_matricula, servidor_nome, cargo_nome, tipocargo_nome, ativo
//            ORDER BY servidor_nome
//            LIMIT 10
//            """,
//            nativeQuery = true
//    )
//    List<ServidorProjection> buscarServidoresPorNome(@Param("nome") String nome);


//    @Query(
//            value = """
//            SELECT
//               servidor_id             AS id,
//               servidor_matricula      AS servidorMatricula,
//               servidor_nome           AS nome,
//               cargo_nome              AS cargoNome,
//               tipocargo_nome          AS tipocargoNome,
//               CASE WHEN ativo = 'A' THEN 'ATIVO' ELSE 'INATIVO' END AS situacao
//            FROM diariooficial.mapa_por_membro_vw
//            WHERE servidor_id = :id
//            LIMIT 1
//            """,
//            nativeQuery = true
//    )
//    Optional<ServidorProjection> buscarServidorPorId(@Param("id") Integer id);

    @Query(
            value = """
            SELECT
                s.id,
                s.matricula                                             AS servidorMatricula,
                p.nome,
                f.nome                                                  AS cargoNome,
                CASE WHEN tc.tipo = 'S' THEN 'Servidor' ELSE 'Membro' END AS tipocargoNome,
                CASE WHEN s.ativo = 'A' THEN 'ATIVO' ELSE 'INATIVO' END   AS situacao
            FROM servidor s
            JOIN pessoa p ON p.id = s.id_pessoa
            JOIN recursoshumanos.funcao f ON f.id = s.id_cargo
            JOIN recursoshumanos.tipocargo tc ON tc.id = f.id_tipocargo
            WHERE s.excluido = false
            AND lower(funcaosemacentos(p.nome)) LIKE lower(concat('%', replace(funcaosemacentos(:nome), ' ', '%'), '%'))
            ORDER BY p.nome
            LIMIT 10
            """,
            nativeQuery = true
    )
    List<ServidorProjection> buscarServidoresPorNome(@Param("nome") String nome);

    @Query(
            value = """
            SELECT
                s.id,
                s.matricula                                             AS servidorMatricula,
                p.nome,
                f.nome                                                  AS cargoNome,
                CASE WHEN tc.tipo = 'S' THEN 'Servidor' ELSE 'Membro' END AS tipocargoNome,
                CASE WHEN s.ativo = 'A' THEN 'ATIVO' ELSE 'INATIVO' END   AS situacao
            FROM servidor s
            JOIN pessoa p ON p.id = s.id_pessoa
            JOIN recursoshumanos.funcao f ON f.id = s.id_cargo
            JOIN recursoshumanos.tipocargo tc ON tc.id = f.id_tipocargo
            WHERE s.id = :id
            LIMIT 1
            """,
            nativeQuery = true
    )
    Optional<ServidorProjection> buscarServidorPorId(@Param("id") Integer id);

}

