package mppb.restapi.mobile.feature.designacao.scheduler;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.mobile.feature.designacao.model.ResultadoProcessamentoNotificacaoDesignacao;
import mppb.restapi.mobile.feature.designacao.service.DesignacaoDeteccaoService;
import mppb.restapi.mobile.feature.designacao.service.DesignacaoProcessamentoNotificacaoService;

@Slf4j
@Component
@RequiredArgsConstructor
public class DesignacaoNotificacaoScheduler {

    private final DesignacaoDeteccaoService deteccaoService;
    private final DesignacaoProcessamentoNotificacaoService processamentoService;

    @Value("${designacoes.notificacao.enabled:false}")
    private boolean enabled;

    @Value("${designacoes.notificacao.fcm.enabled:false}")
    private boolean fcmEnabled;

    @Scheduled(cron = "${designacoes.notificacao.cron:0 */15 * * * *}")
    public void executar() {
        if (!enabled) {
            return;
        }

        log.info("Ciclo de notificacao de designacoes iniciado");
        int novas = 0;
        int processadas = 0;
        try {
            novas = deteccaoService.detectarNovasDesignacoes();
        } catch (RuntimeException e) {
            log.warn("Falha sanitizada na deteccao de designacoes: {}", e.getClass().getSimpleName());
        }

        if (!fcmEnabled) {
            log.info("Ciclo de notificacao de designacoes sem processamento FCM: novas={}", novas);
            return;
        }

        try {
            List<ResultadoProcessamentoNotificacaoDesignacao> resultados = processamentoService.processarPendentes();
            processadas = resultados.size();
            long enviadas = resultados.stream()
                    .filter(resultado -> "EN".equals(resultado.statusFinal()))
                    .count();
            long erros = resultados.stream()
                    .filter(resultado -> "ER".equals(resultado.statusFinal()))
                    .count();
            log.info("Ciclo de notificacao de designacoes finalizado: novas={}, processadas={}, enviadas={}, erros={}",
                    novas, processadas, enviadas, erros);
        } catch (RuntimeException e) {
            log.warn("Falha sanitizada no processamento de notificacoes de designacoes: {}", e.getClass().getSimpleName());
        }
    }
}
