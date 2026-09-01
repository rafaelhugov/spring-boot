package mppb.restapi.mobile.feature.processoconsulta.repository;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.util.ReflectionTestUtils;

import mppb.restapi.mobile.feature.processoconsulta.dto.ConsultaProcessoFilterDTO;

class ProtocoloConsultaRepositoryTest {

    private ProtocoloConsultaRepository repository;
    private CapturingJdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        repository = new ProtocoloConsultaRepository();
        jdbcTemplate = new CapturingJdbcTemplate();
        ReflectionTestUtils.setField(repository, "jdbcTemplate", jdbcTemplate);
    }

    @Test
    void consultaPublicaSempreBloqueiaProcessosSigilosos() {
        ConsultaProcessoFilterDTO filtro = new ConsultaProcessoFilterDTO();
        filtro.setNumeroProcesso("001");

        repository.consultarProcessos(filtro);

        assertTrue(jdbcTemplate.sql.contains("p.id_ultimomovimentosigilosidade IS NULL"));
        assertTrue(jdbcTemplate.sql.contains("mov_sigilo.cod_movimento = 920360"));
    }

    private static class CapturingJdbcTemplate extends JdbcTemplate {

        private String sql;

        @Override
        public <T> List<T> query(String sql, Object[] args, RowMapper<T> rowMapper) {
            this.sql = sql;
            return List.of();
        }
    }
}
