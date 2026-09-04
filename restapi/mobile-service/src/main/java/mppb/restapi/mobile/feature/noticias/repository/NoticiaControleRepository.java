package mppb.restapi.mobile.feature.noticias.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import mppb.restapi.mobile.feature.noticias.model.NoticiaPendenteNotificacao;
import mppb.restapi.mobile.feature.noticias.model.NoticiaPublicada;

@Repository
public class NoticiaControleRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public NoticiaControleRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean possuiNoticiasConhecidas() {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM diariooficial.noticiaconhecida
                )
                """;
        Boolean exists = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    public int registrarConhecidasIniciais(List<NoticiaPublicada> noticias) {
        int total = 0;
        for (NoticiaPublicada noticia : noticias) {
            if (inserirConhecida(noticia, "I", "NA")) {
                total++;
            }
        }
        return total;
    }

    public boolean registrarNova(NoticiaPublicada noticia) {
        return inserirConhecida(noticia, "N", "PE");
    }

    @Transactional
    public List<NoticiaPendenteNotificacao> reservarPendentesParaNotificacao(
            int batchSize,
            int maxTentativas,
            String identificadorReserva) {
        String sql = """
                WITH candidatas AS (
                    SELECT id
                    FROM diariooficial.noticiaconhecida
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
                UPDATE diariooficial.noticiaconhecida nc
                SET datareservaprocessamento = CURRENT_TIMESTAMP,
                    identificadorreserva = :identificadorReserva,
                    dataatualizacao = CURRENT_TIMESTAMP
                FROM candidatas
                WHERE nc.id = candidatas.id
                RETURNING
                    nc.id,
                    nc.identificadorreserva,
                    nc.chaveexterna,
                    nc.statusnotificacao,
                    nc.quantidadetentativas
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("batchSize", batchSize)
                .addValue("maxTentativas", maxTentativas)
                .addValue("identificadorReserva", identificadorReserva);
        return jdbcTemplate.query(sql, params, this::mapPendente);
    }

    public boolean marcarNotificacaoEnviada(Long id, String identificadorReserva) {
        String sql = """
                UPDATE diariooficial.noticiaconhecida
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
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("identificadorReserva", identificadorReserva);
        return jdbcTemplate.update(sql, params) > 0;
    }

    public boolean marcarNotificacaoErro(Long id, String identificadorReserva, String mensagemErro) {
        String sql = """
                UPDATE diariooficial.noticiaconhecida
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
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("identificadorReserva", identificadorReserva)
                .addValue("mensagemErro", mensagemErro);
        return jdbcTemplate.update(sql, params) > 0;
    }

    private boolean inserirConhecida(NoticiaPublicada noticia, String origem, String status) {
        if (noticia.chaveExterna() == null || noticia.chaveExterna().isBlank()) {
            return false;
        }
        String sql = """
                INSERT INTO diariooficial.noticiaconhecida (
                    chaveexterna,
                    link,
                    titulohash,
                    datapublicacao,
                    origemregistro,
                    statusnotificacao
                )
                VALUES (
                    :chaveExterna,
                    :link,
                    :tituloHash,
                    :dataPublicacao,
                    :origem,
                    :status
                )
                ON CONFLICT (chaveexterna) DO NOTHING
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("chaveExterna", noticia.chaveExterna())
                .addValue("link", noticia.link())
                .addValue("tituloHash", sha256(noticia.titulo() == null ? "" : noticia.titulo()))
                .addValue("dataPublicacao", noticia.dataPublicacao() == null
                        ? null
                        : Timestamp.valueOf(noticia.dataPublicacao()))
                .addValue("origem", origem)
                .addValue("status", status);
        try {
            return jdbcTemplate.update(sql, params) > 0;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao calcular hash da noticia", e);
        }
    }

    private NoticiaPendenteNotificacao mapPendente(ResultSet rs, int rowNum) throws SQLException {
        return new NoticiaPendenteNotificacao(
                rs.getLong("id"),
                rs.getString("identificadorreserva"),
                rs.getString("chaveexterna"),
                rs.getString("statusnotificacao"),
                rs.getInt("quantidadetentativas"));
    }
}
