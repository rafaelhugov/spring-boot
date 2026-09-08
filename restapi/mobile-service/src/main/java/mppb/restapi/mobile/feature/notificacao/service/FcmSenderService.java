package mppb.restapi.mobile.feature.notificacao.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.firebase.ErrorCode;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;

import mppb.restapi.mobile.feature.notificacao.model.ResultadoEnvioFcm;

@Service
@ConditionalOnBean(FirebaseMessaging.class)
public class FcmSenderService {

    static final String TITULO_NOVA_DESIGNACAO = "Nova designação";
    static final String CORPO_NOVA_DESIGNACAO = "Você possui uma nova designação disponível.";
    static final String TIPO_EVENTO_NOVAS_DESIGNACOES = "NOVAS_DESIGNACOES";
    static final String ROTA_DESIGNACOES = "/designacoes";
    static final String TITULO_NOVA_NOTICIA = "Nova notícia";
    static final String CORPO_NOVA_NOTICIA = "Uma nova notícia está disponível no MPPB+.";
    static final String TIPO_EVENTO_NOVAS_NOTICIAS = "NOVAS_NOTICIAS";
    static final String ROTA_NOTICIAS = "/noticias";
    static final String CANAL_ANDROID_ALERTAS = "mppb_alertas";
    static final String COR_ANDROID_MPPB = "#D62828";

    private final FcmMessagingClient messagingClient;

    @Autowired
    public FcmSenderService(FirebaseMessaging firebaseMessaging) {
        this(firebaseMessaging::send);
    }

    public FcmSenderService(FcmMessagingClient messagingClient) {
        this.messagingClient = messagingClient;
    }

    public ResultadoEnvioFcm enviarNovaDesignacao(String tokenFcm) {
        String token = obrigatorio(tokenFcm, "tokenFcm");

        try {
            String messageId = messagingClient.send(novaDesignacaoMessage(token));
            return ResultadoEnvioFcm.aceito(messageId);
        } catch (FirebaseMessagingException e) {
            return classificarErro(e);
        } catch (RuntimeException e) {
            return ResultadoEnvioFcm.falhaDesconhecida();
        }
    }

    public ResultadoEnvioFcm enviarNovaNoticiaTopico(String topic) {
        String nomeTopico = obrigatorio(topic, "topic");

        try {
            String messageId = messagingClient.send(novaNoticiaTopicMessage(nomeTopico));
            return ResultadoEnvioFcm.aceito(messageId);
        } catch (FirebaseMessagingException e) {
            return classificarErro(e);
        } catch (RuntimeException e) {
            return ResultadoEnvioFcm.falhaDesconhecida();
        }
    }

    private Message novaDesignacaoMessage(String tokenFcm) {
        return Message.builder()
                .setToken(tokenFcm)
                .setNotification(Notification.builder()
                        .setTitle(TITULO_NOVA_DESIGNACAO)
                        .setBody(CORPO_NOVA_DESIGNACAO)
                        .build())
                .setAndroidConfig(androidConfig(
                        TITULO_NOVA_DESIGNACAO,
                        CORPO_NOVA_DESIGNACAO,
                        TIPO_EVENTO_NOVAS_DESIGNACOES))
                .putData("tipo_evento", TIPO_EVENTO_NOVAS_DESIGNACOES)
                .putData("rota", ROTA_DESIGNACOES)
                .build();
    }

    private Message novaNoticiaTopicMessage(String topic) {
        return Message.builder()
                .setTopic(topic)
                .setNotification(Notification.builder()
                        .setTitle(TITULO_NOVA_NOTICIA)
                        .setBody(CORPO_NOVA_NOTICIA)
                        .build())
                .setAndroidConfig(androidConfig(
                        TITULO_NOVA_NOTICIA,
                        CORPO_NOVA_NOTICIA,
                        TIPO_EVENTO_NOVAS_NOTICIAS))
                .putData("tipo_evento", TIPO_EVENTO_NOVAS_NOTICIAS)
                .putData("rota", ROTA_NOTICIAS)
                .build();
    }

    private AndroidConfig androidConfig(String title, String body, String tag) {
        return AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setNotification(AndroidNotification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .setChannelId(CANAL_ANDROID_ALERTAS)
                        .setColor(COR_ANDROID_MPPB)
                        .setDefaultSound(true)
                        .setDefaultVibrateTimings(true)
                        .setPriority(AndroidNotification.Priority.HIGH)
                        .setVisibility(AndroidNotification.Visibility.PUBLIC)
                        .setTag(tag)
                        .build())
                .build();
    }

    private ResultadoEnvioFcm classificarErro(FirebaseMessagingException erro) {
        MessagingErrorCode messagingErrorCode = erro.getMessagingErrorCode();
        if (messagingErrorCode == MessagingErrorCode.UNREGISTERED
                || messagingErrorCode == MessagingErrorCode.INVALID_ARGUMENT) {
            return ResultadoEnvioFcm.tokenInvalido();
        }

        if (messagingErrorCode == MessagingErrorCode.UNAVAILABLE
                || messagingErrorCode == MessagingErrorCode.INTERNAL
                || messagingErrorCode == MessagingErrorCode.QUOTA_EXCEEDED) {
            return ResultadoEnvioFcm.falhaTransitoria();
        }

        if (messagingErrorCode == MessagingErrorCode.SENDER_ID_MISMATCH
                || messagingErrorCode == MessagingErrorCode.THIRD_PARTY_AUTH_ERROR) {
            return ResultadoEnvioFcm.falhaConfiguracao();
        }

        ErrorCode errorCode = erro.getErrorCode();
        if (errorCode == ErrorCode.UNAUTHENTICATED
                || errorCode == ErrorCode.PERMISSION_DENIED
                || errorCode == ErrorCode.FAILED_PRECONDITION) {
            return ResultadoEnvioFcm.falhaConfiguracao();
        }

        if (errorCode == ErrorCode.UNAVAILABLE
                || errorCode == ErrorCode.INTERNAL
                || errorCode == ErrorCode.DEADLINE_EXCEEDED
                || errorCode == ErrorCode.RESOURCE_EXHAUSTED
                || errorCode == ErrorCode.ABORTED) {
            return ResultadoEnvioFcm.falhaTransitoria();
        }

        if (errorCode == ErrorCode.INVALID_ARGUMENT || errorCode == ErrorCode.NOT_FOUND) {
            return ResultadoEnvioFcm.tokenInvalido();
        }

        return ResultadoEnvioFcm.falhaDesconhecida();
    }

    private String obrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " obrigatorio");
        }
        return valor.trim();
    }

    @FunctionalInterface
    public interface FcmMessagingClient {
        String send(Message message) throws FirebaseMessagingException;
    }
}
