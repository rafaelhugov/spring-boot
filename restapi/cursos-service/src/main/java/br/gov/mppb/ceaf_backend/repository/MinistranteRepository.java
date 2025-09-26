package br.gov.mppb.ceaf_backend.repository;

import br.gov.mppb.ceaf_backend.domain.Ministrante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MinistranteRepository extends JpaRepository<Ministrante, Long> {
    Optional<Ministrante> findByIdServidor(Long idServidor);
}