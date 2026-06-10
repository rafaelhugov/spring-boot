package mppb.restapi.mobile.feature.processodetalhe.repository;

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
                     "LEFT JOIN public.orgao orgao_info ON p.id_orgaocriador = orgao_info.id_pessoa " +
                     "LEFT JOIN public.pessoa org ON orgao_info.id_pessoa = org.id " +
                     "WHERE p.id = ?";

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
        String sql = "SELECT ti.nome AS tipoParte, i.nome, i.nomesocial, i.cpfcnpj AS documento " +
                     "FROM protocolo.interessado i " +
                     "LEFT JOIN protocolo.tipointeressado ti ON i.id_tipointeressado = ti.id " +
                     "WHERE i.id_protocolo = ? " +
                     "ORDER BY i.nome ASC";

        return jdbcTemplate.query(sql, new Object[]{idProcesso}, (rs, rowNum) -> {
            InteressadoDTO dto = new InteressadoDTO();
            dto.setTipoParte(rs.getString("tipoParte"));
            dto.setNome(rs.getString("nome"));
            dto.setNomeSocial(rs.getString("nomesocial"));
            dto.setDocumento(rs.getString("documento"));
            // Definir Pessoa Física ou Jurídica com base no tamanho do documento (se aplicável, mas vou simplificar)
            String doc = rs.getString("documento");
            if (doc != null && doc.length() > 14) {
                dto.setTipoPessoa("Jurídica");
            } else if (doc != null && doc.length() > 0) {
                dto.setTipoPessoa("Física");
            } else {
                dto.setTipoPessoa("N/D");
            }
            return dto;
        });
    }

    public List<MovimentoDTO> findMovimentosPorProcesso(Long idProcesso) {
        String sql = "SELECT pm.dataregistro AS data, m.nome AS tipo, " +
                     "(SELECT pdoc.resumo FROM protocolo.protocolo pdoc WHERE pdoc.id = pm.id_documentogerado) AS complemento " +
                     "FROM protocolo.protocolomovimento pm " +
                     "LEFT JOIN taxonomia.movimentos m ON pm.id_movimento = m.id_item " +
                     "WHERE pm.id_protocolo = ? " +
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
