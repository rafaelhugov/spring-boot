package mppb.restapi.mobile.feature.diarias.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import mppb.restapi.mobile.feature.diarias.dto.DiariaPagamentoDto;

@Repository
public class DiariasRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DiariasRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String getCpfByLogin(String login) {
        String sql = "SELECT cpf FROM autorizacao.usuario WHERE login = ? AND excluido = false ORDER BY id DESC LIMIT 1";
        try {
            return jdbcTemplate.queryForObject(sql, String.class, login);
        } catch (Exception e) {
            return null;
        }
    }

    public List<DiariaPagamentoDto> findDiariasByExercicioAndCpf(String exercicio, String cpf) {
        String sql =
            "SELECT " +
            "  tt.cd_orgao, " +
            "  tt.sigla, " +
            "  tt.nu_exercicio, " +
            "  tt.documento, " +
            "  tt.dt_pagamento, " +
            "  tt.vl_bruto, " +
            "  tt.vl_liquido, " +
            "  tt.dsmodalidadepag, " +
            "  ne.nu_processo, " +
            "  SUBSTRING(soe.fk_natureza FROM 5 FOR 2) AS cdelemento, " +
            "  elem.ds AS descricao_elemento " +
            "FROM ( " +
            "  SELECT " +
            "    ap.fk_cnpj, " +
            "    org.cd_orgao, " +
            "    org.sigla, " +
            "    ap.nu_exercicio, " +
            "    ap.dt_movimento, " +
            "    CASE " +
            "      WHEN length(ap.nu_documento::text) = 1 THEN '0000' || ap.nu_documento::text " +
            "      WHEN length(ap.nu_documento::text) = 2 THEN '000'  || ap.nu_documento::text " +
            "      WHEN length(ap.nu_documento::text) = 3 THEN '00'   || ap.nu_documento::text " +
            "      WHEN length(ap.nu_documento::text) = 4 THEN '0'    || ap.nu_documento::text " +
            "      ELSE ap.nu_documento::text " +
            "    END AS documento, " +
            "    ap.dt_pagamento, " +
            "    ap.vl_bruto, " +
            "    ap.vl_liquido, " +
            "    ap.nu_cnpj_cpf, " +
            "    mp.ds AS dsmodalidadepag, " +
            "    CASE " +
            "      WHEN ap.cd_inscricao_rp <> '00000' THEN rp.nu_ne_origem::text " +
            "      ELSE ap.nu_ne::text " +
            "    END AS nu_ne, " +
            "    CASE " +
            "      WHEN ap.cd_inscricao_rp <> '00000' THEN rp.fk_exercicio " +
            "      ELSE ap.nu_exercicio " +
            "    END AS exercicione, " +
            "    CASE " +
            "      WHEN ap.nu_class > 0 THEN ap.nu_class::text " +
            "      WHEN rp.nu_classificacao::integer > 0 THEN rp.nu_classificacao::text " +
            "    END AS nuclass, " +
            "    CASE " +
            "      WHEN ap.nu_documento_principal = 0 THEN '' " +
            "      WHEN length(ap.nu_documento_principal::text) = 1 THEN '0000' || ap.nu_documento_principal::text " +
            "      WHEN length(ap.nu_documento_principal::text) = 2 THEN '000'  || ap.nu_documento_principal::text " +
            "      WHEN length(ap.nu_documento_principal::text) = 3 THEN '00'   || ap.nu_documento_principal::text " +
            "      WHEN length(ap.nu_documento_principal::text) = 4 THEN '0'    || ap.nu_documento_principal::text " +
            "      ELSE ap.nu_documento_principal::text " +
            "    END AS apprincipal, " +
            "    ap.nu_documento " +
            "  FROM siaf.siaf_tb_autorizacao_pagamento ap " +
            "  LEFT JOIN siaf.siaf_tb_modalidade_pagamento mp ON mp.id = ap.cd_modalidade " +
            "  LEFT JOIN siaf.seg_tb_cliente org ON org.nu_cnpj = ap.fk_cnpj " +
            "  LEFT JOIN siaf.siaf_tb_restos_pagar_difin rp " +
            "         ON rp.fk_cnpj = ap.fk_cnpj " +
            "        AND rp.fk_exercicio = ap.nu_exercicio_rp " +
            "        AND rp.nu_inscricao::integer = ap.cd_inscricao_rp " +
            "  WHERE ap.nu_exercicio = ? " +
            ") tt " +
            "LEFT JOIN siaf.siaf_tb_dotacao_orcamentaria soe " +
            "       ON soe.fk_cnpj = tt.fk_cnpj " +
            "      AND soe.fk_exercicio = tt.exercicione " +
            "      AND soe.nu_classificacao::integer = tt.nuclass::integer " +
            "LEFT JOIN siaf.par_tb_elemento_despesa elem ON elem.id = SUBSTRING(soe.fk_natureza FROM 5 FOR 2) " +
            "LEFT JOIN siaf.siaf_tb_nota_empenho ne " +
            "       ON ne.fk_cnpj = tt.fk_cnpj " +
            "      AND ne.nu_exercicio = tt.exercicione " +
            "      AND ne.nu_empenho = tt.nu_ne::integer " +
            "WHERE tt.nu_exercicio = ? " +
            "  AND tt.nu_cnpj_cpf = ? " +
            "  AND tt.nuclass IS NOT NULL " +
            "  AND tt.apprincipal = '' " +
            "  AND SUBSTRING(soe.fk_natureza FROM 5 FOR 2) IN ('14') " +
            "ORDER BY tt.fk_cnpj, tt.nu_documento";

        return jdbcTemplate.query(sql, new RowMapper<DiariaPagamentoDto>() {
            @Override
            public DiariaPagamentoDto mapRow(ResultSet rs, int rowNum) throws SQLException {
                DiariaPagamentoDto dto = new DiariaPagamentoDto();
                dto.setCdOrgao(rs.getString("cd_orgao"));
                dto.setSigla(rs.getString("sigla"));
                dto.setNuExercicio(rs.getString("nu_exercicio"));
                dto.setDocumento(rs.getString("documento"));

                java.sql.Timestamp ts = rs.getTimestamp("dt_pagamento");
                if (ts != null) {
                    dto.setDtPagamento(
                        new java.text.SimpleDateFormat("dd/MM/yyyy").format(ts)
                    );
                }

                dto.setVlBruto(rs.getBigDecimal("vl_bruto"));
                dto.setVlLiquido(rs.getBigDecimal("vl_liquido"));
                dto.setDsModalidadePag(rs.getString("dsmodalidadepag"));
                dto.setNuProcesso(rs.getString("nu_processo"));
                dto.setCdElemento(rs.getString("cdelemento"));
                dto.setDescricaoElemento(rs.getString("descricao_elemento"));
                return dto;
            }
        }, exercicio, exercicio, cpf);
    }
}
