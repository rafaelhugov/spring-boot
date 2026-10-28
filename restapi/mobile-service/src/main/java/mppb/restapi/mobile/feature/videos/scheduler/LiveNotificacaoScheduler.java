package mppb.restapi.mobile.feature.videos.scheduler;

import lombok.extern.slf4j.Slf4j;
import mppb.restapi.mobile.feature.notificacao.model.ResultadoEnvioFcm;
import mppb.restapi.mobile.feature.notificacao.service.FcmSenderService;
import mppb.restapi.mobile.feature.videos.dto.VideoDto;
import mppb.restapi.mobile.feature.videos.dto.VideosResponseDto;
import mppb.restapi.mobile.feature.videos.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class LiveNotificacaoScheduler {

    private final VideoService videoService;
    private final Optional<FcmSenderService> fcmSenderService;

    @Value("${lives.notificacao.enabled:true}")
    private boolean enabled;

    @Value("${lives.notificacao.fcm.topic:mppb_lives}")
    private String topic;

    private String lastLiveNotificadaId;

    @Autowired
    public LiveNotificacaoScheduler(VideoService videoService, Optional<FcmSenderService> fcmSenderService) {
        this.videoService = videoService;
        this.fcmSenderService = fcmSenderService;
    }

    @Scheduled(cron = "${lives.notificacao.cron:0 */3 * * * *}")
    public void verificarENotificarLive() {
        if (!enabled) {
            return;
        }

        try {
            VideosResponseDto response = videoService.getVideos();
            VideoDto liveAtiva = response != null ? response.getLiveAtiva() : null;

            if (liveAtiva != null && liveAtiva.getId() != null) {
                String liveId = liveAtiva.getId();

                if (liveId.equals(lastLiveNotificadaId)) {
                    log.debug("Live já notificada anteriormente: {}", liveId);
                    return;
                }

                log.info("Nova transmissão ao vivo detectada no Canal MPPB: id={}, título='{}'",
                        liveId, liveAtiva.getTitulo());

                if (fcmSenderService.isPresent()) {
                    ResultadoEnvioFcm resultado = fcmSenderService.get().enviarNovaLiveTopico(topic, liveAtiva);
                    log.info("Push de nova live enviado para tópico '{}': status={}, messageId={}",
                            topic, resultado.status(), resultado.messageId());
                } else {
                    log.warn("FCM indisponível ou desabilitado para envio de notificação de live");
                }

                this.lastLiveNotificadaId = liveId;
            } else {
                if (lastLiveNotificadaId != null) {
                    log.info("Transmissão ao vivo anterior encerrada (id={}). Monitor pronto para nova live.",
                            lastLiveNotificadaId);
                    this.lastLiveNotificadaId = null;
                }
            }
        } catch (Exception e) {
            log.warn("Erro ao verificar transmissão ao vivo para notificação: {}", e.getMessage());
        }
    }

    public String getLastLiveNotificadaId() {
        return lastLiveNotificadaId;
    }

    public void setLastLiveNotificadaId(String lastLiveNotificadaId) {
        this.lastLiveNotificadaId = lastLiveNotificadaId;
    }
}
