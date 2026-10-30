package mppb.restapi.mobile.feature.designacao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import mppb.restapi.mobile.feature.designacao.model.DesignacaoAtivaRow;
import mppb.restapi.mobile.feature.designacao.model.ResultadoNotificacaoDesignacao;
import mppb.restapi.mobile.feature.notificacao.model.DispositivoFcm;
import mppb.restapi.mobile.feature.notificacao.model.ResultadoEnvioFcm;
import mppb.restapi.mobile.feature.notificacao.model.StatusEnvioFcm;
import mppb.restapi.mobile.feature.notificacao.repository.DispositivoFcmRepository;
import mppb.restapi.mobile.feature.notificacao.service.FcmSenderService;

class DesignacaoNotificacaoServiceTest {

    private FakeDispositivoFcmRepository repository;
    private FakeFcmSenderService sender;
    private DesignacaoNotificacaoService service;

    @BeforeEach
    void setUp() {
        repository = new FakeDispositivoFcmRepository();
        sender = new FakeFcmSenderService();
        service = new DesignacaoNotificacaoService(repository, provider(sender));
        ReflectionTestUtils.setField(service, "fcmEnabled", true);
    }

    @Test
    void enviaParaUsuarioComUmDispositivoAtivo() {
        repository.add(dispositivo(1L, "12345", "token-1", true));

        ResultadoNotificacaoDesignacao resultado = service.notificarNovaDesignacao(designacaoSensivel());

        assertEquals(1, resultado.dispositivosAtivos());
        assertEquals(1, resultado.aceitos());
        assertTrue(resultado.algumEnvioAceito());
        assertEquals(List.of("token-1"), sender.tokensRecebidos);
    }

    @Test
    void enviaParaMultiplosDispositivosAtivos() {
        repository.add(dispositivo(1L, "12345", "token-1", true));
        repository.add(dispositivo(2L, "12345", "token-2", true));

        ResultadoNotificacaoDesignacao resultado = service.notificarNovaDesignacao(designacaoSensivel());

        assertEquals(2, resultado.dispositivosAtivos());
        assertEquals(2, resultado.aceitos());
        assertEquals(List.of("token-1", "token-2"), sender.tokensRecebidos);
    }

    @Test
    void ignoraDispositivoInativo() {
        repository.add(dispositivo(1L, "12345", "token-ativo", true));
        repository.add(dispositivo(2L, "12345", "token-inativo", false));

        ResultadoNotificacaoDesignacao resultado = service.notificarNovaDesignacao(designacaoSensivel());

        assertEquals(1, resultado.dispositivosAtivos());
        assertEquals(List.of("token-ativo"), sender.tokensRecebidos);
    }

    @Test
    void nenhumDispositivoAtivoNaoEErroFcm() {
        ResultadoNotificacaoDesignacao resultado = service.notificarNovaDesignacao(designacaoSensivel());

        assertTrue(resultado.nenhumDispositivoAtivo());
        assertEquals(0, resultado.dispositivosAtivos());
        assertTrue(sender.tokensRecebidos.isEmpty());
    }

    @Test
    void sucessoEmUmDispositivoEFalhaEmOutroNaoInterrompeFluxo() {
        repository.add(dispositivo(1L, "12345", "token-ok", true));
        repository.add(dispositivo(2L, "12345", "token-falha", true));
        sender.resultados.put("token-falha", ResultadoEnvioFcm.falhaTransitoria());

        ResultadoNotificacaoDesignacao resultado = service.notificarNovaDesignacao(designacaoSensivel());

        assertEquals(1, resultado.aceitos());
        assertEquals(1, resultado.falhasTransitorias());
        assertTrue(resultado.possuiFalhaTransitoria());
        assertEquals(List.of("token-ok", "token-falha"), sender.tokensRecebidos);
    }

    @Test
    void tokenInvalidoDesativaSomenteDispositivoInterno() {
        repository.add(dispositivo(1L, "12345", "token-invalido", true));
        repository.add(dispositivo(2L, "99999", "token-outro-usuario", true));
        sender.resultados.put("token-invalido", ResultadoEnvioFcm.tokenInvalido());

        ResultadoNotificacaoDesignacao resultado = service.notificarNovaDesignacao(designacaoSensivel());

        assertEquals(1, resultado.tokensInvalidos());
        assertTrue(resultado.todosTokensInvalidos());
        assertEquals(List.of(1L), repository.idsDesativados);
        assertFalse(repository.rows.get(1L).ativo());
        assertTrue(repository.rows.get(2L).ativo());
    }

