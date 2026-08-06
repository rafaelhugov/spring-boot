package br.gov.mppb.ncap.feature.servidor;

import mppb.restapi.common.core.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServidorRepository extends BaseRepository<Servidor, Long> {

    /**
     * Busca servidores/membros ativos por nome (máx. 10 resultados).
     * Segue o mesmo padrão de query nativa usado em {@code cursos-service}.
     */
    @Query(value =
            "SELECT " +
            "    s.id, " +
            "    p.nome, " +
            "    f.nome                                                  AS cargoNome, " +
            "    CASE WHEN tc.tipo = 'S' THEN 'Servidor' ELSE 'Membro' END AS tipoCargoNome, " +
            "    s.matricula                                             AS servidorMatricula, " +
            "    CASE WHEN s.ativo = 'A' THEN 'ATIVO' ELSE 'INATIVO' END   AS situacao, " +
            "    p.email " +
            "FROM public.servidor s " +
            "JOIN public.pessoa p ON p.id = s.id_pessoa " +
            "LEFT JOIN recursoshumanos.funcao f ON f.id = s.id_cargo " +
            "LEFT JOIN recursoshumanos.tipocargo tc ON tc.id = f.id_tipocargo " +
            "WHERE s.excluido = false " +
            "AND s.ativo = 'A' " +
            "AND tc.tipo <> 'S' " +
            "AND lower(funcaosemacentos(p.nome)) LIKE lower(concat('%', replace(funcaosemacentos(:nome), ' ', '%'), '%')) " +
            "ORDER BY " +
            "    CASE WHEN lower(funcaosemacentos(p.nome)) LIKE lower(concat(funcaosemacentos(:nome), '%')) THEN 1 ELSE 2 END, " +
            "    p.nome " +
            "LIMIT 10",
            nativeQuery = true)
    List<ServidorProjection> buscarServidoresPorNome(@Param("nome") String nome);

    @Query(value =
            "SELECT " +
            "    s.id, " +
            "    p.nome, " +
            "    f.nome                                                  AS cargoNome, " +
            "    CASE WHEN tc.tipo = 'S' THEN 'Servidor' ELSE 'Membro' END AS tipoCargoNome, " +
            "    s.matricula                                             AS servidorMatricula, " +
            "    CASE WHEN s.ativo = 'A' THEN 'ATIVO' ELSE 'INATIVO' END   AS situacao, " +
            "    p.email " +
            "FROM public.servidor s " +
            "JOIN public.pessoa p ON p.id = s.id_pessoa " +
            "LEFT JOIN recursoshumanos.funcao f ON f.id = s.id_cargo " +
            "LEFT JOIN recursoshumanos.tipocargo tc ON tc.id = f.id_tipocargo " +
            "WHERE s.id = :id",
            nativeQuery = true)
    java.util.Optional<ServidorProjection> obterDetalhesPorId(@Param("id") Long id);
}
