package mppb.restapi.cursos.feature.solicitacao.repository;

import mppb.restapi.cursos.feature.solicitacao.entity.SolicitacaoAgenda;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitacaoAgendaRepository extends JpaRepository<SolicitacaoAgenda, Long> {
}
