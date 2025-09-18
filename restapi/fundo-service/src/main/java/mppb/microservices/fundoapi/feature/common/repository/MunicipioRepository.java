package mppb.microservices.fundoapi.feature.common.repository;

import mppb.microservices.fundoapi.feature.common.entities.Municipio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MunicipioRepository extends JpaRepository<Municipio, Long> {
}
