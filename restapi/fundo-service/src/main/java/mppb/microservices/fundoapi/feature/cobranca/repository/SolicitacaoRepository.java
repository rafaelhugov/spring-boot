package mppb.microservices.fundoapi.feature.cobranca.repository;

import mppb.microservices.fundoapi.feature.cobranca.entity.Solicitacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SolicitacaoRepository extends JpaRepository<Solicitacao, Long> {
}
