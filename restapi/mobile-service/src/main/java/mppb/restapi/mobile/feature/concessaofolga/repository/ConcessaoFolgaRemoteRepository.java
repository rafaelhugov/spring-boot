package mppb.restapi.mobile.feature.concessaofolga.repository;

import java.security.MessageDigest;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import mppb.restapi.mobile.feature.concessaofolga.dto.AusenciaCalendarioDto;
import mppb.restapi.mobile.feature.concessaofolga.dto.ConcessaoFolgaDto;
import mppb.restapi.mobile.feature.concessaofolga.dto.SolicitacaoConcessaoDto;

@Repository
public class ConcessaoFolgaRemoteRepository {

    private final NamedParameterJdbcTemplate atendimentoJdbcTemplate;

    public ConcessaoFolgaRemoteRepository(NamedParameterJdbcTemplate atendimentoJdbcTemplate) {
        this.atendimentoJdbcTemplate = atendimentoJdbcTemplate;
    }

    public List<ConcessaoFolgaDto> findConcessoesVigentes(String matricula) {
        String sql = ""
                + "SELECT "
                + "  id_servidor, nome, matricula, tipo_concessao, id_concessao, "
                + "  concessao_tipo_periodo, dias_concedidos, saldo_restante, "
                + "  periodo_concessao, ano_referencia_concessao, "
                + "  inicio_periodo_aquisitivo, final_periodo_aquisitivo, "
                + "  tipo_solicitacao, inicio_solicitado, final_solicitado, "
                + "  status_solicitacao, dias_solicitados, id_solicitacao "
                + "FROM recursoshumanos.concessoes_vigentes_vw "
                + "WHERE matricula::text = :matricula "
                + "ORDER BY ano_referencia_concessao DESC NULLS LAST, "
                + "         periodo_concessao NULLS LAST, id_concessao, inicio_solicitado NULLS LAST";

        try {
            List<Map<String, Object>> rows = atendimentoJdbcTemplate.queryForList(
                    sql,
                    new MapSqlParameterSource("matricula", matricula));
            return agruparConcessoes(rows);
        } catch (DataAccessException e) {
            throw new AtendimentoUnavailableException("Falha ao consultar concessoes de folga");
        }
    }

    public List<AusenciaCalendarioDto> findAusencias(String matricula, Integer mes, Integer ano) {
        LocalDate primeiroDia;
        LocalDate ultimoDia;
        
        if (mes != null && ano != null) {
            primeiroDia = LocalDate.of(ano, mes, 1);
            ultimoDia = primeiroDia.withDayOfMonth(primeiroDia.lengthOfMonth());
        } else {
            primeiroDia = LocalDate.now().withDayOfMonth(1);
            ultimoDia = LocalDate.of(2099, 12, 31);
        }
        String sql = ""
                + "SELECT DISTINCT * "
                + "FROM relatorios.ausenciasdemembroseservidores_vw vw "
                + "WHERE vw.servidor_matricula::text = :matricula "
                + "  AND vw.datainicioafastamento_date <= :ultimoDiaMes "
                + "  AND vw.datafimafastamento_date >= :primeiroDiaMes";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("matricula", matricula)
                .addValue("primeiroDiaMes", Date.valueOf(primeiroDia))
                .addValue("ultimoDiaMes", Date.valueOf(ultimoDia));

        try {
            List<AusenciaCalendarioDto> ausencias = atendimentoJdbcTemplate.queryForList(sql, params)
                    .stream()
                    .map(this::mapAusencia)
                    .filter(dto -> dto.getDataInicio() != null && dto.getDataFim() != null)
                    .toList();
            return deduplicarAusencias(ausencias);
        } catch (DataAccessException e) {
            throw new AtendimentoUnavailableException("Falha ao consultar ausencias");
        }
    }

