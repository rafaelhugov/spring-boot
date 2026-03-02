package mppb.restapi.cursos.feature.solicitacao.repository;

import mppb.restapi.cursos.feature.solicitacao.entity.SolicitacaoMinistrante;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitacaoMinistranteRepository extends JpaRepository<SolicitacaoMinistrante, Long> {
}
