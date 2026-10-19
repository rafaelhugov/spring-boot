package mppb.restapi.mobile.feature.processodetalhe.repository;

import mppb.restapi.mobile.core.security.ProcessoPublicoSql;
import mppb.restapi.mobile.feature.processodetalhe.dto.InteressadoDTO;
import mppb.restapi.mobile.feature.processodetalhe.dto.MovimentoDTO;
import mppb.restapi.mobile.feature.processodetalhe.dto.ProcessoDetalheDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProcessoDetalhesRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public ProcessoDetalheDTO findDetalhesProcesso(Long idProcesso) {
        String sql = "SELECT p.id, num.numero AS numeroProcesso, p.numeroorigem, " +
                     "c.nome AS classe, a.nome AS assuntoPrincipal, " +
                     "org.nome AS promotoria, p.dataregistro, p.resumo AS textoInformado " +
                     "FROM protocolo.protocolo p " +
                     "LEFT JOIN protocolo.numeracao num ON p.id_numeracao = num.id " +
                     "LEFT JOIN taxonomia.itens c ON p.id_classe = c.id " +
                     "LEFT JOIN taxonomia.itens a ON p.id_assunto = a.id " +
                     "LEFT JOIN orgao orgao_info ON p.id_orgaocriador = orgao_info.id_pessoa " +
                     "LEFT JOIN pessoa org ON orgao_info.id_pessoa = org.id " +
                     "WHERE p.id = ? " +
                     "  AND " + ProcessoPublicoSql.processoPublico("p", "detalhe");

        List<ProcessoDetalheDTO> processos = jdbcTemplate.query(sql, new Object[]{idProcesso}, (rs, rowNum) -> {
            ProcessoDetalheDTO dto = new ProcessoDetalheDTO();
            dto.setId(rs.getLong("id"));
            dto.setNumeroProcesso(rs.getString("numeroProcesso"));
            dto.setNumeroOrigem(rs.getString("numeroorigem"));
            dto.setClasse(rs.getString("classe"));
            dto.setAssuntoPrincipal(rs.getString("assuntoPrincipal"));
            dto.setPromotoria(rs.getString("promotoria"));
            dto.setDataRegistro(rs.getTimestamp("dataregistro"));
            dto.setTextoInformado(rs.getString("textoInformado"));
            return dto;
        });

        return processos.isEmpty() ? null : processos.get(0);
    }

    public List<InteressadoDTO> findInteressadosPorProcesso(Long idProcesso) {
        String sql = "SELECT NULL AS tipoParte, " +
                     "       COALESCE(i.nome, pes.nome) AS nome, " +
                     "       NULL AS nomesocial, " +
                     "       LENGTH(REGEXP_REPLACE(COALESCE(i.cpfcnpj, pf.cpf, pj.cnpj, ''), '[^0-9]', '', 'g')) AS documento_tamanho, " +
                     "       RIGHT(REGEXP_REPLACE(COALESCE(i.cpfcnpj, pf.cpf, pj.cnpj, ''), '[^0-9]', '', 'g'), 2) AS documento_final " +
                     "FROM protocolo.pasta pasta " +
                     "JOIN protocolo.protocolo p ON p.id = pasta.id_protocolo " +
                     "JOIN protocolo.pastadocumento pd ON pd.id_protocolomovimento = pasta.id_pastadocumentopecainicial " +
                     "JOIN protocolo.interessado i ON i.id_protocolo = pd.id_documento " +
                     "LEFT JOIN pessoa pes ON pes.id = i.id_pessoa " +
                     "LEFT JOIN pessoafisica pf ON pf.id_pessoa = i.id_pessoa " +
                     "LEFT JOIN pessoajuridica pj ON pj.id_pessoa = i.id_pessoa " +
                     "WHERE pasta.id_protocolo = ? " +
                     "  AND " + ProcessoPublicoSql.processoPublico("p", "interessado") + " " +
                     "  AND i.sigiloso = false " +
                     "  AND i.dataexclusao IS NULL " +
                     "ORDER BY COALESCE(i.nome, pes.nome) ASC";

        return jdbcTemplate.query(sql, new Object[]{idProcesso}, (rs, rowNum) -> {
            InteressadoDTO dto = new InteressadoDTO();
            dto.setTipoParte(rs.getString("tipoParte"));
            dto.setNome(rs.getString("nome"));
            dto.setNomeSocial(rs.getString("nomesocial"));
            int tamanhoDocumento = rs.getInt("documento_tamanho");
            String finalDocumento = rs.getString("documento_final");
            dto.setDocumento(documentoMascarado(tamanhoDocumento, finalDocumento));
            if (tamanhoDocumento == 14) {
                dto.setTipoPessoa("Jurídica");
            } else if (tamanhoDocumento == 11) {
                dto.setTipoPessoa("Física");
            } else {
                dto.setTipoPessoa("N/D");
            }
            return dto;
        });
    }

    private String documentoMascarado(int tamanho, String ultimosDigitos) {
        if (ultimosDigitos == null || ultimosDigitos.length() != 2) {
            return null;
        }
        if (tamanho == 11) {
            return "***.***.***-" + ultimosDigitos;
        }
        if (tamanho == 14) {
            return "**.***.***/****-" + ultimosDigitos;
        }
        return null;
    }

    public List<MovimentoDTO> findMovimentosPorProcesso(Long idProcesso) {
        String sql = "SELECT pm.dataregistro AS data, i.nome AS tipo, " +
                     "pdoc.resumo AS complemento " +
                     "FROM protocolo.protocolomovimento pm " +
                     "JOIN protocolo.protocolo p ON p.id = pm.id_protocolo " +
                     "LEFT JOIN taxonomia.itens i ON i.id = pm.id_movimento " +
                     "LEFT JOIN protocolo.pastadocumento pd ON pd.id_protocolomovimento = pm.id " +
                     "LEFT JOIN protocolo.protocolo pdoc ON pdoc.id = pd.id_documento " +
                     "WHERE pm.id_protocolo = ? " +
                     "  AND " + ProcessoPublicoSql.processoPublico("p", "movimento") + " " +
                     "  AND pm.dataexclusao IS NULL " +
                     "  AND pm.id_protocolomovimento IS NULL " +
                     "  AND (pdoc.id IS NULL OR " + ProcessoPublicoSql.semSigiloAtivo("pdoc", "documento") + ") " +
                     "ORDER BY pm.dataregistro DESC";

        return jdbcTemplate.query(sql, new Object[]{idProcesso}, (rs, rowNum) -> {
            MovimentoDTO dto = new MovimentoDTO();
            dto.setData(rs.getTimestamp("data"));
            dto.setTipo(rs.getString("tipo"));
            dto.setComplemento(rs.getString("complemento")); // Simplificação do complemento
            return dto;
        });
    }
}