    public static List<ConcessaoFolgaDto> agruparConcessoes(List<Map<String, Object>> rows) {
        Map<Long, ConcessaoFolgaDto> agrupadas = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Long idConcessao = longValue(row.get("id_concessao"));
            if (idConcessao == null) {
                continue;
            }

            ConcessaoFolgaDto concessao = agrupadas.computeIfAbsent(idConcessao, id -> {
                ConcessaoFolgaDto dto = new ConcessaoFolgaDto();
                dto.setId(id);
                dto.setTipo(stringValue(row.get("tipo_concessao")));
                dto.setDescricaoOriginal(stringValue(row.get("concessao_tipo_periodo")));
                dto.setDiasConcedidos(intValue(row.get("dias_concedidos")));
                dto.setSaldoRestante(intValue(row.get("saldo_restante")));
                dto.setPeriodo(intValue(row.get("periodo_concessao")));
                dto.setAnoReferencia(intValue(row.get("ano_referencia_concessao")));
                dto.setInicioPeriodoAquisitivo(localDateValue(row.get("inicio_periodo_aquisitivo")));
                dto.setFinalPeriodoAquisitivo(localDateValue(row.get("final_periodo_aquisitivo")));
                return dto;
            });

            Long idSolicitacao = longValue(row.get("id_solicitacao"));
            LocalDate inicio = localDateValue(row.get("inicio_solicitado"));
            LocalDate fim = localDateValue(row.get("final_solicitado"));
            if (idSolicitacao == null || inicio == null || fim == null) {
                continue;
            }

            SolicitacaoConcessaoDto solicitacao = new SolicitacaoConcessaoDto();
            solicitacao.setId(idSolicitacao);
            solicitacao.setTipo(stringValue(row.get("tipo_solicitacao")));
            solicitacao.setTipoOriginal(stringValue(row.get("tipo_solicitacao")));
            solicitacao.setInicio(inicio);
            solicitacao.setFim(fim);
            solicitacao.setStatus(stringValue(row.get("status_solicitacao")));
            solicitacao.setStatusOriginal(stringValue(row.get("status_solicitacao")));
            solicitacao.setDiasInformados(intValue(row.get("dias_solicitados")));
            concessao.getSolicitacoes().add(solicitacao);
        }
        return new ArrayList<>(agrupadas.values());
    }

    public static List<AusenciaCalendarioDto> deduplicarAusencias(List<AusenciaCalendarioDto> ausencias) {
        Map<String, AusenciaCalendarioDto> unicas = new LinkedHashMap<>();
        for (AusenciaCalendarioDto ausencia : ausencias) {
            unicas.putIfAbsent(displayKey(ausencia), ausencia);
        }
        return new ArrayList<>(unicas.values());
    }

    private AusenciaCalendarioDto mapAusencia(Map<String, Object> row) {
        AusenciaCalendarioDto dto = new AusenciaCalendarioDto();
        LocalDate inicio = localDateValue(value(row, "datainicioafastamento_date"));
        LocalDate fim = localDateValue(value(row, "datafimafastamento_date"));
        String tipo = firstString(row,
                "tipoafastamento",
                "tipo_afastamento",
                "tipo",
                "descricaoafastamento",
                "descricao_afastamento");
        String descricao = firstString(row,
                "descricaoafastamento",
                "descricao_afastamento",
                "descricao",
                "tipoafastamento",
                "tipo_afastamento");

        dto.setDataInicio(inicio);
        dto.setDataFim(fim);
        dto.setTipo(tipo == null ? "Outros" : tipo);
        dto.setDescricao(descricao == null ? dto.getTipo() : descricao);
        dto.setStatus(firstString(row, "status", "situacao", "statusafastamento"));
        dto.setObservacao(firstString(row, "observacao", "observacaoafastamento", "observacoes"));
        dto.setId(firstString(row, "id", "id_afastamento", "idafastamento"));
        if (dto.getId() == null) {
            dto.setId(stableId(dto));
        }
        return dto;
    }

    private Object value(Map<String, Object> row, String key) {
        if (row.containsKey(key)) {
            return row.get(key);
        }
        return row.get(key.toUpperCase());
    }

    private String firstString(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            String value = stringValue(value(row, key));
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private static Integer intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return Integer.parseInt(value.toString());
    }

    private static Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return Long.parseLong(value.toString());
    }

    private static LocalDate localDateValue(Object value) {
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        try {
            String str = value.toString().trim();
            if (str.length() >= 10) {
                return LocalDate.parse(str.substring(0, 10));
            }
        } catch (Exception e) {
            // ignore exception and return null
        }
        return null;
    }

    private static String stableId(AusenciaCalendarioDto dto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(("%s|%s|%s|%s".formatted(
                    dto.getTipo(),
                    dto.getDescricao(),
                    dto.getDataInicio(),
                    dto.getDataFim())).getBytes());
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 8 && i < bytes.length; i++) {
                hex.append(String.format("%02x", bytes[i]));
            }
            return hex.toString();
        } catch (Exception e) {
            return String.valueOf(System.nanoTime());
        }
    }

    private static String displayKey(AusenciaCalendarioDto dto) {
        return "%s|%s|%s|%s|%s|%s".formatted(
                dto.getTipo(),
                dto.getDescricao(),
                dto.getDataInicio(),
                dto.getDataFim(),
                dto.getStatus(),
                dto.getObservacao());
    }
}
