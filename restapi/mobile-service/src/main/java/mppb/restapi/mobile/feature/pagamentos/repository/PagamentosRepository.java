package mppb.restapi.mobile.feature.pagamentos.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import mppb.restapi.mobile.feature.pagamentos.dto.PagamentoItemDto;

@Repository
public class PagamentosRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public PagamentosRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String getCpfByLogin(String login) {
        if (login != null && login.matches("\\d{11}")) {
            return login;
        }

        String sql = "SELECT s.cpf " +
                     "FROM autorizacao.usuario u " +
                     "JOIN consignacao.servidor s ON s.matricula::text = u.matricula " +
                     "WHERE u.login = ? AND u.excluido = false " +
                     "ORDER BY u.id DESC LIMIT 1";
        try {
            String cpf = jdbcTemplate.queryForObject(sql, String.class, login);
            if (cpf != null) {
                while (cpf.length() < 11) {
                    cpf = "0" + cpf;
                }
            }
            return cpf;
        } catch (Exception e) {
            return null;
        }
    }

    public List<PagamentoItemDto> findPagamentosByExercicioAndCpf(String exercicio, String cpf) {
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
            "      WHEN ap.cd_inscricao_rp::text <> '0' AND ap.cd_inscricao_rp::text <> '00000' THEN rp.nu_ne_origem::text " +
            "      ELSE ap.nu_ne::text " +
            "    END AS nu_ne, " +
            "    CASE " +
            "      WHEN ap.cd_inscricao_rp::text <> '0' AND ap.cd_inscricao_rp::text <> '00000' THEN rp.fk_exercicio " +
            "      ELSE ap.nu_exercicio " +
            "    END AS exercicione, " +
            "    CASE " +
            "      WHEN ap.nu_class > 0 THEN ap.nu_class::text " +
            "      WHEN NULLIF(rp.nu_classificacao, '')::integer > 0 THEN rp.nu_classificacao::text " +
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
            "        AND rp.nu_inscricao::text = ap.cd_inscricao_rp::text " +
            "  WHERE ap.nu_exercicio = ? " +
            ") tt " +
            "LEFT JOIN siaf.siaf_tb_dotacao_orcamentaria soe " +
            "       ON soe.fk_cnpj = tt.fk_cnpj " +
            "      AND soe.fk_exercicio = tt.exercicione " +
            "      AND soe.nu_classificacao::text = tt.nuclass::text " +
            "LEFT JOIN siaf.par_tb_elemento_despesa elem ON elem.id = SUBSTRING(soe.fk_natureza FROM 5 FOR 2) " +
            "LEFT JOIN siaf.siaf_tb_nota_empenho ne " +
            "       ON ne.fk_cnpj = tt.fk_cnpj " +
            "      AND ne.nu_exercicio = tt.exercicione " +
            "      AND ne.nu_empenho::text = tt.nu_ne::text " +
            "WHERE tt.nu_exercicio = ? " +
            "  AND tt.nu_cnpj_cpf = ? " +
            "  AND tt.nuclass IS NOT NULL " +
            "  AND tt.apprincipal = '' " +
            "  AND SUBSTRING(soe.fk_natureza FROM 5 FOR 2) IN ('14') " +
            "ORDER BY tt.fk_cnpj, tt.nu_documento";

        return jdbcTemplate.query(sql, new RowMapper<PagamentoItemDto>() {
            @Override
            public PagamentoItemDto mapRow(ResultSet rs, int rowNum) throws SQLException {
                PagamentoItemDto dto = new PagamentoItemDto();
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
