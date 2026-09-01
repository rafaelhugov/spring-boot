package mppb.restapi.mobile.feature.processodetalhe.repository;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.util.ReflectionTestUtils;

class ProcessoDetalhesRepositoryTest {

    private ProcessoDetalhesRepository repository;
    private CapturingJdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        repository = new ProcessoDetalhesRepository();
        jdbcTemplate = new CapturingJdbcTemplate();
        ReflectionTestUtils.setField(repository, "jdbcTemplate", jdbcTemplate);
    }

    @Test
    void detalhesPublicosBloqueiamAcessoDiretoAProcessoSigiloso() {
        repository.findDetalhesProcesso(123L);

        String sql = jdbcTemplate.sql;
        assertTrue(sql.contains("p.tipoprotocolo = 'P'"));
        assertTrue(sql.contains("p.id_pasta IS NULL"));
        assertTrue(sql.contains("p.dataexclusao IS NULL"));
        assertTrue(sql.contains("p.id_ultimomovimentosigilosidade IS NULL"));
        assertTrue(sql.contains("mov_sigilo.cod_movimento = 920360"));
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
