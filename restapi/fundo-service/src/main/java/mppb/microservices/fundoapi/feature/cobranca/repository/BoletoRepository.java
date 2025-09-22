package mppb.microservices.fundoapi.feature.cobranca.repository;

import mppb.microservices.fundoapi.feature.cobranca.entity.Boleto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BoletoRepository extends JpaRepository<Boleto, Long> {
    Optional<Boleto> findByIdentificador(String identificador);
}
