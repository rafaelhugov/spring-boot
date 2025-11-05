package br.gov.mppb.ceaf_backend.repository;

import br.gov.mppb.ceaf_backend.domain.EntidadeTutora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EntidadeTutoraRepository extends JpaRepository<EntidadeTutora, Integer> {}