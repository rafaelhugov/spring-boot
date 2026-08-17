package mppb.restapi.cursos.feature.common.repository;

import mppb.restapi.common.core.repository.BaseRepository;
import mppb.restapi.cursos.feature.common.entities.Servidor;
import org.springframework.data.jpa.repository.Query;
import mppb.restapi.cursos.feature.common.projection.ServidorProjection;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// TODO substituir view por consulta direta ao RH, pois a view é para designacoes
@Repository
public interface ServidorRepository extends BaseRepository<Servidor, Long> {

        @Query(value = "SELECT " +
                        "    s.id, " +
                        "    p.id                                                    AS idPessoa, " +
                        "    s.matricula                                             AS servidorMatricula, " +
                        "    p.nome, " +
                        "    f.nome                                                  AS cargoNome, " +
                        "    CASE WHEN tc.tipo = 'S' THEN 'Servidor' ELSE 'Membro' END AS tipoCargoNome, " +
                        "    CASE WHEN s.ativo = 'A' THEN 'ATIVO' ELSE 'INATIVO' END   AS situacao, " +
                        "    s.datasaida                                              AS dataSaida, " +
                        "    p.email, " +
                        "    COALESCE(p.cel, p.tel)                                   AS telefone " +
                        "FROM public.servidor s " +
                        "JOIN public.pessoa p ON p.id = s.id_pessoa " +
                        "LEFT JOIN recursoshumanos.funcao f ON f.id = s.id_cargo " +
                        "LEFT JOIN recursoshumanos.tipocargo tc ON tc.id = f.id_tipocargo " +
                        "WHERE s.excluido = false " +
                        "AND s.ativo = 'A' " +
                        "AND lower(funcaosemacentos(p.nome)) LIKE lower(concat('%', replace(funcaosemacentos(:nome), ' ', '%'), '%')) "
                        +
                        "ORDER BY " +
                        "    CASE WHEN lower(funcaosemacentos(p.nome)) LIKE lower(concat(funcaosemacentos(:nome), '%')) THEN 1 ELSE 2 END, "
                        +
                        "    p.nome " +
                        "LIMIT 10", nativeQuery = true)
        List<ServidorProjection> buscarServidoresPorNome(@Param("nome") String nome);

        @Query(value = "SELECT " +
                        "    s.id, " +
                        "    p.id                                                    AS idPessoa, " +
                        "    s.matricula                                             AS servidorMatricula, " +
                        "    p.nome, " +
                        "    f.nome                                                  AS cargoNome, " +
                        "    CASE WHEN tc.tipo = 'S' THEN 'Servidor' ELSE 'Membro' END AS tipoCargoNome, " +
                        "    CASE WHEN s.ativo = 'A' THEN 'ATIVO' ELSE 'INATIVO' END   AS situacao, " +
                        "    s.datasaida                                              AS dataSaida, " +
                        "    p.email, " +
                        "    COALESCE(p.cel, p.tel)                                   AS telefone " +
                        "FROM public.servidor s " +
                        "JOIN public.pessoa p ON p.id = s.id_pessoa " +
                        "LEFT JOIN recursoshumanos.funcao f ON f.id = s.id_cargo " +
                        "LEFT JOIN recursoshumanos.tipocargo tc ON tc.id = f.id_tipocargo " +
                        "WHERE s.excluido = false " +
                        "AND (s.ativo = 'A' OR s.datasaida IS NULL OR s.datasaida >= :dataFinalCurso) " +
                        "AND lower(funcaosemacentos(p.nome)) LIKE lower(concat('%', replace(funcaosemacentos(:nome), ' ', '%'), '%')) " +
                        "ORDER BY " +
                        "    CASE WHEN lower(funcaosemacentos(p.nome)) LIKE lower(concat(funcaosemacentos(:nome), '%')) THEN 1 ELSE 2 END, " +
                        "    CASE WHEN s.ativo = 'A' THEN 1 ELSE 2 END, " +
                        "    p.nome " +
                        "LIMIT 10", nativeQuery = true)
        List<ServidorProjection> buscarServidoresPorNomeElegiveisNoPeriodo(@Param("nome") String nome,
                        @Param("dataFinalCurso") LocalDate dataFinalCurso);

        @Query(value = "SELECT " +
                        "    s.id, " +
                        "    p.id                                                    AS idPessoa, " +
                        "    s.matricula                                             AS servidorMatricula, " +
                        "    p.nome, " +
                        "    pf.cpf, " +
                        "    p.email, " +
                        "    f.nome                                                  AS cargoNome, " +
                        "    CASE WHEN tc.tipo = 'S' THEN 'Servidor' ELSE 'Membro' END AS tipoCargoNome, " +
                        "    CASE WHEN s.ativo = 'A' THEN 'ATIVO' ELSE 'INATIVO' END   AS situacao, " +
                        "    s.datasaida                                              AS dataSaida " +
                        "FROM public.servidor s " +
                        "LEFT JOIN public.pessoa p ON p.id = s.id_pessoa " +
                        "LEFT JOIN public.pessoafisica pf ON pf.id_pessoa = p.id " +
                        "LEFT JOIN recursoshumanos.funcao f ON f.id = s.id_cargo " +
                        "LEFT JOIN recursoshumanos.tipocargo tc ON tc.id = f.id_tipocargo " +
                        "WHERE s.id = :id " +
                        "LIMIT 1", nativeQuery = true)
        Optional<ServidorProjection> buscarServidorPorId(@Param("id") Integer id);

        @Query(value = "SELECT " +
                        "    s.id as id, " +
                        "    p.id as idPessoa, " +
                        "    s.matricula AS servidorMatricula, " +
                        "    p.nome as nome, " +
                        "    pf.cpf, " +
                        "    p.email, " +
                        "    f.nome AS cargoNome, " +
                        "    CASE WHEN tc.tipo = 'S' THEN 'Servidor' ELSE 'Membro' END AS tipoCargoNome, " +
                        "    CASE WHEN s.ativo = 'A' THEN 'ATIVO' ELSE 'INATIVO' END AS situacao, " +
                        "    s.datasaida AS dataSaida " +
                        "FROM public.servidor s " +
                        "LEFT JOIN public.pessoa p ON p.id = s.id_pessoa " +
                        "LEFT JOIN public.pessoafisica pf ON pf.id_pessoa = p.id " +
                        "LEFT JOIN recursoshumanos.funcao f ON f.id = s.id_cargo " +
                        "LEFT JOIN recursoshumanos.tipocargo tc ON tc.id = f.id_tipocargo " +
                        "WHERE pf.cpf = :cpf " +
                        "AND s.excluido = false " +
                        "AND s.ativo = 'A' " +
                        "LIMIT 1", nativeQuery = true)
        Optional<ServidorProjection> buscarServidorPorCpf(@Param("cpf") String cpf);

        /**
         * Busca o ID de servidor (do RH) com base no login (do sistema de
         * autenticação).
         * Utiliza a view de mapeamento 'autorizacao.usuarioservidor_vw'.
         *
         * @param login O login do utilizador (ex: 'usuario.teste').
         * @return Um Optional contendo o ID do servidor (Integer).
         */
        @Query(value = "SELECT v.id_servidor " +
                        "FROM autorizacao.usuarioservidor_vw v " +
                        "JOIN public.servidor s ON s.id = v.id_servidor " +
                        "WHERE v.login = :login " +
                        "AND s.excluido = false " +
                        "AND s.ativo = 'A' " +
                        "LIMIT 1", nativeQuery = true)
        Optional<Integer> findIdServidorByLogin(@Param("login") String login);

}
