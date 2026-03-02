package mppb.restapi.mobile.feature.lookup.repository;

import mppb.restapi.common.core.repository.BaseRepository;
import mppb.restapi.mobile.feature.lookup.entities.PapelMinistrante;
import org.springframework.stereotype.Repository;

@Repository
public interface PapelMinistranteRepository extends BaseRepository<PapelMinistrante, Long> {}

