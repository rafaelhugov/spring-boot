package mppb.restapi.mobile.feature.lookup.repository;

import mppb.restapi.common.core.repository.BaseRepository;
import mppb.restapi.mobile.feature.lookup.entities.Modalidade;
import org.springframework.stereotype.Repository;

@Repository
public interface ModalidadeRepository extends BaseRepository<Modalidade, Long> {}

