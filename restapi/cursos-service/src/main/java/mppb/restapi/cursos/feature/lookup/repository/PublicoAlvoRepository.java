package mppb.restapi.cursos.feature.lookup.repository;

import mppb.restapi.common.core.repository.BaseRepository;
import mppb.restapi.cursos.feature.lookup.entities.PublicoAlvo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PublicoAlvoRepository extends BaseRepository<PublicoAlvo, Long> {}

