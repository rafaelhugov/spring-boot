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
                SELECT push_noticias, push_designacoes, push_contracheques, push_pagamentos, push_afastamentos,
                       central_noticias, central_designacoes, central_contracheques, central_pagamentos, central_afastamentos
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
                    servidormatricula, push_noticias, push_designacoes, push_contracheques, push_pagamentos, push_afastamentos,
                    central_noticias, central_designacoes, central_contracheques, central_pagamentos, central_afastamentos
                ) VALUES (
                    :matricula, :pushNoticias, :pushDesignacoes, :pushContracheques, :pushPagamentos, :pushAfastamentos,
                    :centralNoticias, :centralDesignacoes, :centralContracheques, :centralPagamentos, :centralAfastamentos
                )
                ON CONFLICT (servidormatricula) DO UPDATE SET
                    push_noticias = EXCLUDED.push_noticias, push_designacoes = EXCLUDED.push_designacoes,
                    push_contracheques = EXCLUDED.push_contracheques, push_pagamentos = EXCLUDED.push_pagamentos,
                    push_afastamentos = EXCLUDED.push_afastamentos, central_noticias = EXCLUDED.central_noticias,
                    central_designacoes = EXCLUDED.central_designacoes, central_contracheques = EXCLUDED.central_contracheques,
                    central_pagamentos = EXCLUDED.central_pagamentos, central_afastamentos = EXCLUDED.central_afastamentos,
                    dataatualizacao = CURRENT_TIMESTAMP
                RETURNING push_noticias, push_designacoes, push_contracheques, push_pagamentos, push_afastamentos,
                          central_noticias, central_designacoes, central_contracheques, central_pagamentos, central_afastamentos
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("matricula", matricula)
                .addValue("pushNoticias", preferencias.pushNoticias())
                .addValue("pushDesignacoes", preferencias.pushDesignacoes())
                .addValue("pushContracheques", preferencias.pushContracheques())
                .addValue("pushPagamentos", preferencias.pushPagamentos())
                .addValue("pushAfastamentos", preferencias.pushAfastamentos())
                .addValue("centralNoticias", preferencias.centralNoticias())
                .addValue("centralDesignacoes", preferencias.centralDesignacoes())
                .addValue("centralContracheques", preferencias.centralContracheques())
                .addValue("centralPagamentos", preferencias.centralPagamentos())
                .addValue("centralAfastamentos", preferencias.centralAfastamentos());
        return jdbcTemplate.queryForList(sql, params).stream()
                .map(this::mapear)
                .findFirst()
                .orElseThrow();
    }

    private PreferenciasNotificacao mapear(Map<String, Object> row) {
        return new PreferenciasNotificacao(
                Boolean.TRUE.equals(row.get("push_noticias")), Boolean.TRUE.equals(row.get("push_designacoes")),
                Boolean.TRUE.equals(row.get("push_contracheques")), Boolean.TRUE.equals(row.get("push_pagamentos")),
                Boolean.TRUE.equals(row.get("push_afastamentos")), Boolean.TRUE.equals(row.get("central_noticias")),
                Boolean.TRUE.equals(row.get("central_designacoes")), Boolean.TRUE.equals(row.get("central_contracheques")),
                Boolean.TRUE.equals(row.get("central_pagamentos")), Boolean.TRUE.equals(row.get("central_afastamentos")));
    }
}
