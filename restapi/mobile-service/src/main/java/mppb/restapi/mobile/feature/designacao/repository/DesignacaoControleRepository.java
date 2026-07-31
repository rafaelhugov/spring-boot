package mppb.restapi.mobile.feature.designacao.repository;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import mppb.restapi.mobile.feature.designacao.model.DesignacaoAtivaRow;

@Repository
public class DesignacaoControleRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DesignacaoControleRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Long> lockControle(String matricula) {
        String sql = """
                SELECT id
                FROM diariooficial.designacaousuariocontrole
                WHERE servidor_matricula = :matricula
                FOR UPDATE
                """;
        List<Long> ids = jdbcTemplate.queryForList(sql, new MapSqlParameterSource("matricula", matricula), Long.class);
        return ids.stream().findFirst();
    }

    public void criarControleSeAusente(String matricula) {
        String sql = """
                INSERT INTO diariooficial.designacaousuariocontrole (servidor_matricula)
                VALUES (:matricula)
                ON CONFLICT (servidor_matricula) DO NOTHING
                """;
        jdbcTemplate.update(sql, new MapSqlParameterSource("matricula", matricula));
    }

    public boolean isInicializado(String matricula) {
        String sql = """
                SELECT inicializado
                FROM diariooficial.designacaousuariocontrole
                WHERE servidor_matricula = :matricula
                """;
        List<Boolean> rows = jdbcTemplate.queryForList(sql, new MapSqlParameterSource("matricula", matricula), Boolean.class);
        return rows.stream().findFirst().orElse(false);
    }

    public void marcarInicializado(String matricula) {
        String sql = """
                UPDATE diariooficial.designacaousuariocontrole
                SET inicializado = TRUE,
                    datainicializacao = COALESCE(datainicializacao, CURRENT_TIMESTAMP),
                    dataultimoprocessamento = CURRENT_TIMESTAMP,
                    dataatualizacao = CURRENT_TIMESTAMP
                WHERE servidor_matricula = :matricula
                """;
        jdbcTemplate.update(sql, new MapSqlParameterSource("matricula", matricula));
    }

    public void atualizarProcessamento(Long controleId) {
        String sql = """
                UPDATE diariooficial.designacaousuariocontrole
                SET dataultimoprocessamento = CURRENT_TIMESTAMP,
                    dataatualizacao = CURRENT_TIMESTAMP
                WHERE id = :controleId
                """;
        jdbcTemplate.update(sql, new MapSqlParameterSource("controleId", controleId));
    }

    public void registrarConhecidasIniciais(String matricula, List<DesignacaoAtivaRow> designacoes) {
        for (DesignacaoAtivaRow designacao : designacoes) {
            inserirConhecida(matricula, designacao, "I", "NA");
        }
    }

    public boolean registrarNova(String matricula, DesignacaoAtivaRow designacao) {
        return inserirConhecida(matricula, designacao, "N", "PE");
    }

    private boolean inserirConhecida(String matricula, DesignacaoAtivaRow designacao, String origem, String status) {
        if (designacao.id() == null || designacao.tipoCodigo() == null || designacao.tipoCodigo().isBlank()) {
            return false;
        }
        String sql = """
                INSERT INTO diariooficial.designacaoconhecida (
                    servidormatricula,
                    id_designacao,
                    tipodesignacao,
                    datapublicacao,
                    origemregistro,
                    statusnotificacao
                )
                VALUES (
                    :matricula,
                    :idDesignacao,
                    :tipoDesignacao,
                    :dataPublicacao,
                    :origem,
                    :status
                )
                ON CONFLICT (servidormatricula, id_designacao, tipodesignacao) DO NOTHING
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("matricula", matricula)
                .addValue("idDesignacao", designacao.id())
                .addValue("tipoDesignacao", designacao.tipoCodigo())
                .addValue("dataPublicacao", designacao.dataPublicacao() == null ? null : Date.valueOf(designacao.dataPublicacao()))
                .addValue("origem", origem)
                .addValue("status", status);
        try {
            return jdbcTemplate.update(sql, params) > 0;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }
}
