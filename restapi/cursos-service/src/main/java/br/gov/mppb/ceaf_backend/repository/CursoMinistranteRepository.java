package br.gov.mppb.ceaf_backend.repository;

import br.gov.mppb.ceaf_backend.domain.CursoMinistrante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CursoMinistranteRepository extends JpaRepository<CursoMinistrante, Long> {

    List<CursoMinistrante> findByCursoId(Long cursoId);

    boolean existsByCursoIdAndMinistranteId(Long cursoId, Long ministranteId);
}

