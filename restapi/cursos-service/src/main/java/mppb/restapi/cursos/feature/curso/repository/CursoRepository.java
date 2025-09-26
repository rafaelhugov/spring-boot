package mppb.restapi.cursos.feature.curso.repository;

import mppb.restapi.cursos.feature.curso.entity.Curso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CursoRepository  extends JpaRepository<Curso, Long> {
    Optional<Curso> findByHashInscricao(UUID hash);
}
