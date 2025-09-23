package mppb.microservices.cursosapi.feature.lookup.repository;

import mppb.microservices.cursosapi.feature.lookup.entities.Modalidade;
import mppb.microservices.cursosapi.feature.lookup.entities.PapelMinistrante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PapelMinistranteRepository extends JpaRepository<PapelMinistrante, Long> {}

