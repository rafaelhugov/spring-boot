package mppb.restapi.mobile.feature.noticias.scheduler;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.mobile.feature.noticias.model.ResultadoProcessamentoNotificacaoNoticia;
import mppb.restapi.mobile.feature.noticias.service.NoticiaDeteccaoService;
import mppb.restapi.mobile.feature.noticias.service.NoticiaProcessamentoNotificacaoService;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoticiaNotificacaoScheduler {

    private final NoticiaDeteccaoService deteccaoService;
    private final NoticiaProcessamentoNotificacaoService processamentoService;

    @Value("${noticias.notificacao.enabled:false}")
    private boolean enabled;

    @Value("${designacoes.notificacao.fcm.enabled:false}")
    private boolean fcmEnabled;

    @Scheduled(cron = "${noticias.notificacao.cron:0 */15 * * * *}")
    public void executar() {
        if (!enabled) {
            return;
        }

        log.info("Ciclo de notificacao de noticias iniciado");
        int novas = 0;
        try {
            novas = deteccaoService.detectarNovasNoticias();
        } catch (RuntimeException e) {
            log.warn("Falha sanitizada na deteccao de noticias: {}", e.getClass().getSimpleName());
        }

        if (!fcmEnabled) {
            log.info("Ciclo de notificacao de noticias sem processamento FCM: novas={}", novas);
            return;
        }

        try {
            List<ResultadoProcessamentoNotificacaoNoticia> resultados = processamentoService.processarPendentes();
            long enviadas = resultados.stream()
                    .filter(resultado -> "EN".equals(resultado.statusFinal()))
                    .count();
            long erros = resultados.stream()
                    .filter(resultado -> "ER".equals(resultado.statusFinal()))
                    .count();
            log.info("Ciclo de notificacao de noticias finalizado: novas={}, processadas={}, enviadas={}, erros={}",
                    novas, resultados.size(), enviadas, erros);
        } catch (RuntimeException e) {
            log.warn("Falha sanitizada no processamento de notificacoes de noticias: {}", e.getClass().getSimpleName());
        }
    }
}
