package mppb.microservices.fundoapi.feature.common.repository;

import mppb.microservices.fundoapi.feature.common.entities.Comarca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComarcaRepository extends JpaRepository<Comarca, Long> {
}
