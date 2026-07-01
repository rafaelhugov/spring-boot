package mppb.restapi.mobile.feature.contracheque.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import mppb.restapi.mobile.feature.contracheque.dto.CompetenciaDto;
import mppb.restapi.mobile.feature.contracheque.dto.ContrachequeResponseDto;
import mppb.restapi.mobile.feature.contracheque.dto.LancamentoDto;

@Repository
public class ContrachequeRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ContrachequeRepository(JdbcTemplate jdbcTemplate) {
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
            // Ignora se não achar ou der erro de parse
        }
        return null;
    }

    public List<CompetenciaDto> findCompetenciasByMatricula(Integer matricula) {
        String sql = "SELECT DISTINCT mes, ano FROM consignacao.itemcontracheque " +
                     "WHERE matricula_servidor = ? " +
                     "ORDER BY ano DESC, mes DESC";

        return jdbcTemplate.query(sql, new RowMapper<CompetenciaDto>() {
            @Override
            public CompetenciaDto mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new CompetenciaDto(rs.getInt("mes"), rs.getInt("ano"));
            }
        }, matricula);
    }

    public ContrachequeResponseDto findContracheque(Integer matricula, Integer mes, Integer ano) {
        String sqlServidor = "SELECT s.nome, s.matricula, car.nome as cargo " +
                             "FROM consignacao.servidor s " +
                             "LEFT JOIN consignacao.cargo car ON car.id = s.id_cargo " +
                             "WHERE s.matricula = ? LIMIT 1";

        ContrachequeResponseDto response;
        try {
            response = jdbcTemplate.queryForObject(sqlServidor, new RowMapper<ContrachequeResponseDto>() {
                @Override
                public ContrachequeResponseDto mapRow(ResultSet rs, int rowNum) throws SQLException {
                    ContrachequeResponseDto dto = new ContrachequeResponseDto();
                    dto.setNome(rs.getString("nome"));
                    dto.setMatricula(String.valueOf(rs.getInt("matricula")));
                    dto.setCargo(rs.getString("cargo"));
                    dto.setMesAno(String.format("%02d/%d", mes, ano));
                    return dto;
                }
            }, matricula);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }

        if (response != null) {
            String sqlLancamentos = "SELECT o.codigo, o.nome as descricao, i.valor, o.tipo " +
                                    "FROM consignacao.itemcontracheque i " +
                                    "JOIN consignacao.operacao o ON o.id = i.id_operacao " +
                                    "WHERE i.matricula_servidor = ? AND i.mes = ? AND i.ano = ? " +
                                    "ORDER BY o.tipo DESC, o.codigo ASC";

            List<LancamentoDto> lancamentos = jdbcTemplate.query(sqlLancamentos, new RowMapper<LancamentoDto>() {
                @Override
                public LancamentoDto mapRow(ResultSet rs, int rowNum) throws SQLException {
                    LancamentoDto dto = new LancamentoDto();
                    dto.setCodigo(String.valueOf(rs.getInt("codigo")));
                    dto.setDescricao(rs.getString("descricao"));
                    dto.setValor(rs.getBigDecimal("valor"));
                    
                    String tipo = rs.getString("tipo");
                    if ("C".equals(tipo)) {
                        dto.setTipo("V"); // Front-end espera 'V' para vantagem
                    } else if ("D".equals(tipo)) {
                        dto.setTipo("D"); // Desconto
                    } else {
                        dto.setTipo(tipo);
                    }
                    
                    return dto;
                }
            }, matricula, mes, ano);

            response.setLancamentos(lancamentos);
        }

        return response;
    }

    public String getHashContracheque(Integer matricula, Integer mes, Integer ano) {
        String sql = "SELECT hash FROM consignacao.contracheque WHERE matricula_servidor = ? AND mes = ? AND ano = ? LIMIT 1";
        try {
            return jdbcTemplate.queryForObject(sql, String.class, matricula, mes, ano);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public void insertHashContracheque(String hash, Integer matricula, Integer mes, Integer ano) {
        String sql = "INSERT INTO consignacao.contracheque(hash, matricula_servidor, mes, ano, emitido, dataregistro, ultima_emissao) " +
                     "VALUES (?, ?, ?, ?, 1, current_timestamp, current_timestamp)";
        try {
            jdbcTemplate.update(sql, hash, matricula, mes, ano);
        } catch (Exception e) {
            // Ignora se conflito
        }
    }

    public void incrementaEmissaoHash(String hash) {
        String sql = "UPDATE consignacao.contracheque SET emitido = emitido + 1, ultima_emissao = current_timestamp WHERE upper(hash) = upper(?)";
        try {
            jdbcTemplate.update(sql, hash.trim());
        } catch (Exception e) {
            // Ignora erro
        }
    }
}
