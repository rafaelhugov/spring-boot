package mppb.restapi.mobile.feature.designacao.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import mppb.restapi.mobile.feature.designacao.service.DesignacaoDeteccaoService;

@Component
@RequiredArgsConstructor
public class DesignacaoNotificacaoScheduler {

    private final DesignacaoDeteccaoService deteccaoService;

    @Value("${designacoes.notificacao.enabled:false}")
    private boolean enabled;

    @Scheduled(cron = "${designacoes.notificacao.cron:0 */15 * * * *}")
    public void executar() {
        if (enabled) {
            deteccaoService.detectarNovasDesignacoes();
        }
    }
}
