package mppb.microservices.fundoapi.feature.common.repository;

import mppb.microservices.fundoapi.feature.common.entities.Estado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstadoRepository extends JpaRepository<Estado, Long> {
}
