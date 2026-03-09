package mppb.restapi.cursos.feature.common.repository;

import mppb.restapi.common.core.repository.BaseRepository;
import mppb.restapi.cursos.feature.common.entities.Arquivo;
import org.springframework.stereotype.Repository;

@Repository
public interface ArquivoRepository extends BaseRepository<Arquivo, Long> {
}
