package mppb.restapi.mobile.feature.notificacao.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import mppb.restapi.mobile.feature.notificacao.model.DispositivoFcm;

@Repository
public class DispositivoFcmRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DispositivoFcmRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<DispositivoFcm> findByIdentificador(String identificador) {
        String sql = """
                SELECT *
                FROM diariooficial.dispositivofcm
                WHERE identificador = :identificador
                """;
        return queryOne(sql, new MapSqlParameterSource("identificador", identificador));
    }

    public Optional<DispositivoFcm> findByTokenHash(String tokenFcmHash) {
        String sql = """
                SELECT *
                FROM diariooficial.dispositivofcm
                WHERE tokenfcmhash = :tokenFcmHash
                """;
        return queryOne(sql, new MapSqlParameterSource("tokenFcmHash", tokenFcmHash));
    }

    public List<DispositivoFcm> findAtivosByMatricula(String matricula) {
        String sql = """
                SELECT *
                FROM diariooficial.dispositivofcm
                WHERE servidormatricula = :matricula
                  AND ativo = TRUE
                ORDER BY id
                """;
        return jdbcTemplate.queryForList(sql, new MapSqlParameterSource("matricula", matricula))
                .stream()
                .map(this::mapRow)
                .toList();
    }

    public DispositivoFcm inserir(
            String identificador,
            String matricula,
            String tokenFcm,
            String tokenFcmHash,
            String plataforma) {
        String sql = """
                INSERT INTO diariooficial.dispositivofcm (
                    identificador,
                    servidormatricula,
                    tokenfcm,
                    tokenfcmhash,
                    plataforma,
                    ativo,
                    dataultimouso
                )
                VALUES (
                    :identificador,
                    :matricula,
                    :tokenFcm,
                    :tokenFcmHash,
                    :plataforma,
                    TRUE,
                    CURRENT_TIMESTAMP
                )
                RETURNING *
                """;
        MapSqlParameterSource params = baseParams(null, matricula, tokenFcm, tokenFcmHash, plataforma)
                .addValue("identificador", identificador);
        return queryOne(sql, params).orElseThrow();
    }

    public DispositivoFcm atualizar(
            Long id,
            String matricula,
            String tokenFcm,
            String tokenFcmHash,
            String plataforma) {
        String sql = """
                UPDATE diariooficial.dispositivofcm
                SET servidormatricula = :matricula,
                    tokenfcm = :tokenFcm,
                    tokenfcmhash = :tokenFcmHash,
                    plataforma = :plataforma,
                    ativo = TRUE,
                    dataultimouso = CURRENT_TIMESTAMP,
                    dataultimaatualizacao = CURRENT_TIMESTAMP
                WHERE id = :id
                RETURNING *
                """;
        return queryOne(sql, baseParams(id, matricula, tokenFcm, tokenFcmHash, plataforma)).orElseThrow();
    }

    public boolean desativar(String matricula, String identificador) {
        String sql = """
                UPDATE diariooficial.dispositivofcm
                SET ativo = FALSE,
                    dataultimaatualizacao = CURRENT_TIMESTAMP
                WHERE identificador = :identificador
                  AND servidormatricula = :matricula
                  AND ativo = TRUE
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("identificador", identificador)
                .addValue("matricula", matricula);
        return jdbcTemplate.update(sql, params) > 0;
    }

    public boolean desativarPorId(Long id) {
        String sql = """
                UPDATE diariooficial.dispositivofcm
                SET ativo = FALSE,
                    dataultimaatualizacao = CURRENT_TIMESTAMP
                WHERE id = :id
                  AND ativo = TRUE
                """;
        return jdbcTemplate.update(sql, new MapSqlParameterSource("id", id)) > 0;
    }

    private Optional<DispositivoFcm> queryOne(String sql, MapSqlParameterSource params) {
        List<DispositivoFcm> rows = jdbcTemplate.queryForList(sql, params)
                .stream()
                .map(this::mapRow)
                .toList();
        return rows.stream().findFirst();
    }

    private MapSqlParameterSource baseParams(
            Long id,
            String matricula,
            String tokenFcm,
            String tokenFcmHash,
            String plataforma) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("matricula", matricula)
                .addValue("tokenFcm", tokenFcm)
                .addValue("tokenFcmHash", tokenFcmHash)
                .addValue("plataforma", plataforma);
    }

    private DispositivoFcm mapRow(Map<String, Object> row) {
        return new DispositivoFcm(
                longValue(row.get("id")),
                stringValue(row.get("identificador")),
                stringValue(row.get("servidormatricula")),
                stringValue(row.get("tokenfcm")),
                stringValue(row.get("tokenfcmhash")),
                stringValue(row.get("plataforma")),
                booleanValue(row.get("ativo")),
                localDateTimeValue(row.get("datacriacao")),
                localDateTimeValue(row.get("dataultimaatualizacao")),
                localDateTimeValue(row.get("dataultimouso")));
    }

    private static Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return Long.parseLong(value.toString());
    }

    private static String stringValue(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return value.toString().trim();
    }

    private static boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && Boolean.parseBoolean(value.toString());
    }

    private static LocalDateTime localDateTimeValue(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value.toString().replace(' ', 'T'));
    }
}
