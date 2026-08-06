package br.gov.mppb.ncap.feature.entidade;

import mppb.restapi.common.core.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EntidadeRepository extends BaseRepository<Entidade, Long> {
    Page<Entidade> findByNomecnmpContainingIgnoreCaseOrMunicipioContainingIgnoreCase(String nome, String municipio, Pageable pageable);

    Optional<Entidade> findByIdCnmp(Long idCnmp);

    @Query(value =
            "SELECT COALESCE(c1.id, c2.id) AS id_comarca " +
            "FROM public.municipio m " +
            "LEFT JOIN tj.comarca c1 ON m.id = c1.id_municipio AND c1.excluido = false " +
            "LEFT JOIN tj.termo t ON m.id = t.id_municipio " +
            "LEFT JOIN tj.comarca c2 ON t.id_comarca = c2.id AND c2.excluido = false " +
            "WHERE lower(funcaosemacentos(m.nome)) = lower(funcaosemacentos(:nomeMunicipio)) " +
            "LIMIT 1",
            nativeQuery = true)
    Optional<Long> findComarcaIdByMunicipioNome(@Param("nomeMunicipio") String nomeMunicipio);
}
