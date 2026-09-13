package mppb.restapi.mobile.feature.notificacao.repository;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PagamentoNotificacaoControleRepository extends NotificacaoDominioControleRepository {

    public PagamentoNotificacaoControleRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        super(
                jdbcTemplate,
                "diariooficial.pagamentonotificacaocontrole",
                "diariooficial.pagamentonotificacaoconhecida");
    }
}
