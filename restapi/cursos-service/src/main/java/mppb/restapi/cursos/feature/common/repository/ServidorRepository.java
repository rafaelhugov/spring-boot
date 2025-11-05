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

    @Query(value = """
            SELECT
                s.id as id,
                s.matricula AS servidorMatricula,
                p.nome as nome,
                f.nome AS cargoNome,
                CASE WHEN tc.tipo = 'S' THEN 'Servidor' ELSE 'Membro' END AS tipoCargoNome,
                CASE WHEN s.ativo = 'A' THEN 'ATIVO' ELSE 'INATIVO' END AS situacao
            FROM servidor s
            JOIN pessoa p ON p.id = s.id_pessoa
            JOIN pessoafisica pf ON pf.id_pessoa = p.id
            JOIN recursoshumanos.funcao f ON f.id = s.id_cargo
            JOIN recursoshumanos.tipocargo tc ON tc.id = f.id_tipocargo
            WHERE pf.cpf = :cpf
            LIMIT 1
            """, nativeQuery = true)
    Optional<ServidorProjection> buscarServidorPorCpf(@Param("cpf") String cpf);


    /**
     * Busca o ID de servidor (do RH) com base no login (do sistema de autenticação).
     * Utiliza a view de mapeamento 'autorizacao.usuarioservidor_vw'.
     * @param login O login do utilizador (ex: 'usuario.teste').
     * @return Um Optional contendo o ID do servidor (Integer).
     */
    @Query(value = "SELECT id_servidor FROM autorizacao.usuarioservidor_vw WHERE login = :login LIMIT 1",
            nativeQuery = true)
    Optional<Integer> findIdServidorByLogin(@Param("login") String login);



}

