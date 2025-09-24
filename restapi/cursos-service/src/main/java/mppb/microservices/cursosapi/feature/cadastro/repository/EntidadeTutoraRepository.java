package mppb.microservices.cursosapi.feature.cadastro.repository;

import mppb.microservices.cursosapi.feature.cadastro.entities.EntidadeTutora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EntidadeTutoraRepository extends JpaRepository<EntidadeTutora, Long> {}