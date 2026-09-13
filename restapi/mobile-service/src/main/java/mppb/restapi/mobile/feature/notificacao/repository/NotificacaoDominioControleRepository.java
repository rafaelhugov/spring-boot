package mppb.restapi.mobile.feature.notificacao.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import mppb.restapi.mobile.feature.notificacao.model.EventoFuncionalNotificacao;
import mppb.restapi.mobile.feature.notificacao.model.EventoFuncionalPendenteNotificacao;

public abstract class NotificacaoDominioControleRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final String tabelaControle;
    private final String tabelaConhecida;

    protected NotificacaoDominioControleRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            String tabelaControle,
            String tabelaConhecida) {
        this.jdbcTemplate = jdbcTemplate;
        this.tabelaControle = tabelaControle;
        this.tabelaConhecida = tabelaConhecida;
    }

    public void criarControleSeAusente(String matricula) {
        String sql = """
                INSERT INTO %s (servidormatricula)
                VALUES (:matricula)
                ON CONFLICT (servidormatricula) DO NOTHING
                """.formatted(tabelaControle);
        jdbcTemplate.update(sql, params(matricula));
    }

    public Optional<Long> lockControle(String matricula) {
        String sql = """
                SELECT id
                FROM %s
                WHERE servidormatricula = :matricula
                FOR UPDATE
                """.formatted(tabelaControle);
        List<Long> ids = jdbcTemplate.queryForList(sql, params(matricula), Long.class);
        return ids.stream().findFirst();
    }

    public boolean isInicializado(String matricula) {
        String sql = """
                SELECT inicializado
                FROM %s
                WHERE servidormatricula = :matricula
                """.formatted(tabelaControle);
        List<Boolean> rows = jdbcTemplate.queryForList(sql, params(matricula), Boolean.class);
        return rows.stream().findFirst().orElse(false);
    }

    public void marcarInicializado(String matricula) {
        String sql = """
                UPDATE %s
                SET inicializado = TRUE,
                    datainicializacao = COALESCE(datainicializacao, CURRENT_TIMESTAMP),
                    dataultimoprocessamento = CURRENT_TIMESTAMP,
                    dataatualizacao = CURRENT_TIMESTAMP
                WHERE servidormatricula = :matricula
                """.formatted(tabelaControle);
        jdbcTemplate.update(sql, params(matricula));
    }

    public void atualizarProcessamento(Long controleId) {
        String sql = """
                UPDATE %s
                SET dataultimoprocessamento = CURRENT_TIMESTAMP,
                    dataatualizacao = CURRENT_TIMESTAMP
                WHERE id = :controleId
                """.formatted(tabelaControle);
        jdbcTemplate.update(sql, new MapSqlParameterSource("controleId", controleId));
    }

    public int registrarConhecidasIniciais(List<EventoFuncionalNotificacao> eventos) {
        int total = 0;
        for (EventoFuncionalNotificacao evento : eventos) {
            if (inserirConhecida(evento, "I", "NA")) {
                total++;
            }
        }
        return total;
    }

    public boolean registrarNova(EventoFuncionalNotificacao evento) {
        return inserirConhecida(evento, "N", "PE");
    }

    @Transactional
    public List<EventoFuncionalPendenteNotificacao> reservarPendentesParaNotificacao(
            int batchSize,
            int maxTentativas,
            String identificadorReserva) {
        String sql = """
                WITH candidatas AS (
                    SELECT id
                    FROM %s
                    WHERE origemregistro = 'N'
                      AND statusnotificacao IN ('PE', 'ER')
                      AND quantidadetentativas < :maxTentativas
                      AND (
                          datareservaprocessamento IS NULL
                          OR datareservaprocessamento < CURRENT_TIMESTAMP - INTERVAL '10 minutes'
                      )
                    ORDER BY dataregistro ASC, id ASC
                    LIMIT :batchSize
                    FOR UPDATE SKIP LOCKED
                )
                UPDATE %s conhecida
                SET datareservaprocessamento = CURRENT_TIMESTAMP,
                    identificadorreserva = :identificadorReserva,
                    dataatualizacao = CURRENT_TIMESTAMP
                FROM candidatas
                WHERE conhecida.id = candidatas.id
                RETURNING
                    conhecida.id,
                    conhecida.identificadorreserva,
                    conhecida.servidormatricula,
                    conhecida.chaveevento,
                    conhecida.tipoevento,
                    conhecida.rota,
                    conhecida.titulo,
                    conhecida.corpo,
                    conhecida.statusnotificacao,
                    conhecida.quantidadetentativas
                """.formatted(tabelaConhecida, tabelaConhecida);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("batchSize", batchSize)
                .addValue("maxTentativas", maxTentativas)
                .addValue("identificadorReserva", identificadorReserva);
        return jdbcTemplate.query(sql, params, this::mapPendente);
    }

    public boolean marcarNotificacaoEnviada(Long id, String identificadorReserva) {
        String sql = """
                UPDATE %s
                SET statusnotificacao = 'EN',
                    datanotificacao = CURRENT_TIMESTAMP,
                    quantidadetentativas = quantidadetentativas + 1,
                    ultimamensagemerro = NULL,
                    datareservaprocessamento = NULL,
                    identificadorreserva = NULL,
                    dataatualizacao = CURRENT_TIMESTAMP
                WHERE id = :id
                  AND identificadorreserva = :identificadorReserva
                  AND origemregistro = 'N'
                  AND statusnotificacao IN ('PE', 'ER')
                """.formatted(tabelaConhecida);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("identificadorReserva", identificadorReserva);
        return jdbcTemplate.update(sql, params) > 0;
    }

    public boolean marcarNotificacaoErro(Long id, String identificadorReserva, String mensagemErro) {
        String sql = """
                UPDATE %s
                SET statusnotificacao = 'ER',
                    quantidadetentativas = quantidadetentativas + 1,
                    ultimamensagemerro = :mensagemErro,
                    datareservaprocessamento = NULL,
                    identificadorreserva = NULL,
                    dataatualizacao = CURRENT_TIMESTAMP
                WHERE id = :id
                  AND identificadorreserva = :identificadorReserva
                  AND origemregistro = 'N'
                  AND statusnotificacao IN ('PE', 'ER')
                """.formatted(tabelaConhecida);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("identificadorReserva", identificadorReserva)
                .addValue("mensagemErro", mensagemErro);
        return jdbcTemplate.update(sql, params) > 0;
    }

    private boolean inserirConhecida(EventoFuncionalNotificacao evento, String origem, String status) {
        if (evento == null || isBlank(evento.matricula()) || isBlank(evento.chaveEvento())) {
            return false;
        }
        String sql = """
                INSERT INTO %s (
                    servidormatricula,
                    chaveevento,
                    tipoevento,
                    rota,
                    titulo,
                    corpo,
                    origemregistro,
                    statusnotificacao
                )
                VALUES (
                    :matricula,
                    :chaveEvento,
                    :tipoEvento,
                    :rota,
                    :titulo,
                    :corpo,
                    :origem,
                    :status
                )
                ON CONFLICT (servidormatricula, chaveevento) DO NOTHING
                """.formatted(tabelaConhecida);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("matricula", evento.matricula())
                .addValue("chaveEvento", limit(evento.chaveEvento(), 160))
                .addValue("tipoEvento", limit(evento.tipoEvento(), 64))
                .addValue("rota", limit(evento.rota(), 128))
                .addValue("titulo", limit(evento.titulo(), 120))
                .addValue("corpo", limit(evento.corpo(), 220))
                .addValue("origem", origem)
                .addValue("status", status);
        try {
            return jdbcTemplate.update(sql, params) > 0;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    private EventoFuncionalPendenteNotificacao mapPendente(ResultSet rs, int rowNum) throws SQLException {
        EventoFuncionalNotificacao evento = new EventoFuncionalNotificacao(
                rs.getString("chaveevento"),
                rs.getString("servidormatricula"),
                rs.getString("tipoevento"),
                rs.getString("rota"),
                rs.getString("titulo"),
                rs.getString("corpo"));
        return new EventoFuncionalPendenteNotificacao(
                rs.getLong("id"),
                rs.getString("identificadorreserva"),
                rs.getString("statusnotificacao"),
                rs.getInt("quantidadetentativas"),
                evento);
    }

    private MapSqlParameterSource params(String matricula) {
        return new MapSqlParameterSource("matricula", matricula);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String limit(String value, int max) {
        String normalized = value == null ? "" : value.trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }
}
