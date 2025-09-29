package mppb.restapi.cursos.feature.curso.repository;

import mppb.restapi.common.core.repository.BaseRepository;
import mppb.restapi.cursos.feature.curso.entity.Curso;

import java.util.Optional;
import java.util.UUID;

public interface CursoRepository  extends BaseRepository<Curso, Long> {
    Optional<Curso> findByHashInscricao(UUID hash);
}
