package mppb.restapi.mobile.feature.designacao.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class DesignacaoIdentityRepository {

    private final JdbcTemplate jdbcTemplate;

    public String getMatriculaByLogin(String login) {
        String sql = "SELECT matricula FROM autorizacao.usuario WHERE login = ? AND excluido = false ORDER BY id DESC LIMIT 1";
        try {
            String matricula = jdbcTemplate.queryForObject(sql, String.class, login);
            return matricula == null ? null : matricula.trim();
        } catch (Exception e) {
            return null;
        }
    }
}
