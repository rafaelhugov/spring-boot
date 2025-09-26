package mppb.restapi.cursos.feature.curso.repository;

import mppb.restapi.cursos.feature.curso.entity.Ministrante;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MinistranteRepository extends JpaRepository<Ministrante, Long> {}