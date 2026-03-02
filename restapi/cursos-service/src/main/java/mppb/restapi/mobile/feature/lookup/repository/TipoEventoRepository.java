package mppb.restapi.mobile.feature.lookup.repository;

import mppb.restapi.common.core.repository.BaseRepository;
import mppb.restapi.mobile.feature.lookup.entities.TipoEvento;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoEventoRepository extends BaseRepository<TipoEvento, Long> {}

