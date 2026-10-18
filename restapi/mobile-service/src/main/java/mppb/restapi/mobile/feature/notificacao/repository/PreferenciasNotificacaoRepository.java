package mppb.restapi.mobile.feature.notificacao.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import mppb.restapi.mobile.feature.notificacao.model.PreferenciasNotificacao;

@Repository
@RequiredArgsConstructor
public class PreferenciasNotificacaoRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public Optional<PreferenciasNotificacao> buscar(String matricula) {
        String sql = """
                SELECT noticias, designacoes, contracheques, pagamentos, afastamentos
                FROM diariooficial.preferencianotificacao
                WHERE servidormatricula = :matricula
                """;
        List<PreferenciasNotificacao> resultados = jdbcTemplate.queryForList(
                        sql,
                        new MapSqlParameterSource("matricula", matricula))
                .stream()
                .map(this::mapear)
                .toList();
        return resultados.stream().findFirst();
    }

    public PreferenciasNotificacao salvar(String matricula, PreferenciasNotificacao preferencias) {
        String sql = """
                INSERT INTO diariooficial.preferencianotificacao (
                    servidormatricula, noticias, designacoes, contracheques, pagamentos, afastamentos
                ) VALUES (
                    :matricula, :noticias, :designacoes, :contracheques, :pagamentos, :afastamentos
                )
                ON CONFLICT (servidormatricula) DO UPDATE SET
                    noticias = EXCLUDED.noticias,
                    designacoes = EXCLUDED.designacoes,
                    contracheques = EXCLUDED.contracheques,
                    pagamentos = EXCLUDED.pagamentos,
                    afastamentos = EXCLUDED.afastamentos,
                    dataatualizacao = CURRENT_TIMESTAMP
                RETURNING noticias, designacoes, contracheques, pagamentos, afastamentos
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("matricula", matricula)
                .addValue("noticias", preferencias.noticias())
                .addValue("designacoes", preferencias.designacoes())
                .addValue("contracheques", preferencias.contracheques())
                .addValue("pagamentos", preferencias.pagamentos())
                .addValue("afastamentos", preferencias.afastamentos());
        return jdbcTemplate.queryForList(sql, params).stream()
                .map(this::mapear)
                .findFirst()
                .orElseThrow();
    }

    private PreferenciasNotificacao mapear(Map<String, Object> row) {
        return new PreferenciasNotificacao(
                Boolean.TRUE.equals(row.get("noticias")),
                Boolean.TRUE.equals(row.get("designacoes")),
                Boolean.TRUE.equals(row.get("contracheques")),
                Boolean.TRUE.equals(row.get("pagamentos")),
                Boolean.TRUE.equals(row.get("afastamentos")));
    }
}
