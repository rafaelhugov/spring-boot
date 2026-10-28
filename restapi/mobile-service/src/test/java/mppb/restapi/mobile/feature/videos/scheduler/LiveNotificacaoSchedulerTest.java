package mppb.restapi.mobile.feature.videos.scheduler;

import mppb.restapi.mobile.feature.notificacao.model.ResultadoEnvioFcm;
import mppb.restapi.mobile.feature.notificacao.service.FcmSenderService;
import mppb.restapi.mobile.feature.videos.dto.VideoDto;
import mppb.restapi.mobile.feature.videos.dto.VideosResponseDto;
import mppb.restapi.mobile.feature.videos.model.TipoVideo;
import mppb.restapi.mobile.feature.videos.service.VideoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LiveNotificacaoSchedulerTest {

    private VideoService videoService;
    private FcmSenderService fcmSenderService;
    private LiveNotificacaoScheduler scheduler;

    @BeforeEach
    void setUp() {
        videoService = Mockito.mock(VideoService.class);
        fcmSenderService = Mockito.mock(FcmSenderService.class);
        scheduler = new LiveNotificacaoScheduler(videoService, Optional.of(fcmSenderService));

        ReflectionTestUtils.setField(scheduler, "enabled", true);
        ReflectionTestUtils.setField(scheduler, "topic", "mppb_lives");
    }

    @Test
    void deveEnviarPushQuandoDetectarNovaLive() {
        VideoDto live = VideoDto.builder()
                .id("live123")
                .titulo("Sessão Extraordinária do CPJ")
                .link("https://www.youtube.com/watch?v=live123")
                .tipo(TipoVideo.AO_VIVO)
                .isAoVivo(true)
                .build();

        VideosResponseDto response = VideosResponseDto.builder()
                .liveAtiva(live)
                .videos(Collections.singletonList(live))
                .total(1)
                .build();

        when(videoService.getVideos()).thenReturn(response);
        when(fcmSenderService.enviarNovaLiveTopico(eq("mppb_lives"), any(VideoDto.class)))
                .thenReturn(ResultadoEnvioFcm.aceito("msg_12345"));

        scheduler.verificarENotificarLive();

        verify(fcmSenderService, times(1)).enviarNovaLiveTopico(eq("mppb_lives"), eq(live));
        assertEquals("live123", scheduler.getLastLiveNotificadaId());
    }

    @Test
    void naoDeveEnviarPushDuplicadoParaAMesmaLive() {
        VideoDto live = VideoDto.builder()
                .id("live123")
                .titulo("Sessão Extraordinária do CPJ")
                .tipo(TipoVideo.AO_VIVO)
                .isAoVivo(true)
                .build();

        VideosResponseDto response = VideosResponseDto.builder()
                .liveAtiva(live)
                .videos(Collections.singletonList(live))
                .total(1)
                .build();

        when(videoService.getVideos()).thenReturn(response);
        when(fcmSenderService.enviarNovaLiveTopico(eq("mppb_lives"), any(VideoDto.class)))
                .thenReturn(ResultadoEnvioFcm.aceito("msg_12345"));

        scheduler.setLastLiveNotificadaId("live123");
        scheduler.verificarENotificarLive();

        verify(fcmSenderService, never()).enviarNovaLiveTopico(any(), any());
    }

    @Test
    void deveResetarIdQuandoLiveEncerrar() {
        VideosResponseDto response = VideosResponseDto.builder()
                .liveAtiva(null)
                .videos(Collections.emptyList())
                .total(0)
                .build();

        when(videoService.getVideos()).thenReturn(response);

        scheduler.setLastLiveNotificadaId("liveAntiga");
        scheduler.verificarENotificarLive();

        assertNull(scheduler.getLastLiveNotificadaId());
        verify(fcmSenderService, never()).enviarNovaLiveTopico(any(), any());
    }

    @Test
    void naoDeveExecutarQuandoDesabilitado() {
        ReflectionTestUtils.setField(scheduler, "enabled", false);

        scheduler.verificarENotificarLive();

        verify(videoService, never()).getVideos();
        verify(fcmSenderService, never()).enviarNovaLiveTopico(any(), any());
    }
}
