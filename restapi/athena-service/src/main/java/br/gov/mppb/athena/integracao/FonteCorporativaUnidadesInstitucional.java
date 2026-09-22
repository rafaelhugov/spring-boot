package br.gov.mppb.athena.integracao;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

public final class FonteCorporativaUnidadesInstitucional implements FonteCorporativaUnidades {

    private static final int LIMITE_RESULTADOS = 100;
    private static final String CONSULTA_SQL = """
            SELECT o.id_pessoa, p.nome, o.sigla
              FROM public.orgao o
              JOIN public.pessoa p ON p.id = o.id_pessoa
             WHERE COALESCE(o.excluido, FALSE) = FALSE
               AND COALESCE(p.excluido, FALSE) = FALSE
               AND (lower(p.nome) LIKE ? OR lower(COALESCE(o.sigla, '')) LIKE ?
                    OR CAST(o.id_pessoa AS VARCHAR) LIKE ?)
             ORDER BY p.nome, o.sigla
             LIMIT ?
            """;

    private final JdbcTemplate jdbc;

    public FonteCorporativaUnidadesInstitucional(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<UnidadeCorporativa> pesquisar(String termo) {
        if (termo == null || termo.isBlank()) {
            throw new IllegalArgumentException("termo de pesquisa da unidade deve ser informado.");
        }
        String padrao = "%" + termo.trim().toLowerCase() + "%";
        return jdbc.query(CONSULTA_SQL, (rs, linha) -> new UnidadeCorporativa(
                rs.getInt("id_pessoa"), rs.getString("nome"), rs.getString("sigla")
        ), padrao, padrao, padrao, LIMITE_RESULTADOS);
    }

    static String consultaSql() {
        return CONSULTA_SQL;
    }
}
