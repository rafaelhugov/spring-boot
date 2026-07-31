package mppb.restapi.mobile.feature.designacao.repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import mppb.restapi.mobile.feature.designacao.model.DesignacaoAtivaRow;
import mppb.restapi.mobile.feature.designacao.model.DesignacaoPendenteNotificacao;

@Repository
public class DesignacaoViewRepository {

    private static final String SELECT_ATIVAS = """
            SELECT
                m.designacao_id,
                m.servidor_matricula,
                m.designacao_tipoletra,
                m.designacao_tipo,
                m.funcao_nome,
                m.orgao_nome,
                m.orgao_execucao_nome,
                m.designacao_data_inicial,
                m.designacao_data_final,
                m.diario_oficial_data_publicacao,
                m.ato_numero,
                CASE
                    WHEN UPPER(TRIM(arq.nomearquivo)) LIKE 'INI\\_%' ESCAPE '\\' THEN TRUE
                    ELSE FALSE
                END AS documento_inicial,
                m.tipocargo,
                m.tipocargo_nome
            FROM diariooficial.mapa_por_membro_vw m
            LEFT JOIN ged.arquivo arq
                ON arq.id = m.diario_oficial_documento_id
            WHERE m.ativo = 'A'
              AND m.servidor_matricula::text = :matricula
            ORDER BY
                m.designacao_data_inicial DESC NULLS LAST,
                m.designacao_id DESC
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DesignacaoViewRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<DesignacaoAtivaRow> findAtivasByMatricula(String matricula) {
        return jdbcTemplate.queryForList(SELECT_ATIVAS, new MapSqlParameterSource("matricula", matricula))
                .stream()
                .map(this::mapRow)
                .toList();
    }

    public List<DesignacaoPendenteNotificacao> findNovasParaUsuariosInicializados(int batchSize) {
        String sql = """
                SELECT
                    c.id AS controle_id,
                    m.designacao_id,
                    m.servidor_matricula,
                    m.designacao_tipoletra,
                    m.designacao_tipo,
                    m.funcao_nome,
                    m.orgao_nome,
                    m.orgao_execucao_nome,
                    m.designacao_data_inicial,
                    m.designacao_data_final,
                    m.diario_oficial_data_publicacao,
                    m.ato_numero,
                    CASE
                        WHEN UPPER(TRIM(arq.nomearquivo)) LIKE 'INI\\_%' ESCAPE '\\' THEN TRUE
                        ELSE FALSE
                    END AS documento_inicial,
                    m.tipocargo,
                    m.tipocargo_nome
                FROM diariooficial.mapa_por_membro_vw m
                LEFT JOIN ged.arquivo arq
                    ON arq.id = m.diario_oficial_documento_id
                INNER JOIN diariooficial.designacaousuariocontrole c
                    ON c.servidor_matricula = m.servidor_matricula::text
                WHERE m.ativo = 'A'
                  AND c.inicializado = TRUE
                  AND NOT EXISTS (
                      SELECT 1
                      FROM diariooficial.designacaoconhecida dc
                      WHERE dc.servidormatricula = m.servidor_matricula::text
                        AND dc.id_designacao = m.designacao_id
                        AND dc.tipodesignacao = m.designacao_tipoletra::text
                  )
                ORDER BY
                    c.dataultimoprocessamento NULLS FIRST,
                    m.designacao_data_inicial DESC NULLS LAST,
                    m.designacao_id DESC
                LIMIT :batchSize
                """;
        return jdbcTemplate.queryForList(sql, new MapSqlParameterSource("batchSize", batchSize))
                .stream()
                .map(row -> new DesignacaoPendenteNotificacao(
                        longValue(row.get("controle_id")),
                        stringValue(row.get("servidor_matricula")),
                        mapRow(row)))
                .toList();
    }

    private DesignacaoAtivaRow mapRow(Map<String, Object> row) {
        return new DesignacaoAtivaRow(
                longValue(row.get("designacao_id")),
                stringValue(row.get("servidor_matricula")),
                stringValue(row.get("designacao_tipoletra")),
                stringValue(row.get("designacao_tipo")),
                stringValue(row.get("funcao_nome")),
                stringValue(row.get("orgao_nome")),
                stringValue(row.get("orgao_execucao_nome")),
                localDateValue(row.get("designacao_data_inicial")),
                localDateValue(row.get("designacao_data_final")),
                localDateValue(row.get("diario_oficial_data_publicacao")),
                stringValue(row.get("ato_numero")),
                booleanValue(row.get("documento_inicial")),
                stringValue(row.get("tipocargo")),
                stringValue(row.get("tipocargo_nome")));
    }

    private static String stringValue(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return value.toString().trim();
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

    private static LocalDate localDateValue(Object value) {
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return LocalDate.parse(value.toString().substring(0, 10));
    }

    private static boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && Boolean.parseBoolean(value.toString());
    }
}
