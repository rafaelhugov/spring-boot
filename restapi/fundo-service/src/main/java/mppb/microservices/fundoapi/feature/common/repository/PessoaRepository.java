package mppb.microservices.fundoapi.feature.common.repository;

import mppb.microservices.fundoapi.feature.common.entities.Pessoa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PessoaRepository extends JpaRepository<Pessoa, Long> {
}
