package mppb.restapi.mobile.feature.cadastro.repository;

import mppb.restapi.common.core.repository.SoftDeleteRepository;
import mppb.restapi.mobile.feature.cadastro.entities.EntidadeTutora;
import org.springframework.stereotype.Repository;

@Repository
public interface EntidadeTutoraRepository extends SoftDeleteRepository<EntidadeTutora, Long> {}