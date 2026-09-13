package mppb.restapi.mobile.feature.notificacao.service;

import java.util.UUID;

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

import mppb.restapi.mobile.feature.noticias.model.NoticiaPublicada;
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
    static final String DATA_ID_EVENTO_NOTIFICACAO = "id_evento_notificacao";

    private final FcmMessagingClient messagingClient;

    @Autowired
    public FcmSenderService(FirebaseMessaging firebaseMessaging) {
        this(firebaseMessaging::send);
    }

    public FcmSenderService(FcmMessagingClient messagingClient) {
        this.messagingClient = messagingClient;
    }

    public ResultadoEnvioFcm enviarNovaDesignacao(String tokenFcm) {
        return enviarNovaDesignacao(tokenFcm, null);
    }

    public ResultadoEnvioFcm enviarNovaDesignacao(String tokenFcm, Long designacaoId) {
        String token = obrigatorio(tokenFcm, "tokenFcm");

        try {
            String messageId = messagingClient.send(novaDesignacaoMessage(token, designacaoId));
            return ResultadoEnvioFcm.aceito(messageId);
        } catch (FirebaseMessagingException e) {
            return classificarErro(e);
        } catch (RuntimeException e) {
            return ResultadoEnvioFcm.falhaDesconhecida();
        }
    }

    public ResultadoEnvioFcm enviarNovaNoticiaTopico(String topic) {
        return enviarNovaNoticiaTopico(topic, null);
    }

    public ResultadoEnvioFcm enviarNovaNoticiaTopico(String topic, NoticiaPublicada noticia) {
        String nomeTopico = obrigatorio(topic, "topic");

        try {
            String messageId = messagingClient.send(novaNoticiaTopicMessage(nomeTopico, noticia));
            return ResultadoEnvioFcm.aceito(messageId);
        } catch (FirebaseMessagingException e) {
            return classificarErro(e);
        } catch (RuntimeException e) {
            return ResultadoEnvioFcm.falhaDesconhecida();
        }
    }

    private Message novaDesignacaoMessage(String tokenFcm, Long designacaoId) {
        String notificationTag = notificationTag(TIPO_EVENTO_NOVAS_DESIGNACOES, designacaoId);
        return Message.builder()
                .setToken(tokenFcm)
                .setNotification(Notification.builder()
                        .setTitle(TITULO_NOVA_DESIGNACAO)
                        .setBody(CORPO_NOVA_DESIGNACAO)
                        .build())
                .setAndroidConfig(androidConfig(
                        TITULO_NOVA_DESIGNACAO,
                        CORPO_NOVA_DESIGNACAO,
                        notificationTag))
                .putData("tipo_evento", TIPO_EVENTO_NOVAS_DESIGNACOES)
                .putData("rota", ROTA_DESIGNACOES)
                .putData(DATA_ID_EVENTO_NOTIFICACAO, notificationTag)
                .build();
    }

    private Message novaNoticiaTopicMessage(String topic, NoticiaPublicada noticia) {
        String title = textoOuPadrao(noticia == null ? null : noticia.titulo(), TITULO_NOVA_NOTICIA, 120);
        String body = textoOuPadrao(noticia == null ? null : noticia.resumo(), CORPO_NOVA_NOTICIA, 180);
        String imageUrl = urlOuNull(noticia == null ? null : noticia.imagemUrl());
        String notificationTag = notificationTag(
                TIPO_EVENTO_NOVAS_NOTICIAS,
                noticia == null ? null : noticia.chaveExterna());

        Notification.Builder notification = Notification.builder()
                .setTitle(title)
                .setBody(body);
        if (imageUrl != null) {
            notification.setImage(imageUrl);
        }

        Message.Builder message = Message.builder()
                .setTopic(topic)
                .setNotification(notification.build())
                .setAndroidConfig(androidConfig(
                        title,
                        body,
                        notificationTag,
                        imageUrl))
                .putData("tipo_evento", TIPO_EVENTO_NOVAS_NOTICIAS)
                .putData("rota", ROTA_NOTICIAS)
                .putData(DATA_ID_EVENTO_NOTIFICACAO, notificationTag);

        if (noticia != null) {
            putDataIfPresent(message, "noticia_chave", noticia.chaveExterna());
            putDataIfPresent(message, "noticia_link", noticia.link());
            putDataIfPresent(message, "noticia_titulo", title);
            putDataIfPresent(message, "noticia_imagem", imageUrl);
        }
        return message.build();
    }

    private AndroidConfig androidConfig(String title, String body, String tag) {
        return androidConfig(title, body, tag, null);
    }

    private AndroidConfig androidConfig(String title, String body, String tag, String imageUrl) {
        AndroidNotification.Builder notification = AndroidNotification.builder()
                .setTitle(title)
                .setBody(body)
                .setChannelId(CANAL_ANDROID_ALERTAS)
                .setColor(COR_ANDROID_MPPB)
                .setDefaultSound(true)
                .setDefaultVibrateTimings(true)
                .setPriority(AndroidNotification.Priority.HIGH)
                .setVisibility(AndroidNotification.Visibility.PUBLIC)
                .setTag(tag);
        if (imageUrl != null) {
            notification.setImage(imageUrl);
        }

        return AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setNotification(notification.build())
                .build();
    }

    private static String notificationTag(String eventType, Object identifier) {
        String suffix = identifier == null ? UUID.randomUUID().toString() : identifier.toString();
        return eventType + "_" + safeTagSegment(suffix);
    }

    private static String safeTagSegment(String value) {
        String normalized = value == null || value.isBlank()
                ? UUID.randomUUID().toString()
                : value.trim().replaceAll("[^A-Za-z0-9._-]", "_");
        return normalized.length() <= 80 ? normalized : normalized.substring(0, 80);
    }

    private static void putDataIfPresent(Message.Builder message, String key, String value) {
        if (value != null && !value.isBlank()) {
            message.putData(key, value.trim());
        }
    }

    private static String textoOuPadrao(String valor, String padrao, int limite) {
        String texto = valor == null || valor.isBlank() ? padrao : valor.trim();
        if (texto.length() <= limite) {
            return texto;
        }
        return texto.substring(0, limite - 3).trim() + "...";
    }

    private static String urlOuNull(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String url = valor.trim();
        return url.startsWith("https://") || url.startsWith("http://") ? url : null;
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