    @Test
    void tokenInvalidoNaoImpedeDemaisDispositivos() {
        repository.add(dispositivo(1L, "12345", "token-invalido", true));
        repository.add(dispositivo(2L, "12345", "token-ok", true));
        sender.resultados.put("token-invalido", ResultadoEnvioFcm.tokenInvalido());

        ResultadoNotificacaoDesignacao resultado = service.notificarNovaDesignacao(designacaoSensivel());

        assertEquals(1, resultado.tokensInvalidos());
        assertEquals(1, resultado.aceitos());
        assertEquals(List.of("token-invalido", "token-ok"), sender.tokensRecebidos);
        assertEquals(List.of(1L), repository.idsDesativados);
    }

    @Test
    void falhaTransitoriaMantemDispositivoAtivo() {
        repository.add(dispositivo(1L, "12345", "token-transitorio", true));
        sender.resultados.put("token-transitorio", ResultadoEnvioFcm.falhaTransitoria());

        ResultadoNotificacaoDesignacao resultado = service.notificarNovaDesignacao(designacaoSensivel());

        assertTrue(resultado.possuiFalhaTransitoria());
        assertTrue(repository.rows.get(1L).ativo());
        assertTrue(repository.idsDesativados.isEmpty());
    }

    @Test
    void falhaConfiguracaoMantemDispositivoAtivo() {
        repository.add(dispositivo(1L, "12345", "token-config", true));
        sender.resultados.put("token-config", ResultadoEnvioFcm.falhaConfiguracao());

        ResultadoNotificacaoDesignacao resultado = service.notificarNovaDesignacao(designacaoSensivel());

        assertTrue(resultado.possuiFalhaConfiguracao());
        assertTrue(repository.rows.get(1L).ativo());
        assertTrue(repository.idsDesativados.isEmpty());
    }

    @Test
    void falhaDesconhecidaFicaClassificadaNoAgregado() {
        repository.add(dispositivo(1L, "12345", "token-erro", true));
        sender.resultados.put("token-erro", ResultadoEnvioFcm.falhaDesconhecida());

        ResultadoNotificacaoDesignacao resultado = service.notificarNovaDesignacao(designacaoSensivel());

        assertTrue(resultado.possuiFalhaDesconhecida());
        assertTrue(repository.rows.get(1L).ativo());
    }

    @Test
    void payloadContinuaGenericoSemDadosDaDesignacao() throws Exception {
        repository.add(dispositivo(1L, "12345", "token-1", true));
        CapturingFcmClient client = new CapturingFcmClient();
        service = new DesignacaoNotificacaoService(repository, provider(new FcmSenderService(client)));
        ReflectionTestUtils.setField(service, "fcmEnabled", true);

        service.notificarNovaDesignacao(designacaoSensivel());

        Notification notification = valorCampo(client.message, "notification");
        assertEquals("Nova designação", valorCampo(notification, "title"));
        assertEquals("Você possui uma nova designação disponível.", valorCampo(notification, "body"));

        Map<String, String> data = valorCampo(client.message, "data");
        assertEquals("NOVAS_DESIGNACOES", data.get("tipo_evento"));
        assertEquals("/designacoes", data.get("rota"));
        assertEquals("NOVAS_DESIGNACOES_1001", data.get("id_evento_notificacao"));
        assertEquals(3, data.size());
        assertFalse(data.toString().contains("Promotoria"));
        assertFalse(data.toString().contains("João Pessoa"));
        assertFalse(data.toString().contains("12345"));
    }

    @Test
    void fcmDesligadoNaoBuscaDispositivosNemEnvia() {
        ReflectionTestUtils.setField(service, "fcmEnabled", false);
        repository.add(dispositivo(1L, "12345", "token-1", true));

        ResultadoNotificacaoDesignacao resultado = service.notificarNovaDesignacao(designacaoSensivel());

        assertTrue(resultado.fcmDesligado());
        assertEquals(0, repository.consultas);
        assertTrue(sender.tokensRecebidos.isEmpty());
    }

