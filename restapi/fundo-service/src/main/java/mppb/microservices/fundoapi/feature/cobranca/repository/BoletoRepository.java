package mppb.microservices.fundoapi.feature.cobranca.repository;

import mppb.microservices.fundoapi.feature.cobranca.dto.AreaDto;
import mppb.microservices.fundoapi.feature.cobranca.entity.Area;
import mppb.microservices.fundoapi.feature.cobranca.entity.Boleto;
import mppb.microservices.fundoapi.feature.cobranca.projection.AreaSelectProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BoletoRepository extends JpaRepository<Boleto, Long> {
    Optional<Boleto> findByIdentificador(String identificador);
}
