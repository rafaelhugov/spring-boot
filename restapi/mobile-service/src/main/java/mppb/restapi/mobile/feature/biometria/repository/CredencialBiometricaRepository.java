package mppb.restapi.mobile.feature.biometria.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import mppb.restapi.mobile.feature.biometria.model.CredencialBiometrica;

@Repository
public class CredencialBiometricaRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CredencialBiometricaRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public CredencialBiometrica salvar(
            String seletor,
            String segredoHash,
            String login,
            String dispositivoId,
            String plataforma,
            String tokenInstitucionalCifrado,
            LocalDateTime expiraEm) {
        String sql = """
                INSERT INTO diariooficial.credencialbiometrica (
                    seletor, segredohash, login, dispositivoid, plataforma,
                    tokeninstitucionalcifrado, ativa, expiraem, dataultimouso
                ) VALUES (
                    :seletor, :segredoHash, :login, :dispositivoId, :plataforma,
                    :tokenCifrado, TRUE, :expiraEm, NULL
                )
                ON CONFLICT (login, dispositivoid) DO UPDATE SET
                    seletor = EXCLUDED.seletor,
                    segredohash = EXCLUDED.segredohash,
                    plataforma = EXCLUDED.plataforma,
                    tokeninstitucionalcifrado = EXCLUDED.tokeninstitucionalcifrado,
                    ativa = TRUE,
                    expiraem = EXCLUDED.expiraem,
                    dataultimaatualizacao = CURRENT_TIMESTAMP,
                    dataultimouso = NULL
                RETURNING *
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("seletor", UUID.fromString(seletor))
                .addValue("segredoHash", segredoHash)
                .addValue("login", login)
                .addValue("dispositivoId", dispositivoId)
                .addValue("plataforma", plataforma)
                .addValue("tokenCifrado", tokenInstitucionalCifrado)
                .addValue("expiraEm", Timestamp.valueOf(expiraEm));
        return queryOne(sql, params).orElseThrow();
    }

    public Optional<CredencialBiometrica> findAtivaBySeletor(String seletor) {
        String sql = """
                SELECT *
                FROM diariooficial.credencialbiometrica
                WHERE seletor = :seletor
                  AND ativa = TRUE
                """;
        return queryOne(sql, new MapSqlParameterSource("seletor", UUID.fromString(seletor)));
    }

    public void registrarUso(Long id) {
        String sql = """
                UPDATE diariooficial.credencialbiometrica
                SET dataultimouso = CURRENT_TIMESTAMP,
                    dataultimaatualizacao = CURRENT_TIMESTAMP
                WHERE id = :id AND ativa = TRUE
                """;
        jdbcTemplate.update(sql, new MapSqlParameterSource("id", id));
    }

    public boolean desativar(String login, String dispositivoId) {
        String sql = """
                UPDATE diariooficial.credencialbiometrica
                SET ativa = FALSE,
                    tokeninstitucionalcifrado = '',
                    dataultimaatualizacao = CURRENT_TIMESTAMP
                WHERE login = :login
                  AND dispositivoid = :dispositivoId
                  AND ativa = TRUE
                """;
        return jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("login", login)
                .addValue("dispositivoId", dispositivoId)) > 0;
    }

    public void desativarPorId(Long id) {
        String sql = """
                UPDATE diariooficial.credencialbiometrica
                SET ativa = FALSE,
                    tokeninstitucionalcifrado = '',
                    dataultimaatualizacao = CURRENT_TIMESTAMP
                WHERE id = :id
                """;
        jdbcTemplate.update(sql, new MapSqlParameterSource("id", id));
    }

    public int desativarExpiradas() {
        String sql = """
                UPDATE diariooficial.credencialbiometrica
                SET ativa = FALSE,
                    tokeninstitucionalcifrado = '',
                    dataultimaatualizacao = CURRENT_TIMESTAMP
                WHERE ativa = TRUE
                  AND expiraem <= CURRENT_TIMESTAMP
                """;
        return jdbcTemplate.update(sql, new MapSqlParameterSource());
    }

    private Optional<CredencialBiometrica> queryOne(String sql, MapSqlParameterSource params) {
        List<CredencialBiometrica> rows = jdbcTemplate.queryForList(sql, params).stream()
                .map(this::mapRow)
                .toList();
        return rows.stream().findFirst();
    }

    private CredencialBiometrica mapRow(Map<String, Object> row) {
        return new CredencialBiometrica(
                longValue(row.get("id")),
                stringValue(row.get("seletor")),
                stringValue(row.get("segredohash")),
                stringValue(row.get("login")),
                stringValue(row.get("dispositivoid")),
                stringValue(row.get("plataforma")),
                stringValue(row.get("tokeninstitucionalcifrado")),
                booleanValue(row.get("ativa")),
                localDateTimeValue(row.get("expiraem")),
                localDateTimeValue(row.get("datacriacao")),
                localDateTimeValue(row.get("dataultimaatualizacao")),
                localDateTimeValue(row.get("dataultimouso")));
    }

    private static Long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : Long.valueOf(value.toString());
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private static boolean booleanValue(Object value) {
        return value instanceof Boolean bool ? bool : Boolean.parseBoolean(String.valueOf(value));
    }

    private static LocalDateTime localDateTimeValue(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return value instanceof LocalDateTime localDateTime ? localDateTime : null;
    }
}
