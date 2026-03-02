package mppb.restapi.cursos.feature.lookup.repository;

import mppb.restapi.common.core.repository.BaseRepository;
import mppb.restapi.cursos.feature.lookup.entities.TipoEvento;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoEventoRepository extends BaseRepository<TipoEvento, Long> {}

