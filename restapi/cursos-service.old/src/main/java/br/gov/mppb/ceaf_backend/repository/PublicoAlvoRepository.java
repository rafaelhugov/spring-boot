package br.gov.mppb.ceaf_backend.repository;

import br.gov.mppb.ceaf_backend.domain.PublicoAlvo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PublicoAlvoRepository extends JpaRepository<PublicoAlvo, Integer> {}