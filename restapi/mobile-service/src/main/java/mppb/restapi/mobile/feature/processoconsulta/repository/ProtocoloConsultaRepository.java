package mppb.restapi.mobile.feature.processoconsulta.repository;

import mppb.restapi.mobile.feature.processoconsulta.dto.ConsultaProcessoFilterDTO;
import mppb.restapi.mobile.feature.processoconsulta.dto.ConsultaProcessoResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ProtocoloConsultaRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<ConsultaProcessoResponseDTO> consultarProcessos(ConsultaProcessoFilterDTO filtro) {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        sql.append("WITH p_ids AS ( ");
        sql.append("    SELECT p.id, p.dataregistro ");
        sql.append("    FROM protocolo.protocolo p ");
        sql.append("    JOIN protocolo.pasta pasta ON pasta.id_protocolo = p.id ");
        sql.append("    LEFT JOIN protocolo.numeracao num ON num.id = p.id_numeracao ");
        sql.append("    LEFT JOIN taxonomia.movimentos m ON p.id_ultimomovimentoarquivamento = m.id_item ");
        
        boolean temCpf = filtro.getCpf() != null && !filtro.getCpf().isEmpty();
        boolean temCnpj = filtro.getCnpj() != null && !filtro.getCnpj().isEmpty();
        boolean temNome = filtro.getInteressado() != null && !filtro.getInteressado().trim().isEmpty();

        if (temCpf || temCnpj || temNome) {
            sql.append("    JOIN protocolo.pastadocumento pecainicial ON pecainicial.id_protocolomovimento = pasta.id_pastadocumentopecainicial ");
            sql.append("    JOIN protocolo.protocolo pdocini ON pdocini.id = pecainicial.id_documento AND pdocini.assinado = true ");
            sql.append("    JOIN protocolo.interessado pi ON pi.id_protocolo = pdocini.id AND pi.sigiloso = false AND pi.dataexclusao IS NULL ");
            
            if (temNome) {
                sql.append("    LEFT JOIN pessoa pes ON pi.id_pessoa = pes.id ");
            }
            if (temCpf) {
                sql.append("    LEFT JOIN pessoafisica pf ON pi.id_pessoa = pf.id_pessoa ");
            }
            if (temCnpj) {
                sql.append("    LEFT JOIN pessoajuridica pj ON pi.id_pessoa = pj.id_pessoa ");
            }
        }

        sql.append("    WHERE p.tipoprotocolo = 'P' ");
        sql.append("      AND p.id_pasta IS NULL ");
        sql.append("      AND p.dataexclusao IS NULL ");

        if (temNome || temCpf || temCnpj) {
            sql.append("      AND p.id_ultimomovimentosigilosidade IS NULL ");
        }

        if (filtro.getNumeroProcesso() != null && !filtro.getNumeroProcesso().isEmpty()) {
            sql.append("      AND num.numero LIKE ? ");
            params.add("%" + filtro.getNumeroProcesso() + "%");
        }
        if (filtro.getNumeroOrigem() != null && !filtro.getNumeroOrigem().isEmpty()) {
            sql.append("      AND p.numeroorigem LIKE ? ");
            params.add("%" + filtro.getNumeroOrigem() + "%");
        }
        if (filtro.getNumeroCnmp() != null && !filtro.getNumeroCnmp().isEmpty()) {
            sql.append("      AND num.numeroCNMP LIKE ? ");
            params.add("%" + filtro.getNumeroCnmp() + "%");
        }
        if (filtro.getIdClasse() != null && filtro.getIdClasse() > 0) {
            sql.append("      AND p.id_classe = ? ");
            params.add(filtro.getIdClasse());
        }
        if (filtro.getIdComarca() != null && filtro.getIdComarca() > 0) {
            sql.append("      AND p.id_orgaocriador = ? ");
            params.add(filtro.getIdComarca());
        }
        if (filtro.getStatus() != null && filtro.getStatus().equals("A")) {
            sql.append("      AND (p.id_ultimomovimentoarquivamento IS NULL OR m.arquivamentomppb IS NULL OR m.arquivamentomppb = false) ");
        } else if (filtro.getStatus() != null && filtro.getStatus().equals("I")) {
            sql.append("      AND m.arquivamentomppb = true ");
        }
        if (temNome) {
            sql.append("      AND (CASE WHEN pi.id_pessoa IS NULL THEN UPPER(pi.nome) ELSE UPPER(pes.nome) END) LIKE UPPER(?) ");
            params.add("%" + filtro.getInteressado().trim() + "%");
        }
        if (temCpf) {
            sql.append("      AND (CASE WHEN pi.id_pessoa IS NULL THEN pi.cpfcnpj ELSE pf.cpf END) = ? ");
            params.add(filtro.getCpf().replaceAll("[^0-9]", ""));
        }
        if (temCnpj) {
            sql.append("      AND (CASE WHEN pi.id_pessoa IS NULL THEN pi.cpfcnpj ELSE pj.cnpj END) = ? ");
            params.add(filtro.getCnpj().replaceAll("[^0-9]", ""));
        }

        sql.append("    GROUP BY p.id, p.dataregistro ");
        sql.append("    ORDER BY p.dataregistro DESC ");
        sql.append("    LIMIT 50 ");
        sql.append(") ");

        sql.append("SELECT ");
        sql.append("    p.id AS id, ");
        sql.append("    num.numero AS numeroProcesso, ");
        sql.append("    p.numeroorigem AS numeroOrigem, ");
        sql.append("    c.nome AS classe, ");
        sql.append("    (SELECT string_agg(coalesce(pi2.nome, pes2.nome), ', ') ");
        sql.append("     FROM protocolo.pastadocumento pd2 ");
        sql.append("     JOIN protocolo.interessado pi2 ON pi2.id_protocolo = pd2.id_documento ");
        sql.append("     LEFT JOIN pessoa pes2 ON pes2.id = pi2.id_pessoa ");
        sql.append("     WHERE pd2.id_protocolomovimento = pasta.id_pastadocumentopecainicial ");
        sql.append("       AND pi2.sigiloso = false AND pi2.dataexclusao IS NULL) AS interessado, ");
        sql.append("    o.nome AS promotoria, ");
        sql.append("    CASE WHEN p.id_ultimomovimentoarquivamento IS NOT NULL ");
        sql.append("         AND m.arquivamentomppb = true THEN 'Arquivado' ELSE 'Ativo' END AS status, ");
        sql.append("    p.dataregistro AS dataRegistro ");
        sql.append("FROM p_ids ");
        sql.append("JOIN protocolo.protocolo p ON p_ids.id = p.id ");
        sql.append("JOIN protocolo.pasta pasta ON pasta.id_protocolo = p.id ");
        sql.append("LEFT JOIN protocolo.numeracao num ON num.id = p.id_numeracao ");
        sql.append("LEFT JOIN taxonomia.itens c ON c.id = p.id_classe ");
        sql.append("LEFT JOIN orgao orgao_info ON orgao_info.id_pessoa = p.id_orgaocriador ");
        sql.append("LEFT JOIN pessoa o ON o.id = orgao_info.id_pessoa ");
        sql.append("LEFT JOIN taxonomia.movimentos m ON p.id_ultimomovimentoarquivamento = m.id_item ");
        sql.append("ORDER BY p.dataregistro DESC");

        return jdbcTemplate.query(sql.toString(), params.toArray(), new RowMapper<ConsultaProcessoResponseDTO>() {
            @Override
            public ConsultaProcessoResponseDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
                ConsultaProcessoResponseDTO dto = new ConsultaProcessoResponseDTO();
                dto.setId(rs.getLong("id"));
                dto.setNumeroProcesso(rs.getString("numeroProcesso"));
                dto.setNumeroOrigem(rs.getString("numeroOrigem"));
                dto.setClasse(rs.getString("classe"));
                dto.setInteressado(rs.getString("interessado"));
                dto.setPromotoria(rs.getString("promotoria"));
                dto.setStatus(rs.getString("status"));
                dto.setDataRegistro(rs.getTimestamp("dataRegistro"));
                return dto;
            }
        });
    }
}
