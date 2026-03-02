package mppb.restapi.cursos.feature.lookup.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import mppb.restapi.cursos.feature.lookup.entities.StatusTramitacao;

@Repository
public interface StatusTramitacaoRepository extends JpaRepository<StatusTramitacao, Long> {
}
