package mppb.restapi.mobile.feature.processoconsulta.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

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

        assertProtecaoSigilo(jdbcTemplate.sql, "consulta");
    }

    @Test
    void filtroDeComarcasNaoUsaProcessosSigilososComoFonte() {
        repository.listarComarcas();

        assertProtecaoSigilo(jdbcTemplate.sql, "comarca");
    }

    @Test
    void payloadsDeInjecaoPermanecemParametrosEProtecaoDeSigiloContinuaAtiva() {
        String payload = "' OR 1=1; SELECT pg_sleep(10); --";
        ConsultaProcessoFilterDTO filtro = new ConsultaProcessoFilterDTO();
        filtro.setNumeroProcesso(payload);
        filtro.setNumeroOrigem(payload);
        filtro.setNumeroCnmp(payload);
        filtro.setInteressado(payload);
        filtro.setNomePromotor(payload);

        repository.consultarProcessos(filtro);

        assertFalse(jdbcTemplate.sql.contains(payload));
        String escapedPayload = "%" + payload.replace("_", "\\_") + "%";
        assertTrue(Arrays.stream(jdbcTemplate.args).anyMatch(escapedPayload::equals));
        assertTrue(jdbcTemplate.sql.contains("ESCAPE '\\'"));
        assertProtecaoSigilo(jdbcTemplate.sql, "consulta");
    }

    @Test
    void consultaTemTimeoutTransacionalContraEsgotamento() throws Exception {
        Transactional transactional = ProtocoloConsultaRepository.class
                .getMethod("consultarProcessos", ConsultaProcessoFilterDTO.class)
                .getAnnotation(Transactional.class);

        assertTrue(transactional.readOnly());
        assertTrue(transactional.timeout() <= 15);
    }

    private void assertProtecaoSigilo(String sql, String sufixo) {
        assertTrue(sql.contains("p.id_ultimomovimentosigilosidade IS NULL"));
        assertTrue(sql.contains("mov_sigilo_" + sufixo + ".cod_movimento = 920360"));
        assertTrue(sql.contains("p.tipoprotocolo = 'P'"));
        assertTrue(sql.contains("p.id_pasta IS NULL"));
        assertTrue(sql.contains("p.dataexclusao IS NULL"));
    }

    private static class CapturingJdbcTemplate extends JdbcTemplate {

        private String sql;
        private Object[] args = new Object[0];

        @Override
        public <T> List<T> query(String sql, RowMapper<T> rowMapper) {
            this.sql = sql;
            return List.of();
        }

        @Override
        public <T> List<T> query(String sql, Object[] args, RowMapper<T> rowMapper) {
            this.sql = sql;
            this.args = args;
            return List.of();
        }
    }
}
