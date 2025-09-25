package mppb.restapi.cursos.feature.lookup.repository;

import mppb.restapi.cursos.feature.lookup.entities.Modalidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModalidadeRepository extends JpaRepository<Modalidade, Long> {}

