package br.gov.mppb.ceaf_backend.repository;

import br.gov.mppb.ceaf_backend.domain.TipoEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoEventoRepository extends JpaRepository<TipoEvento, Integer> {}