    @Test
    void fcmHabilitadoSemSenderClassificaFalhaConfiguracaoPorDispositivo() {
        repository.add(dispositivo(1L, "12345", "token-1", true));
        repository.add(dispositivo(2L, "12345", "token-2", true));
        service = new DesignacaoNotificacaoService(repository, provider(null));
        ReflectionTestUtils.setField(service, "fcmEnabled", true);

        ResultadoNotificacaoDesignacao resultado = service.notificarNovaDesignacao(designacaoSensivel());

        assertEquals(2, resultado.dispositivosAtivos());
        assertEquals(2, resultado.falhasConfiguracao());
        assertTrue(resultado.possuiFalhaConfiguracao());
        assertTrue(repository.idsDesativados.isEmpty());
    }

    private static ObjectProvider<FcmSenderService> provider(FcmSenderService sender) {
        return new ObjectProvider<>() {
            @Override
            public FcmSenderService getObject() {
                return sender;
            }

            @Override
            public FcmSenderService getIfAvailable() {
                return sender;
            }
        };
    }

    private static DispositivoFcm dispositivo(Long id, String matricula, String token, boolean ativo) {
        return new DispositivoFcm(
                id,
                "device-" + id,
                matricula,
                token,
                "hash-" + id,
                "ANDROID",
                ativo,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    private static DesignacaoAtivaRow designacaoSensivel() {
        return new DesignacaoAtivaRow(
                1001L,
                "12345",
                "F",
                "Função",
                "Cargo sensivel",
                "Promotoria de Justiça de João Pessoa",
                "Comarca sensivel",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                LocalDate.of(2026, 8, 30),
                "ATO-123",
                false,
                "S",
                "Servidor");
    }

    @SuppressWarnings("unchecked")
    private static <T> T valorCampo(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(target);
    }

    private static class FakeDispositivoFcmRepository extends DispositivoFcmRepository {
        private final Map<Long, DispositivoFcm> rows = new LinkedHashMap<>();
        private final List<Long> idsDesativados = new ArrayList<>();
        private int consultas;

        FakeDispositivoFcmRepository() {
            super(null);
        }

        void add(DispositivoFcm dispositivo) {
            rows.put(dispositivo.id(), dispositivo);
        }

        @Override
        public List<DispositivoFcm> findAtivosByMatricula(String matricula) {
            consultas++;
            return rows.values().stream()
                    .filter(DispositivoFcm::ativo)
                    .filter(row -> matricula.equals(row.servidorMatricula()))
                    .toList();
        }

        @Override
        public boolean desativarPorId(Long id) {
            DispositivoFcm atual = rows.get(id);
            if (atual == null || !atual.ativo()) {
                return false;
            }
            idsDesativados.add(id);
            rows.put(id, new DispositivoFcm(
                    atual.id(),
                    atual.identificador(),
                    atual.servidorMatricula(),
                    atual.tokenFcm(),
                    atual.tokenFcmHash(),
                    atual.plataforma(),
                    false,
                    atual.dataCriacao(),
                    LocalDateTime.now(),
                    atual.dataUltimoUso()));
            return true;
        }
    }

    private static class FakeFcmSenderService extends FcmSenderService {
        private final List<String> tokensRecebidos = new ArrayList<>();
        private final Map<String, ResultadoEnvioFcm> resultados = new LinkedHashMap<>();

        FakeFcmSenderService() {
            super(message -> "unused");
        }

        @Override
        public ResultadoEnvioFcm enviarNovaDesignacao(String tokenFcm) {
            return enviarNovaDesignacao(tokenFcm, null);
        }

        @Override
        public ResultadoEnvioFcm enviarNovaDesignacao(String tokenFcm, Long designacaoId) {
            return enviarNovaDesignacao(tokenFcm, designacaoId, true);
        }

        @Override
        public ResultadoEnvioFcm enviarNovaDesignacao(String tokenFcm, Long designacaoId, boolean exibirPush) {
            tokensRecebidos.add(tokenFcm);
            return resultados.getOrDefault(tokenFcm, ResultadoEnvioFcm.aceito("message-" + tokenFcm));
        }
    }

    private static class CapturingFcmClient implements FcmSenderService.FcmMessagingClient {
        private Message message;

        @Override
        public String send(Message message) {
            this.message = message;
            return "message-id";
        }
    }
}
