package mppb.restapi.mobile.feature.mensagem.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.mobile.feature.mensagem.service.MensagemEnvioService;

@Component
@RequiredArgsConstructor
@Slf4j
public class MensagemAgendadaScheduler {

    private final MensagemEnvioService mensagemEnvioService;

    @Scheduled(cron = "${mensagens.agendamento-cron:0 * * * * *}")
    public void executar() {
        int publicadas = mensagemEnvioService.processarAgendadas();
        if (publicadas > 0) {
            log.info("Publicadas {} mensagens/avisos agendados.", publicadas);
        }
    }
}

