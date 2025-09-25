package mppb.restapi.cursos.feature.lookup.repository;

import mppb.restapi.cursos.feature.lookup.entities.TipoEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoEventoRepository extends JpaRepository<TipoEvento, Long> {}

