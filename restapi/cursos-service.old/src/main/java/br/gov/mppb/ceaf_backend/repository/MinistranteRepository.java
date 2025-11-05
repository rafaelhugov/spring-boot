package br.gov.mppb.ceaf_backend.repository;

import br.gov.mppb.ceaf_backend.domain.Ministrante;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MinistranteRepository extends JpaRepository<Ministrante, Integer> {}