package mppb.microservices.fundoapi.feature.cobranca.repository;

import mppb.microservices.fundoapi.feature.cobranca.entity.Natureza;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NaturezaRepository extends JpaRepository<Natureza, Long> {

}
