package mppb.restapi.mobile.feature.concessaofolga.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import mppb.restapi.mobile.feature.concessaofolga.dto.AusenciaCalendarioDto;
import mppb.restapi.mobile.feature.concessaofolga.dto.ConcessaoFolgaDto;

class ConcessaoFolgaRemoteRepositoryTest {

    @Test
    void agrupaConcessaoComMultiplasSolicitacoes() {
        List<Map<String, Object>> rows = List.of(
                Map.ofEntries(
                        Map.entry("id_concessao", 10L),
                        Map.entry("tipo_concessao", "F"),
                        Map.entry("dias_concedidos", 30),
                        Map.entry("saldo_restante", 0),
                        Map.entry("periodo_concessao", 1),
                        Map.entry("ano_referencia_concessao", 2026),
                        Map.entry("inicio_periodo_aquisitivo", Date.valueOf("2025-01-01")),
                        Map.entry("final_periodo_aquisitivo", Date.valueOf("2025-12-31")),
                        Map.entry("id_solicitacao", 100L),
                        Map.entry("tipo_solicitacao", "Gozo"),
                        Map.entry("inicio_solicitado", Date.valueOf("2026-07-01")),
                        Map.entry("final_solicitado", Date.valueOf("2026-07-10")),
                        Map.entry("status_solicitacao", "DEFERIDO"),
                        Map.entry("dias_solicitados", 10)),
                Map.ofEntries(
                        Map.entry("id_concessao", 10L),
                        Map.entry("tipo_concessao", "F"),
                        Map.entry("dias_concedidos", 30),
                        Map.entry("saldo_restante", 0),
                        Map.entry("periodo_concessao", 1),
                        Map.entry("ano_referencia_concessao", 2026),
                        Map.entry("id_solicitacao", 101L),
                        Map.entry("tipo_solicitacao", "Adiamento"),
                        Map.entry("inicio_solicitado", Date.valueOf("2026-08-01")),
                        Map.entry("final_solicitado", Date.valueOf("2026-08-05")),
                        Map.entry("status_solicitacao", "PENDENTE"),
                        Map.entry("dias_solicitados", 5)));

        List<ConcessaoFolgaDto> result = ConcessaoFolgaRemoteRepository.agruparConcessoes(rows);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
        assertEquals(2, result.get(0).getSolicitacoes().size());
        assertEquals(Date.valueOf("2025-01-01").toLocalDate(), result.get(0).getInicioPeriodoAquisitivo());
    }

    @Test
    void mantemConcessaoSemSolicitacao() {
        List<Map<String, Object>> rows = List.of(Map.of(
                "id_concessao", 11L,
                "tipo_concessao", "L",
                "dias_concedidos", 30,
                "saldo_restante", 30));

        List<ConcessaoFolgaDto> result = ConcessaoFolgaRemoteRepository.agruparConcessoes(rows);

        assertEquals(1, result.size());
        assertEquals(11L, result.get(0).getId());
        assertTrue(result.get(0).getSolicitacoes().isEmpty());
    }

    @Test
    void mantemMovimentacoesSemPeriodoCompleto() {
        Map<String, Object> interrupcao = new HashMap<>();
        interrupcao.put("id_concessao", 468153L);
        interrupcao.put("tipo_concessao", "FÉRIAS");
        interrupcao.put("dias_concedidos", 30);
        interrupcao.put("saldo_restante", 0);
        interrupcao.put("periodo_concessao", 1);
        interrupcao.put("ano_referencia_concessao", 2025);
        interrupcao.put("id_solicitacao", 506839L);
        interrupcao.put("tipo_solicitacao", "Interrupção");
        interrupcao.put("inicio_solicitado", null);
        interrupcao.put("final_solicitado", Date.valueOf("2026-09-16"));
        interrupcao.put("status_solicitacao", "CONFIRMADO");
        interrupcao.put("dias_solicitados", 12);

        Map<String, Object> suspensaoTotal = new HashMap<>();
        suspensaoTotal.put("id_concessao", 468154L);
        suspensaoTotal.put("tipo_concessao", "FÉRIAS");
        suspensaoTotal.put("dias_concedidos", 30);
        suspensaoTotal.put("saldo_restante", 0);
        suspensaoTotal.put("periodo_concessao", 2);
        suspensaoTotal.put("ano_referencia_concessao", 2025);
        suspensaoTotal.put("id_solicitacao", 506840L);
        suspensaoTotal.put("tipo_solicitacao", "Suspensão total");
        suspensaoTotal.put("inicio_solicitado", null);
        suspensaoTotal.put("final_solicitado", null);
        suspensaoTotal.put("status_solicitacao", "CONFIRMADO");
        suspensaoTotal.put("dias_solicitados", 20);

        List<ConcessaoFolgaDto> result = ConcessaoFolgaRemoteRepository.agruparConcessoes(
                List.of(interrupcao, suspensaoTotal));

        assertEquals(2, result.size());
        assertEquals(506839L, result.get(0).getSolicitacoes().get(0).getId());
        assertEquals("Interrupção", result.get(0).getSolicitacoes().get(0).getTipo());
        assertNull(result.get(0).getSolicitacoes().get(0).getInicio());
        assertEquals(Date.valueOf("2026-09-16").toLocalDate(), result.get(0).getSolicitacoes().get(0).getFim());
        assertEquals(12, result.get(0).getSolicitacoes().get(0).getDiasInformados());
        assertEquals(506840L, result.get(1).getSolicitacoes().get(0).getId());
        assertNull(result.get(1).getSolicitacoes().get(0).getInicio());
        assertNull(result.get(1).getSolicitacoes().get(0).getFim());
        assertEquals(20, result.get(1).getSolicitacoes().get(0).getDiasInformados());
    }

    @Test
    void removeAusenciasDuplicadasPorCamposExibidos() {
        AusenciaCalendarioDto primeira = ausencia("Férias", "Férias", "2026-11-02", "2026-12-01", "DEFERIDO");
        AusenciaCalendarioDto repetida = ausencia("Férias", "Férias", "2026-11-02", "2026-12-01", "DEFERIDO");
        AusenciaCalendarioDto diferente = ausencia("Férias", "Férias", "2026-12-02", "2026-12-10", "DEFERIDO");

        List<AusenciaCalendarioDto> result = ConcessaoFolgaRemoteRepository.deduplicarAusencias(
                List.of(primeira, repetida, diferente));

        assertEquals(2, result.size());
        assertEquals(primeira, result.get(0));
        assertEquals(diferente, result.get(1));
    }

    private AusenciaCalendarioDto ausencia(String tipo, String descricao, String inicio, String fim, String status) {
        AusenciaCalendarioDto dto = new AusenciaCalendarioDto();
        dto.setId("%s-%s".formatted(inicio, fim));
        dto.setTipo(tipo);
        dto.setDescricao(descricao);
        dto.setDataInicio(Date.valueOf(inicio).toLocalDate());
        dto.setDataFim(Date.valueOf(fim).toLocalDate());
        dto.setStatus(status);
        return dto;
    }
}
