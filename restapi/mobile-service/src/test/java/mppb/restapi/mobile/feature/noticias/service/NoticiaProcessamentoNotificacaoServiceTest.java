package mppb.restapi.mobile.feature.noticias.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import com.google.firebase.messaging.Message;

import mppb.restapi.mobile.feature.noticias.model.NoticiaPendenteNotificacao;
import mppb.restapi.mobile.feature.noticias.model.NoticiaPublicada;
import mppb.restapi.mobile.feature.noticias.model.ResultadoProcessamentoNotificacaoNoticia;
import mppb.restapi.mobile.feature.noticias.repository.NoticiaControleRepository;
import mppb.restapi.mobile.feature.notificacao.model.ResultadoEnvioFcm;
import mppb.restapi.mobile.feature.notificacao.service.FcmSenderService;

class NoticiaProcessamentoNotificacaoServiceTest {

    private FakeNoticiaControleRepository repository;
    private FakeFcmSenderService sender;
    private FakeNoticiaService noticiaService;
    private NoticiaProcessamentoNotificacaoService service;

    @BeforeEach
    void setUp() throws Exception {
        repository = new FakeNoticiaControleRepository();
        sender = new FakeFcmSenderService();
        noticiaService = new FakeNoticiaService();
        service = new NoticiaProcessamentoNotificacaoService(repository, provider(sender), noticiaService);
        setField(service, "batchSize", 50);
        setField(service, "maxTentativas", 3);
        setField(service, "topic", "mppb_noticias");
    }

    @Test
    void sucessoMarcaNoticiaComoEnviada() {
        repository.pendentes = List.of(pendente(1L, "PE", 0));
        sender.resultado = ResultadoEnvioFcm.aceito("message-id");

        List<ResultadoProcessamentoNotificacaoNoticia> resultados = service.processarPendentes();

        assertEquals(1, resultados.size());
        assertEquals("EN", resultados.get(0).statusFinal());
        assertEquals(List.of(1L), repository.enviadas);
        assertEquals(List.of("mppb_noticias"), sender.topicos);
        assertEquals("portal:1", sender.noticias.get(0).chaveExterna());
    }

    @Test
    void falhaTransitoriaMarcaErroSanitizado() {
        repository.pendentes = List.of(pendente(2L, "PE", 1));
        sender.resultado = ResultadoEnvioFcm.falhaTransitoria();

        List<ResultadoProcessamentoNotificacaoNoticia> resultados = service.processarPendentes();

        assertEquals(1, resultados.size());
        assertEquals("ER", resultados.get(0).statusFinal());
        assertEquals("FALHA_TRANSITORIA_FCM", repository.erros.get(0));
    }

    @Test
    void fcmIndisponivelNaoReservaPendenciasNemConsomeTentativa() {
        service = new NoticiaProcessamentoNotificacaoService(repository, provider(null), noticiaService);

        List<ResultadoProcessamentoNotificacaoNoticia> resultados = service.processarPendentes();

        assertEquals(0, resultados.size());
        assertEquals(0, repository.reservas);
    }

    private static NoticiaPendenteNotificacao pendente(Long id, String status, int tentativas) {
        return new NoticiaPendenteNotificacao(id, "reserva", "portal:" + id, status, tentativas);
    }

    private static ObjectProvider<FcmSenderService> provider(FcmSenderService sender) {
        return new ObjectProvider<>() {
            @Override
            public FcmSenderService getObject(Object... args) {
                return sender;
            }

            @Override
            public FcmSenderService getIfAvailable() {
                return sender;
            }

            @Override
            public FcmSenderService getObject() {
                return sender;
            }
        };
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class FakeNoticiaControleRepository extends NoticiaControleRepository {
        private List<NoticiaPendenteNotificacao> pendentes = List.of();
        private final List<Long> enviadas = new ArrayList<>();
        private final List<String> erros = new ArrayList<>();
        private int reservas;

        FakeNoticiaControleRepository() {
            super(null);
        }

        @Override
        public List<NoticiaPendenteNotificacao> reservarPendentesParaNotificacao(
                int batchSize,
                int maxTentativas,
                String identificadorReserva) {
            reservas++;
            return pendentes;
        }

        @Override
        public boolean marcarNotificacaoEnviada(Long id, String identificadorReserva) {
            enviadas.add(id);
            return true;
        }

        @Override
        public boolean marcarNotificacaoErro(Long id, String identificadorReserva, String mensagemErro) {
            erros.add(mensagemErro);
            return true;
        }
    }

    private static class FakeFcmSenderService extends FcmSenderService {
        private ResultadoEnvioFcm resultado = ResultadoEnvioFcm.aceito("message-id");
        private final List<String> topicos = new ArrayList<>();
        private final List<NoticiaPublicada> noticias = new ArrayList<>();

        FakeFcmSenderService() {
            super((Message message) -> "message-id");
        }

        @Override
        public ResultadoEnvioFcm enviarNovaNoticiaTopico(String topic, NoticiaPublicada noticia) {
            topicos.add(topic);
            noticias.add(noticia);
            return resultado;
        }
    }

    private static class FakeNoticiaService extends NoticiaService {

        @Override
        public List<NoticiaPublicada> getUltimasNoticiasPublicadas() {
            return List.of(
                    new NoticiaPublicada(
                            "portal:1",
                            "Titulo da noticia",
                            "https://www.mppb.mp.br/noticia/1",
                            "Previa da noticia",
                            "https://www.mppb.mp.br/imagem.jpg",
                            LocalDateTime.now(),
                            "08/09/2026 16:09",
                            "CEAF"));
        }
    }
}
