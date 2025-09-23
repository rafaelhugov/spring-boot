package mppb.microservices.cursosapi.feature.lookup.repository;

import mppb.microservices.cursosapi.feature.lookup.entities.PublicoAlvo;
import mppb.microservices.cursosapi.feature.lookup.entities.TipoEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PublicoAlvoRepository extends JpaRepository<PublicoAlvo, Long> {}

