package mppb.restapi.mobile.feature.designacao.repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import mppb.restapi.mobile.feature.designacao.model.DesignacaoAtivaRow;
import mppb.restapi.mobile.feature.designacao.model.DesignacaoControlePendenteNotificacao;

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

    @Transactional
    public List<DesignacaoControlePendenteNotificacao> reservarPendentesParaNotificacao(
            int batchSize,
            int maxTentativas,
            String identificadorReserva) {
        String sql = """
                WITH candidatas AS (
                    SELECT id
                    FROM diariooficial.designacaoconhecida
                    WHERE origemregistro = 'N'
                      AND statusnotificacao IN ('PE', 'ER')
                      AND quantidadetentativas < :maxTentativas
                      AND (
                          datareservaprocessamento IS NULL
                          OR datareservaprocessamento < CURRENT_TIMESTAMP - INTERVAL '10 minutes'
                      )
                    ORDER BY
                        dataregistro ASC,
                        id ASC
                    LIMIT :batchSize
                    FOR UPDATE SKIP LOCKED
                )
                UPDATE diariooficial.designacaoconhecida dc
                SET datareservaprocessamento = CURRENT_TIMESTAMP,
                    identificadorreserva = :identificadorReserva
                FROM candidatas
                WHERE dc.id = candidatas.id
                RETURNING
                    dc.id,
                    dc.identificadorreserva,
                    dc.servidormatricula,
                    dc.id_designacao,
                    dc.tipodesignacao,
                    dc.datapublicacao,
                    dc.statusnotificacao,
                    dc.quantidadetentativas
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("batchSize", batchSize)
                .addValue("maxTentativas", maxTentativas)
                .addValue("identificadorReserva", identificadorReserva);
        return jdbcTemplate.query(sql, params, this::mapPendenteNotificacao);
    }

    public boolean marcarNotificacaoEnviada(Long id, String identificadorReserva) {
        String sql = """
                UPDATE diariooficial.designacaoconhecida
                SET statusnotificacao = 'EN',
                    datanotificacao = CURRENT_TIMESTAMP,
                    quantidadetentativas = quantidadetentativas + 1,
                    ultimamensagemerro = NULL,
                    datareservaprocessamento = NULL,
                    identificadorreserva = NULL
                WHERE id = :id
                  AND identificadorreserva = :identificadorReserva
                  AND origemregistro = 'N'
                  AND statusnotificacao IN ('PE', 'ER')
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("identificadorReserva", identificadorReserva);
        return jdbcTemplate.update(sql, params) > 0;
    }

    public boolean marcarNotificacaoErro(Long id, String identificadorReserva, String mensagemErro) {
        String sql = """
                UPDATE diariooficial.designacaoconhecida
                SET statusnotificacao = 'ER',
                    quantidadetentativas = quantidadetentativas + 1,
                    ultimamensagemerro = :mensagemErro,
                    datareservaprocessamento = NULL,
                    identificadorreserva = NULL
                WHERE id = :id
                  AND identificadorreserva = :identificadorReserva
                  AND origemregistro = 'N'
                  AND statusnotificacao IN ('PE', 'ER')
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("identificadorReserva", identificadorReserva)
                .addValue("mensagemErro", mensagemErro);
        return jdbcTemplate.update(sql, params) > 0;
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

    private DesignacaoControlePendenteNotificacao mapPendenteNotificacao(ResultSet rs, int rowNum) throws SQLException {
        String matricula = rs.getString("servidormatricula");
        String tipoDesignacao = rs.getString("tipodesignacao");
        Date dataPublicacao = rs.getDate("datapublicacao");
        DesignacaoAtivaRow designacao = new DesignacaoAtivaRow(
                rs.getLong("id_designacao"),
                matricula,
                tipoDesignacao,
                null,
                null,
                null,
                null,
                null,
                null,
                dataPublicacao == null ? null : dataPublicacao.toLocalDate(),
                null,
                false,
                null,
                null);
        return new DesignacaoControlePendenteNotificacao(
                rs.getLong("id"),
                rs.getString("identificadorreserva"),
                matricula,
                rs.getString("statusnotificacao"),
                rs.getInt("quantidadetentativas"),
                designacao);
    }
}
