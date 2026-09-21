package br.gov.mppb.athena.integracao;

import br.gov.mppb.athena.seguranca.DadosParticipanteCorporativo;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

public final class FonteCorporativaParticipantesInstitucional implements FonteCorporativaParticipantes {

    private static final int LIMITE_RESULTADOS = 50;
    private static final String CONSULTA_SQL = """
            SELECT DISTINCT us.nome,
                   us.matricula,
                   COALESCE(l.funcaolotacao, 'Institucional') AS vinculo,
                   l.funcao_nome AS cargo_funcao,
                   us.login,
                   COALESCE(l.orgao_nome, o.gruponome) AS unidade_lotacao
              FROM autorizacao.usuarioservidor_vw us
              LEFT JOIN LATERAL (
                   SELECT lot.funcaolotacao, lot.funcao_nome, lot.orgao_nome
                     FROM diariooficial.lotacao_vw lot
                    WHERE lot.servidor_id = us.id_servidor
                      AND (lot.ativo IS NULL OR upper(lot.ativo) IN ('A', 'S', 'SIM', 'ATIVO'))
                    ORDER BY lot.designacao_data_inicial DESC NULLS LAST, lot.designacao_id DESC
                    LIMIT 1
              ) l ON TRUE
              LEFT JOIN LATERAL (
                   SELECT org.gruponome
                     FROM diariooficial.orgaos_dos_usuarios_ativos_vw org
                    WHERE lower(org.login) = lower(us.login)
                    ORDER BY org.gruponome
                    LIMIT 1
              ) o ON TRUE
             WHERE (? IS NOT NULL AND (
                        lower(us.nome) LIKE ?
                     OR lower(us.login) LIKE ?
                     OR lower(us.matricula) LIKE ?
                     OR lower(COALESCE(l.funcao_nome, '')) LIKE ?
                     OR lower(COALESCE(l.orgao_nome, o.gruponome, '')) LIKE ?
                   ))
                OR (? IS NOT NULL AND lower(us.matricula) LIKE ?)
                OR (? IS NOT NULL AND lower(COALESCE(l.orgao_nome, o.gruponome, '')) LIKE ?)
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
                consulta.termo(), termo, termo, termo, termo, termo,
                consulta.matricula(), matricula,
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
