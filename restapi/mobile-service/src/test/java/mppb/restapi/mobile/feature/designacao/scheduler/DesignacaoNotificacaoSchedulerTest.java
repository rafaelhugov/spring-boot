package mppb.restapi.mobile.feature.designacao.scheduler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import mppb.restapi.mobile.feature.designacao.model.ResultadoNotificacaoDesignacao;
import mppb.restapi.mobile.feature.designacao.model.ResultadoProcessamentoNotificacaoDesignacao;
import mppb.restapi.mobile.feature.designacao.service.DesignacaoDeteccaoService;
import mppb.restapi.mobile.feature.designacao.service.DesignacaoProcessamentoNotificacaoService;

class DesignacaoNotificacaoSchedulerTest {

    private FakeDesignacaoDeteccaoService deteccaoService;
    private FakeDesignacaoProcessamentoNotificacaoService processamentoService;
    private DesignacaoNotificacaoScheduler scheduler;

    @BeforeEach
    void setUp() {
        deteccaoService = new FakeDesignacaoDeteccaoService();
        processamentoService = new FakeDesignacaoProcessamentoNotificacaoService();
        scheduler = new DesignacaoNotificacaoScheduler(deteccaoService, processamentoService);
        ReflectionTestUtils.setField(scheduler, "enabled", true);
        ReflectionTestUtils.setField(scheduler, "fcmEnabled", true);
    }

    @Test
    void schedulerDesabilitadoNaoExecutaDeteccaoNemProcessamento() {
        ReflectionTestUtils.setField(scheduler, "enabled", false);

        scheduler.executar();

        assertEquals(0, deteccaoService.chamadas);
        assertEquals(0, processamentoService.chamadas);
    }

    @Test
    void executaDeteccaoAntesDoProcessamento() {
        deteccaoService.eventos = processamentoService.eventos;

        scheduler.executar();

        assertEquals(List.of("detectar", "processar"), processamentoService.eventos);
    }

    @Test
    void fcmDesligadoDetectaMasNaoProcessaPendencias() {
        ReflectionTestUtils.setField(scheduler, "fcmEnabled", false);

        scheduler.executar();

        assertEquals(1, deteccaoService.chamadas);
        assertEquals(0, processamentoService.chamadas);
    }

    @Test
    void falhaNaDeteccaoNaoDerrubaAplicacao() {
        deteccaoService.falharUmaVez = true;

        assertDoesNotThrow(() -> scheduler.executar());

        assertEquals(1, deteccaoService.chamadas);
        assertEquals(1, processamentoService.chamadas);
    }

    @Test
    void falhaNoProcessamentoNaoDerrubaAplicacao() {
        processamentoService.falharUmaVez = true;

        assertDoesNotThrow(() -> scheduler.executar());

        assertEquals(1, deteccaoService.chamadas);
        assertEquals(1, processamentoService.chamadas);
    }

    @Test
    void execucaoSeguinteContinuaPossivelAposFalha() {
        processamentoService.falharUmaVez = true;

        scheduler.executar();
        scheduler.executar();

        assertEquals(2, deteccaoService.chamadas);
        assertEquals(2, processamentoService.chamadas);
    }

    @Test
    void ausenciaDeDuplicidadeFicaDelegadaAosServicesIdempotentes() {
        scheduler.executar();

        assertEquals(1, deteccaoService.chamadas);
        assertEquals(1, processamentoService.chamadas);
    }

    @Test
    void schedulerNaoReintroduzTransacaoLonga() throws Exception {
        assertNull(DesignacaoNotificacaoScheduler.class
                .getMethod("executar")
                .getAnnotation(Transactional.class));
    }

    private static class FakeDesignacaoDeteccaoService extends DesignacaoDeteccaoService {
        private int chamadas;
        private boolean falharUmaVez;
        private List<String> eventos = new ArrayList<>();

        FakeDesignacaoDeteccaoService() {
            super(null, null, null);
        }

        @Override
        public int detectarNovasDesignacoes() {
            chamadas++;
            eventos.add("detectar");
            if (falharUmaVez) {
                falharUmaVez = false;
                throw new IllegalStateException("falha sanitizada de teste");
            }
            return 1;
        }
    }

    private static class FakeDesignacaoProcessamentoNotificacaoService extends DesignacaoProcessamentoNotificacaoService {
        private int chamadas;
        private boolean falharUmaVez;
        private final List<String> eventos = new ArrayList<>();

        FakeDesignacaoProcessamentoNotificacaoService() {
            super(null, null, null);
        }

        @Override
        public List<ResultadoProcessamentoNotificacaoDesignacao> processarPendentes() {
            chamadas++;
            eventos.add("processar");
            if (falharUmaVez) {
                falharUmaVez = false;
                throw new IllegalStateException("falha sanitizada de teste");
            }
            return List.of(new ResultadoProcessamentoNotificacaoDesignacao(
                    1L,
                    "PE",
                    "EN",
                    0,
                    null,
                    new ResultadoNotificacaoDesignacao(1, 1, 0, 0, 0, 0, false)));
        }
    }
}
