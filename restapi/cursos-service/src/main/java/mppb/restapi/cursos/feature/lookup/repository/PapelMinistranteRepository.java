package mppb.restapi.cursos.feature.lookup.repository;

import mppb.restapi.common.core.repository.BaseRepository;
import mppb.restapi.cursos.feature.lookup.entities.PapelMinistrante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PapelMinistranteRepository extends BaseRepository<PapelMinistrante, Long> {}

