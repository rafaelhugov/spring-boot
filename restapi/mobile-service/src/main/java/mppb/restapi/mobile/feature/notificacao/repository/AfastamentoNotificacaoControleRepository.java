package mppb.restapi.mobile.feature.notificacao.repository;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AfastamentoNotificacaoControleRepository extends NotificacaoDominioControleRepository {

    public AfastamentoNotificacaoControleRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        super(
                jdbcTemplate,
                "diariooficial.afastamentonotificacaocontrole",
                "diariooficial.afastamentonotificacaoconhecida");
    }
}
