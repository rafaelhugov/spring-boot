package mppb.microservices.cursosapi.feature.lookup.repository;

import mppb.microservices.cursosapi.feature.lookup.entities.Modalidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModalidadeRepository extends JpaRepository<Modalidade, Long> {}

