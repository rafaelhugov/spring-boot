package mppb.restapi.mobile.feature.notificacao.repository;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ContrachequeNotificacaoControleRepository extends NotificacaoDominioControleRepository {

    public ContrachequeNotificacaoControleRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        super(
                jdbcTemplate,
                "diariooficial.contrachequenotificacaocontrole",
                "diariooficial.contrachequenotificacaoconhecida");
    }
}
