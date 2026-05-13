package mppb.restapi.eleicao.feature.foto.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.error.BusinessRuleException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class FotoService {

    private final JdbcTemplate jdbcTemplate;

    public byte[] getFotoPorArquivoId(Long arquivoId) {
        log.debug("Executando query para arquivoId: {}", arquivoId);
        if (arquivoId == null) return null;

        String sql = "select bytes from ged.arquivo where id = ?";
        try {
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, arquivoId);
            if (!result.isEmpty() && result.get(0).get("bytes") != null) {
                return (byte[]) result.get(0).get("bytes");
            }
        } catch (Exception e) {
            log.error("Erro ao buscar foto por arquivo id: {}", arquivoId, e);
        }
        return null;
    }

    public byte[] getFotoPorPessoaId(Long pessoaId) {
        log.debug("Executando query para pessoaId: {}", pessoaId);
        if (pessoaId == null) return null;

        String sql = "select bytes from ged.arquivo where id = (select id_fotoprincipal from publico.pessoafisica where id_pessoa = ?) limit 1";
        try {
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, pessoaId);
            if (!result.isEmpty() && result.get(0).get("bytes") != null) {
                return (byte[]) result.get(0).get("bytes");
            }
        } catch (Exception e) {
            log.error("Erro ao buscar foto por pessoa id: {}", pessoaId, e);
        }
        return null;
    }

    public String getMimetype(Long arquivoId) {
        if (arquivoId == null) return "image/png";
        String sql = "select mimetype from ged.arquivo where id = ?";
        try {
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, arquivoId);
            if (!result.isEmpty() && result.get(0).get("mimetype") != null) {
                return (String) result.get(0).get("mimetype");
            }
        } catch (Exception e) {
            log.warn("Não foi possível determinar o mimetype do arquivo {}. Usando padrão image/png", arquivoId);
        }
        return "image/png";
    }
}
