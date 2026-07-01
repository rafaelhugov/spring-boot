package mppb.restapi.mobile.feature.ferias.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import mppb.restapi.mobile.feature.ferias.dto.EditalFeriasDto;
import mppb.restapi.mobile.feature.ferias.dto.NovaSolicitacaoFeriasDto;
import mppb.restapi.mobile.feature.ferias.dto.SolicitacaoFLADto;

@Repository
public class FeriasRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public FeriasRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Integer getMatriculaByLogin(String login) {
        String sql = "SELECT matricula FROM autorizacao.usuario WHERE login = ? AND excluido = false ORDER BY id DESC LIMIT 1";
        try {
            String matStr = jdbcTemplate.queryForObject(sql, String.class, login);
            if (matStr != null) {
                return Integer.parseInt(matStr.trim());
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    public Long getIdServidorByMatricula(Integer matricula) {
        String sql = "SELECT id FROM public.servidor WHERE matricula = ? AND excluido = false LIMIT 1";
        try {
            return jdbcTemplate.queryForObject(sql, Long.class, String.valueOf(matricula));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public Long getIdUsuarioByLogin(String login) {
        String sql = "SELECT id FROM autorizacao.usuario WHERE login = ? AND excluido = false ORDER BY id DESC LIMIT 1";
        try {
            return jdbcTemplate.queryForObject(sql, Long.class, login);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<SolicitacaoFLADto> findSolicitacoesByMatricula(Integer matricula) {
        String sql = "SELECT " +
                     "  s.id_anotacaofuncional as id, " +
                     "  a.datainicio, " +
                     "  a.datafim, " +
                     "  a.status, " +
                     "  s.tipo, " +
                     "  s.justificativa " +
                     "FROM recursoshumanos.solicitacaofla s " +
                     "JOIN recursoshumanos.anotacaofuncional a ON a.id = s.id_anotacaofuncional " +
                     "JOIN public.servidor serv ON serv.id = a.id_servidor " +
                     "WHERE serv.matricula = ? AND a.excluido = false AND a.dataexclusao IS NULL " +
                     "ORDER BY a.datainicio DESC";

        return jdbcTemplate.query(sql, new RowMapper<SolicitacaoFLADto>() {
            @Override
            public SolicitacaoFLADto mapRow(ResultSet rs, int rowNum) throws SQLException {
                SolicitacaoFLADto dto = new SolicitacaoFLADto();
                dto.setId(rs.getLong("id"));
                dto.setDataInicio(rs.getDate("datainicio"));
                dto.setDataFim(rs.getDate("datafim"));
                dto.setStatus(rs.getString("status"));
                dto.setTipo(rs.getString("tipo"));
                dto.setJustificativa(rs.getString("justificativa"));
                
                String statusExtenso = mapStatus(rs.getString("status"));
                dto.setStatusExtenso(statusExtenso);
                
                String tipoExtenso = mapTipo(rs.getString("tipo"));
                dto.setTipoExtenso(tipoExtenso);
                
                return dto;
            }
        }, String.valueOf(matricula));
    }

    public List<EditalFeriasDto> findEditaisAbertos() {
        String sql = "SELECT " +
                     "  e.id, " +
                     "  e.edital, " +
                     "  e.datainiciogozo, " +
                     "  e.datafimgozo, " +
                     "  e.tipoedital as tipo " +
                     "FROM recursoshumanos.editalferias e " +
                     "WHERE e.dataexclusao IS NULL AND e.ativo = true " +
                     "ORDER BY e.datainiciogozo DESC";

        return jdbcTemplate.query(sql, new RowMapper<EditalFeriasDto>() {
            @Override
            public EditalFeriasDto mapRow(ResultSet rs, int rowNum) throws SQLException {
                EditalFeriasDto dto = new EditalFeriasDto();
                dto.setId(rs.getLong("id"));
                dto.setEdital(rs.getString("edital"));
                dto.setDataInicioGozo(rs.getDate("datainiciogozo"));
                dto.setDataFimGozo(rs.getDate("datafimgozo"));
                dto.setTipo(rs.getString("tipo"));
                return dto;
            }
        });
    }

    @Transactional
    public void saveSolicitacao(NovaSolicitacaoFeriasDto dto, Long idServidor, Long idUsuario) {
        try {
            Long nextId = jdbcTemplate.queryForObject(
                "SELECT nextval('recursoshumanos.anotacaofuncional_id_seq')", Long.class);

            java.sql.Date dataInicio = dto.getDataInicio() != null
                ? java.sql.Date.valueOf(dto.getDataInicio()) : null;
            java.sql.Date dataFim = dto.getDataFim() != null
                ? java.sql.Date.valueOf(dto.getDataFim()) : null;

            String insertAnotacao =
                "INSERT INTO recursoshumanos.anotacaofuncional " +
                "(id, tipoanotacaofuncional, datainicio, datafim, status, id_servidor, dataregistro, id_usuarioregistro, excluido, numerodocumento, id_naturezaato) " +
                "VALUES (?, 'S', ?, ?, 'P', ?, current_timestamp, ?, false, ?, 2095)";
            jdbcTemplate.update(insertAnotacao,
                nextId, dataInicio, dataFim, idServidor, idUsuario, String.valueOf(nextId));

            String insertSolicitacao =
                "INSERT INTO recursoshumanos.solicitacaofla " +
                "(id_anotacaofuncional, tipo, justificativa, id_editalferias, averbada) " +
                "VALUES (?, ?, ?, ?, false)";
            jdbcTemplate.update(insertSolicitacao,
                nextId,
                dto.getTipo(),
                dto.getJustificativa(),
                dto.getIdEditalFerias());

        } catch (Exception e) {
            throw new RuntimeException("Erro ao salvar solicitacao de ferias [idServidor=" + idServidor
                + ", tipo=" + (dto != null ? dto.getTipo() : "null")
                + ", dataInicio=" + (dto != null ? dto.getDataInicio() : "null") + "]: " + e.getMessage(), e);
        }
    }
    
    private String mapStatus(String status) {
        if (status == null) return "Desconhecido";
        switch (status) {
            case "P": return "Pendente";
            case "L": return "Confirmada";
            case "C": return "Cancelada";
            case "H": return "Homologada";
            case "N": return "Negada";
            case "D": return "Deferida";
            case "I": return "Indeferida";
            default: return status;
        }
    }
    
    private String mapTipo(String tipo) {
        if (tipo == null) return "Desconhecido";
        switch (tipo) {
            case "GO": return "Gozo";
            case "GC": return "Gozo Coletivo";
            case "AN": return "Antecipação";
            case "IN": return "Interrupção";
            case "AD": return "Adiamento";
            case "SP": return "Suspensão Parcial";
            case "ST": return "Suspensão Total";
            case "AS": return "Adiamento Sine Die";
            case "IF": return "Indenização";
            case "CF": return "Conversão";
            default: return tipo;
        }
    }
}
