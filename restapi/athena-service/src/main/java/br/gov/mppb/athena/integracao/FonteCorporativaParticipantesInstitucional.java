package br.gov.mppb.athena.integracao;

import br.gov.mppb.athena.seguranca.DadosParticipanteCorporativo;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

public final class FonteCorporativaParticipantesInstitucional implements FonteCorporativaParticipantes {

    private static final int LIMITE_RESULTADOS = 50;
    private static final String CONSULTA_SQL = """
            WITH usuarios_filtrados AS MATERIALIZED (
                SELECT us.nome, us.matricula, us.login, us.id_servidor
                  FROM autorizacao.usuarioservidor_vw us
                 WHERE NULLIF(TRIM(us.login), '') IS NOT NULL
                   AND ((CAST(? AS VARCHAR) IS NOT NULL AND (
                            lower(us.nome) LIKE ?
                         OR lower(us.login) LIKE ?
                         OR lower(us.matricula) LIKE ?
                       ))
                    OR (CAST(? AS VARCHAR) IS NOT NULL AND lower(us.matricula) LIKE ?)
                    OR (CAST(? AS VARCHAR) IS NOT NULL
                        AND CAST(? AS VARCHAR) IS NULL
                        AND CAST(? AS VARCHAR) IS NULL))
            )
            SELECT DISTINCT us.nome,
                   us.matricula,
                   COALESCE(l.funcaolotacao, 'Institucional') AS vinculo,
                   l.funcao_nome AS cargo_funcao,
                   us.login,
                   COALESCE(unidade_hierarquia.sigla, l.orgao_nome, pessoa_unidade.nome) AS unidade_lotacao
              FROM usuarios_filtrados us
              LEFT JOIN LATERAL (
                   SELECT lot.funcaolotacao, lot.funcao_nome, lot.orgao_id, lot.orgao_nome
                     FROM diariooficial.lotacao_vw lot
                    WHERE lot.servidor_id = us.id_servidor
                      AND (lot.ativo IS NULL OR upper(lot.ativo) IN ('A', 'S', 'SIM', 'ATIVO'))
                    ORDER BY lot.designacao_data_inicial DESC NULLS LAST, lot.designacao_id DESC
                    LIMIT 1
              ) l ON TRUE
              LEFT JOIN public.servidor servidor ON servidor.id = us.id_servidor
              LEFT JOIN public.setor setor
                ON setor.id = servidor.id_setor
               AND COALESCE(setor.excluido, FALSE) = FALSE
              LEFT JOIN public.orgao orgao
                ON orgao.id_pessoa = setor.id_orgao
               AND COALESCE(orgao.excluido, FALSE) = FALSE
              LEFT JOIN public.pessoa pessoa_unidade
                ON pessoa_unidade.id = orgao.id_pessoa
               AND COALESCE(pessoa_unidade.excluido, FALSE) = FALSE
              LEFT JOIN LATERAL (
                   WITH RECURSIVE hierarquia AS (
                        SELECT atual.id_pessoa, atual.id_orgaosuperior, atual.sigla, 0 AS nivel
                          FROM public.orgao atual
                         WHERE atual.id_pessoa = COALESCE(l.orgao_id, orgao.id_pessoa)
                           AND COALESCE(atual.excluido, FALSE) = FALSE
                        UNION ALL
                        SELECT superior.id_pessoa, superior.id_orgaosuperior, superior.sigla, h.nivel + 1
                          FROM hierarquia h
                          JOIN public.orgao superior ON superior.id_pessoa = h.id_orgaosuperior
                         WHERE h.nivel < 10
                           AND COALESCE(superior.excluido, FALSE) = FALSE
                   )
                   SELECT h.sigla
                     FROM hierarquia h
                    WHERE NULLIF(TRIM(h.sigla), '') IS NOT NULL
                    ORDER BY h.nivel
                    LIMIT 1
              ) unidade_hierarquia ON TRUE
             WHERE CAST(? AS VARCHAR) IS NULL
                OR lower(COALESCE(unidade_hierarquia.sigla, l.orgao_nome, pessoa_unidade.nome, '')) LIKE ?
             ORDER BY us.nome, us.login
             LIMIT ?
            """;
    private final JdbcTemplate jdbc;

    public FonteCorporativaParticipantesInstitucional(JdbcTemplate jdbc) {
        if (jdbc == null) {
            throw new IllegalArgumentException("acesso corporativo deve ser informado.");
        }
        this.jdbc = jdbc;
    }

    @Override
    public List<DadosParticipanteCorporativo> pesquisar(ConsultaParticipantesCorporativos consulta) {
        if (consulta == null) {
            throw new IllegalArgumentException("consulta corporativa de participantes deve ser informada.");
        }
        String termo = comoPadrao(consulta.termo());
        String matricula = comoPadrao(consulta.matricula());
        String unidade = comoPadrao(consulta.unidadeOuLotacao());

        return jdbc.query(CONSULTA_SQL, (rs, linha) -> new DadosParticipanteCorporativo(
                        rs.getString("nome"),
                        rs.getString("matricula"),
                        rs.getString("vinculo"),
                        rs.getString("cargo_funcao"),
                        null,
                        rs.getString("login"),
                        rs.getString("unidade_lotacao"),
                        null
                ),
                consulta.termo(), termo, termo, termo,
                consulta.matricula(), matricula,
                consulta.unidadeOuLotacao(), consulta.termo(), consulta.matricula(),
                consulta.unidadeOuLotacao(), unidade,
                LIMITE_RESULTADOS);
    }

    static String consultaSql() {
        return CONSULTA_SQL;
    }

    private static String comoPadrao(String valor) {
        return valor == null ? null : "%" + valor.toLowerCase() + "%";
    }
}
