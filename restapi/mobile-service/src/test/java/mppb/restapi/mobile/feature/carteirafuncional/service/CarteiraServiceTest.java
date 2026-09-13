package mppb.restapi.mobile.feature.carteirafuncional.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import mppb.restapi.mobile.feature.carteirafuncional.dto.CarteiraFuncionalTO;

class CarteiraServiceTest {

    @Test
    void recuperaCarteiraComplementandoDataPossePorIdServidor() {
        FakeJdbcTemplate jdbcTemplate = new FakeJdbcTemplate();
        CarteiraService service = new CarteiraService(jdbcTemplate, null);

        CarteiraFuncionalTO carteira = service.recuperaCarteiraFuncionalByLogin("fulano");

        assertEquals(Date.valueOf("2020-03-04"), carteira.getDataPosse());
        assertEquals(1, jdbcTemplate.consultasDataPosse);
    }

    @SuppressWarnings("unchecked")
    private static class FakeJdbcTemplate extends JdbcTemplate {
        private int consultasDataPosse;

        @Override
        public <T> List<T> query(String sql, Object[] args, RowMapper<T> rowMapper) throws DataAccessException {
            if (sql.contains("recursoshumanos.dadosqrcode")) {
                CarteiraFuncionalTO carteira = new CarteiraFuncionalTO();
                carteira.setIdServidor(42L);
                carteira.setMatricula("12345");
                carteira.setNome("Fulano de Tal");
                carteira.setTipoCargo("M");
                return (List<T>) List.of(carteira);
            }
            return List.of();
        }

        @Override
        public <T> List<T> query(String sql, RowMapper<T> rowMapper) throws DataAccessException {
            return List.of();
        }

        @Override
        public <T> List<T> queryForList(String sql, Class<T> elementType, Object... args) throws DataAccessException {
            consultasDataPosse++;
            assertEquals(42L, args[0]);
            return List.of(elementType.cast(Date.valueOf("2020-03-04")));
        }

        @Override
        public void execute(String sql) throws DataAccessException {
        }
    }
}
