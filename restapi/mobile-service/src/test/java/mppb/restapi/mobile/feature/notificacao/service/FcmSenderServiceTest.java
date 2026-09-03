package mppb.restapi.mobile.feature.notificacao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.firebase.ErrorCode;
import com.google.firebase.FirebaseException;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;

import mppb.restapi.mobile.feature.notificacao.model.ResultadoEnvioFcm;
import mppb.restapi.mobile.feature.notificacao.model.StatusEnvioFcm;

class FcmSenderServiceTest {

    @Test
    void montaMensagemGenericaComPayloadTecnicoPermitido() throws Exception {
        FakeFcmMessagingClient client = new FakeFcmMessagingClient();
        client.response = "projects/appmppb/messages/123";
        FcmSenderService service = new FcmSenderService(client);

        ResultadoEnvioFcm resultado = service.enviarNovaDesignacao("token-dispositivo");

        assertEquals(StatusEnvioFcm.ACEITO, resultado.status());
        assertEquals("projects/appmppb/messages/123", resultado.messageId());
        assertEquals("token-dispositivo", valorCampo(client.message, "token"));

        Notification notification = valorCampo(client.message, "notification");
        assertEquals("Nova designação", valorCampo(notification, "title"));
        assertEquals("Você possui uma nova designação disponível.", valorCampo(notification, "body"));

        Map<String, String> data = valorCampo(client.message, "data");
        assertEquals("NOVAS_DESIGNACOES", data.get("tipo_evento"));
        assertEquals("/designacoes", data.get("rota"));
        assertEquals(2, data.size());
    }

    @Test
    void naoIncluiDadosSensiveisNoPayload() throws Exception {
        FakeFcmMessagingClient client = new FakeFcmMessagingClient();
        FcmSenderService service = new FcmSenderService(client);

        service.enviarNovaDesignacao("token-dispositivo");

        Map<String, String> data = valorCampo(client.message, "data");
        assertFalse(data.containsKey("matricula"));
        assertFalse(data.containsKey("nome"));
        assertFalse(data.containsKey("cpf"));
        assertFalse(data.containsKey("promotoria"));
        assertFalse(data.containsKey("comarca"));
        assertFalse(data.containsKey("processo"));
        assertFalse(data.containsKey("cargo"));
        assertFalse(data.containsKey("periodo"));
        assertFalse(data.containsKey("descricao"));
        assertFalse(data.containsKey("jwt"));
        assertFalse(data.containsKey("credenciais"));
    }

    @Test
    void sucessoRetornaEnvioAceito() {
        FakeFcmMessagingClient client = new FakeFcmMessagingClient();
        client.response = "message-id";
        FcmSenderService service = new FcmSenderService(client);

        ResultadoEnvioFcm resultado = service.enviarNovaDesignacao("token-dispositivo");

        assertEquals(StatusEnvioFcm.ACEITO, resultado.status());
        assertEquals("message-id", resultado.messageId());
        assertEquals(1, client.sendCount);
    }

    @Test
    void tokenNaoRegistradoRetornaTokenInvalido() throws Exception {
        FakeFcmMessagingClient client = new FakeFcmMessagingClient();
        client.exception = messagingException(ErrorCode.NOT_FOUND, MessagingErrorCode.UNREGISTERED);
        FcmSenderService service = new FcmSenderService(client);

        ResultadoEnvioFcm resultado = service.enviarNovaDesignacao("token-dispositivo");

        assertEquals(StatusEnvioFcm.TOKEN_INVALIDO, resultado.status());
        assertNull(resultado.messageId());
        assertEquals(1, client.sendCount);
    }

    @Test
    void tokenInvalidoRetornaTokenInvalido() throws Exception {
        FakeFcmMessagingClient client = new FakeFcmMessagingClient();
        client.exception = messagingException(ErrorCode.INVALID_ARGUMENT, MessagingErrorCode.INVALID_ARGUMENT);
        FcmSenderService service = new FcmSenderService(client);

        ResultadoEnvioFcm resultado = service.enviarNovaDesignacao("token-dispositivo");

        assertEquals(StatusEnvioFcm.TOKEN_INVALIDO, resultado.status());
    }

