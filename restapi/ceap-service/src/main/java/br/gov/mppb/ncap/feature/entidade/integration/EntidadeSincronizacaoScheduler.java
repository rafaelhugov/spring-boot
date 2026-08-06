package br.gov.mppb.ncap.feature.entidade.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EntidadeSincronizacaoScheduler {

    private final EntidadeSincronizacaoService syncService;

    // Executa a cada dia às 03:00 da manhã por padrão, configurável via properties
    @Scheduled(cron = "${ncap.scheduler.sincronizacao-cron:0 0 3 * * ?}")
    public void executarSincronizacao() {
        log.info("Disparando job agendado de sincronização de entidades...");
        try {
            syncService.sincronizar();
        } catch (Exception e) {
            log.error("Erro ao executar job agendado de sincronização de entidades", e);
        }
    }
}
