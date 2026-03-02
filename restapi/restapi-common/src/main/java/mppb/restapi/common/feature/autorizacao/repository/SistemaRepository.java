package mppb.restapi.common.feature.autorizacao.repository;

import mppb.restapi.common.feature.autorizacao.entities.Sistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SistemaRepository extends JpaRepository<Sistema, Integer> {

    /**
     * Busca um sistema pela sua Sigla.
     * @param sigla A sigla do sistema (ex: "CURSOS")
     * @return Um Optional contendo a entidade Sistema.
     */
    Optional<Sistema> findBySigla(String sigla);
}