    @Test
    void erroTransitorioRetornaFalhaTransitoriaSemRetryAutomatico() throws Exception {
        FakeFcmMessagingClient client = new FakeFcmMessagingClient();
        client.exception = messagingException(ErrorCode.UNAVAILABLE, MessagingErrorCode.UNAVAILABLE);
        FcmSenderService service = new FcmSenderService(client);

        ResultadoEnvioFcm resultado = service.enviarNovaDesignacao("token-dispositivo");

        assertEquals(StatusEnvioFcm.FALHA_TRANSITORIA, resultado.status());
        assertEquals(1, client.sendCount);
    }

    @Test
    void erroDeQuotaRetornaFalhaTransitoriaSemRetryAutomatico() throws Exception {
        FakeFcmMessagingClient client = new FakeFcmMessagingClient();
        client.exception = messagingException(ErrorCode.RESOURCE_EXHAUSTED, MessagingErrorCode.QUOTA_EXCEEDED);
        FcmSenderService service = new FcmSenderService(client);

        ResultadoEnvioFcm resultado = service.enviarNovaDesignacao("token-dispositivo");

        assertEquals(StatusEnvioFcm.FALHA_TRANSITORIA, resultado.status());
        assertEquals(1, client.sendCount);
    }

    @Test
    void erroDeAutenticacaoRetornaFalhaConfiguracao() throws Exception {
        FakeFcmMessagingClient client = new FakeFcmMessagingClient();
        client.exception = messagingException(ErrorCode.UNAUTHENTICATED, null);
        FcmSenderService service = new FcmSenderService(client);

        ResultadoEnvioFcm resultado = service.enviarNovaDesignacao("token-dispositivo");

        assertEquals(StatusEnvioFcm.FALHA_CONFIGURACAO, resultado.status());
    }

    @Test
    void erroDeConfiguracaoDoSenderRetornaFalhaConfiguracao() throws Exception {
        FakeFcmMessagingClient client = new FakeFcmMessagingClient();
        client.exception = messagingException(ErrorCode.PERMISSION_DENIED, MessagingErrorCode.SENDER_ID_MISMATCH);
        FcmSenderService service = new FcmSenderService(client);

        ResultadoEnvioFcm resultado = service.enviarNovaDesignacao("token-dispositivo");

        assertEquals(StatusEnvioFcm.FALHA_CONFIGURACAO, resultado.status());
    }

    @Test
    void erroDesconhecidoRetornaFalhaDesconhecida() throws Exception {
        FakeFcmMessagingClient client = new FakeFcmMessagingClient();
        client.exception = messagingException(ErrorCode.UNKNOWN, null);
        FcmSenderService service = new FcmSenderService(client);

        ResultadoEnvioFcm resultado = service.enviarNovaDesignacao("token-dispositivo");

        assertEquals(StatusEnvioFcm.FALHA_DESCONHECIDA, resultado.status());
    }

    @Test
    void rejeitaTokenAusente() {
        FcmSenderService service = new FcmSenderService(new FakeFcmMessagingClient());

        assertThrows(IllegalArgumentException.class, () -> service.enviarNovaDesignacao(null));
        assertThrows(IllegalArgumentException.class, () -> service.enviarNovaDesignacao("  "));
    }

    private static FirebaseMessagingException messagingException(
            ErrorCode errorCode,
            MessagingErrorCode messagingErrorCode) throws Exception {
        FirebaseException firebaseException = new FirebaseException(errorCode, "erro-firebase", null);
        Method factory = FirebaseMessagingException.class
                .getDeclaredMethod("withMessagingErrorCode", FirebaseException.class, MessagingErrorCode.class);
        factory.setAccessible(true);
        try {
            return (FirebaseMessagingException) factory.invoke(null, firebaseException, messagingErrorCode);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T valorCampo(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(target);
    }

    private static class FakeFcmMessagingClient implements FcmSenderService.FcmMessagingClient {

        private int sendCount;
        private Message message;
        private String response = "message-id";
        private FirebaseMessagingException exception;

        @Override
        public String send(Message message) throws FirebaseMessagingException {
            sendCount++;
            this.message = message;
            if (exception != null) {
                throw exception;
            }
            return response;
        }
    }
}
