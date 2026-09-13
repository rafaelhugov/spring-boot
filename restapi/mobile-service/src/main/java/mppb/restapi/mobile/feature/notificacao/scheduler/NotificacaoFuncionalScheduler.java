package mppb.restapi.mobile.feature.notificacao.scheduler;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.mobile.feature.notificacao.model.ResultadoProcessamentoNotificacaoFuncional;
import mppb.restapi.mobile.feature.notificacao.repository.DispositivoFcmRepository;
import mppb.restapi.mobile.feature.notificacao.service.AfastamentoNotificacaoDeteccaoService;
import mppb.restapi.mobile.feature.notificacao.service.ContrachequeNotificacaoDeteccaoService;
import mppb.restapi.mobile.feature.notificacao.service.PagamentoNotificacaoDeteccaoService;
import mppb.restapi.mobile.feature.notificacao.service.NotificacaoFuncionalProcessamentoService;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificacaoFuncionalScheduler {

    private final DispositivoFcmRepository dispositivoRepository;
    private final ContrachequeNotificacaoDeteccaoService contrachequeDeteccaoService;
    private final PagamentoNotificacaoDeteccaoService pagamentoDeteccaoService;
    private final AfastamentoNotificacaoDeteccaoService afastamentoDeteccaoService;
    private final NotificacaoFuncionalProcessamentoService processamentoService;

    @Value("${notificacoes-funcionais.enabled:false}")
    private boolean enabled;

    @Value("${designacoes.notificacao.fcm.enabled:false}")
    private boolean fcmEnabled;

    @Value("${notificacoes-funcionais.max-matriculas-por-ciclo:300}")
    private int maxMatriculasPorCiclo;

    @Scheduled(cron = "${notificacoes-funcionais.cron:0 */15 * * * *}")
    public void executar() {
        if (!enabled) {
            return;
        }

        try {
            int novas = detectarNovasNotificacoes();
            if (!fcmEnabled) {
                log.info("Notificacoes funcionais detectadas sem envio FCM: novas={}", novas);
                return;
            }

            List<ResultadoProcessamentoNotificacaoFuncional> resultados =
                    processamentoService.processarPendentes();
            long enviadas = resultados.stream()
                    .filter(resultado -> "EN".equals(resultado.statusFinal()))
                    .count();
            long erros = resultados.stream()
                    .filter(resultado -> "ER".equals(resultado.statusFinal()))
                    .count();
            log.info("Notificacoes funcionais processadas: novas={}, pendentes={}, enviadas={}, erros={}",
                    novas, resultados.size(), enviadas, erros);
        } catch (RuntimeException e) {
            log.error("Falha sanitizada no ciclo de notificacoes funcionais: {}", e.getClass().getSimpleName());
        }
    }

    private int detectarNovasNotificacoes() {
        List<String> matriculas = dispositivoRepository.findMatriculasAtivas().stream()
                .limit(maxMatriculasPorCiclo)
                .toList();
        int novas = 0;
        for (String matricula : matriculas) {
            novas += contrachequeDeteccaoService.detectarNovosContracheques(matricula);
            novas += pagamentoDeteccaoService.detectarNovosPagamentos(matricula);
            novas += afastamentoDeteccaoService.detectarNovosAfastamentos(matricula);
        }
        return novas;
    }
}
