package mppb.restapi.cursos.feature.lookup.repository;

import mppb.restapi.cursos.feature.lookup.entities.LocalEvento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalEventoRepository extends JpaRepository<LocalEvento, Long> {
}
