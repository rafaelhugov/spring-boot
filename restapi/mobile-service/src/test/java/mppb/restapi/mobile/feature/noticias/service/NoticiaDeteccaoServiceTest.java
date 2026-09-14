package mppb.restapi.mobile.feature.noticias.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import mppb.restapi.mobile.feature.noticias.model.NoticiaPublicada;
import mppb.restapi.mobile.feature.noticias.repository.NoticiaControleRepository;

class NoticiaDeteccaoServiceTest {

    private FakeNoticiaService noticiaService;
    private FakeNoticiaControleRepository repository;
    private NoticiaDeteccaoService service;

    @BeforeEach
    void setUp() {
        noticiaService = new FakeNoticiaService();
        repository = new FakeNoticiaControleRepository();
        service = new NoticiaDeteccaoService(noticiaService, repository);
        ReflectionTestUtils.setField(service, "baselineNotificarRecentesMinutos", 120L);
    }

    @Test
    void primeiraExecucaoRegistraNoticiasAntigasComoBaselineSemGerarPendencia() {
        noticiaService.noticias = List.of(noticiaAntiga("portal:1"), noticiaAntiga("portal:2"));

        int novas = service.detectarNovasNoticias();

        assertEquals(0, novas);
        assertEquals(2, repository.iniciais.size());
        assertEquals(0, repository.novas.size());
    }

    @Test
    void primeiraExecucaoRegistraNoticiasRecentesComoPendencia() {
        noticiaService.noticias = List.of(noticiaRecente("portal:1"), noticiaAntiga("portal:2"));

        int novas = service.detectarNovasNoticias();

        assertEquals(1, novas);
        assertEquals(1, repository.iniciais.size());
        assertEquals(1, repository.novas.size());
    }

    @Test
    void noticiaNovaAposBaselineGeraPendenciaUmaUnicaVez() {
        repository.possuiConhecidas = true;
        noticiaService.noticias = List.of(noticiaAntiga("portal:1"), noticiaAntiga("portal:2"));

        int novas = service.detectarNovasNoticias();
        int repetidas = service.detectarNovasNoticias();

        assertEquals(2, novas);
        assertEquals(0, repetidas);
        assertEquals(2, repository.novas.size());
    }

    private static NoticiaPublicada noticiaAntiga(String chave) {
        return new NoticiaPublicada(
                chave,
                "Titulo",
                "https://www.mppb.mp.br/noticia",
                "Resumo",
                null,
                LocalDateTime.of(2026, 9, 4, 10, 0),
                "04/09/2026 10:00",
                "Cidadania");
    }

    private static NoticiaPublicada noticiaRecente(String chave) {
        return new NoticiaPublicada(
                chave,
                "Titulo",
                "https://www.mppb.mp.br/noticia",
                "Resumo",
                null,
                LocalDateTime.now().minusMinutes(30),
                "14/09/2026 19:50",
                "Cidadania");
    }

    private static class FakeNoticiaService extends NoticiaService {
        private List<NoticiaPublicada> noticias = List.of();

        @Override
        public List<NoticiaPublicada> getUltimasNoticiasPublicadas() {
            return noticias;
        }
    }

    private static class FakeNoticiaControleRepository extends NoticiaControleRepository {
        private boolean possuiConhecidas;
        private final List<NoticiaPublicada> iniciais = new ArrayList<>();
        private final List<NoticiaPublicada> novas = new ArrayList<>();

        FakeNoticiaControleRepository() {
            super(null);
        }

        @Override
        public boolean possuiNoticiasConhecidas() {
            return possuiConhecidas;
        }

        @Override
        public int registrarConhecidasIniciais(List<NoticiaPublicada> noticias) {
            iniciais.addAll(noticias);
            possuiConhecidas = true;
            return noticias.size();
        }

        @Override
        public boolean registrarNova(NoticiaPublicada noticia) {
            possuiConhecidas = true;
            if (novas.stream().anyMatch(row -> row.chaveExterna().equals(noticia.chaveExterna()))) {
                return false;
            }
            novas.add(noticia);
            return true;
        }
    }
}
