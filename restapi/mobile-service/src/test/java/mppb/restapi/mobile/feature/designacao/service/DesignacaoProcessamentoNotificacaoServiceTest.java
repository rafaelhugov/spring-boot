package mppb.restapi.mobile.feature.designacao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import mppb.restapi.mobile.feature.designacao.model.DesignacaoAtivaRow;
import mppb.restapi.mobile.feature.designacao.model.DesignacaoControlePendenteNotificacao;
import mppb.restapi.mobile.feature.designacao.model.ResultadoNotificacaoDesignacao;
import mppb.restapi.mobile.feature.designacao.model.ResultadoProcessamentoNotificacaoDesignacao;
import mppb.restapi.mobile.feature.designacao.repository.DesignacaoControleRepository;
import mppb.restapi.mobile.feature.notificacao.model.PreferenciasNotificacao;
import mppb.restapi.mobile.feature.notificacao.repository.PreferenciasNotificacaoRepository;
import mppb.restapi.mobile.feature.notificacao.service.PreferenciasNotificacaoService;

class DesignacaoProcessamentoNotificacaoServiceTest {

    private FakeDesignacaoControleRepository repository;
    private FakeDesignacaoNotificacaoService notificacaoService;
    private DesignacaoProcessamentoNotificacaoService service;

    @BeforeEach
    void setUp() {
        repository = new FakeDesignacaoControleRepository();
        notificacaoService = new FakeDesignacaoNotificacaoService();
        service = new DesignacaoProcessamentoNotificacaoService(
                repository,
                notificacaoService,
                preferenciasService(true));
        ReflectionTestUtils.setField(service, "batchSize", 100);
        ReflectionTestUtils.setField(service, "maxTentativas", 3);
    }

    @Test
    void peParaEnQuandoHaEnvioAceito() {
        repository.add(pendente(1L, "PE", 0));
        notificacaoService.resultados.put(1L, resultado(1, 1, 0, 0, 0, 0, false));

        List<ResultadoProcessamentoNotificacaoDesignacao> resultados = service.processarPendentes();

        assertEquals(1, resultados.size());
        assertEquals("EN", resultados.get(0).statusFinal());
        assertNull(resultados.get(0).mensagemErro());
        assertEquals(1, repository.enviados.size());
        assertEquals(0, repository.erros.size());
        assertTrue(repository.rows.get(1L).reservaEncerrada());
    }

    @Test
    void encerraSemEnviarQuandoUsuarioDesabilitouDesignacoes() {
        repository.add(pendente(1L, "PE", 0));
        service = new DesignacaoProcessamentoNotificacaoService(
                repository,
                notificacaoService,
                preferenciasService(false));
        ReflectionTestUtils.setField(service, "batchSize", 100);
        ReflectionTestUtils.setField(service, "maxTentativas", 3);

        ResultadoProcessamentoNotificacaoDesignacao resultado = service.processarPendentes().get(0);

        assertEquals("EN", resultado.statusFinal());
        assertTrue(notificacaoService.designacoes.isEmpty());
        assertEquals(1, repository.enviados.size());
        assertEquals(0, repository.erros.size());
    }

    private static PreferenciasNotificacaoService preferenciasService(boolean designacoes) {
        return new PreferenciasNotificacaoService(new FakePreferenciasRepository(designacoes));
    }

    @Test
    void processamentoPublicoNaoMantemTransacaoDuranteEnvioFcm() throws Exception {
        assertNull(DesignacaoProcessamentoNotificacaoService.class
                .getMethod("processarPendentes")
                .getAnnotation(Transactional.class));
    }

    @Test
    void peParaErQuandoNaoHaEnvioAceito() {
        repository.add(pendente(1L, "PE", 0));
        notificacaoService.resultados.put(1L, resultado(1, 0, 0, 1, 0, 0, false));

        List<ResultadoProcessamentoNotificacaoDesignacao> resultados = service.processarPendentes();

        assertEquals("ER", resultados.get(0).statusFinal());
        assertEquals("FALHA_TRANSITORIA_FCM", resultados.get(0).mensagemErro());
        assertEquals(1, repository.erros.get(1L).tentativas());
        assertTrue(repository.rows.get(1L).reservaEncerrada());
    }

    @Test
    void erParaEnQuandoNovaTentativaTemSucesso() {
        repository.add(pendente(1L, "ER", 1));
        notificacaoService.resultados.put(1L, resultado(2, 1, 0, 1, 0, 0, false));

        ResultadoProcessamentoNotificacaoDesignacao resultado = service.processarPendentes().get(0);

        assertEquals("ER", resultado.statusAnterior());
        assertEquals("EN", resultado.statusFinal());
        assertEquals(2, repository.enviados.get(1L).tentativas());
        assertFalse(repository.erros.containsKey(1L));
    }

    @Test
    void erParaErQuandoNovaTentativaFalha() {
        repository.add(pendente(1L, "ER", 1));
        notificacaoService.resultados.put(1L, resultado(1, 0, 0, 0, 1, 0, false));

        ResultadoProcessamentoNotificacaoDesignacao resultado = service.processarPendentes().get(0);

        assertEquals("ER", resultado.statusAnterior());
        assertEquals("ER", resultado.statusFinal());
        assertEquals(2, repository.erros.get(1L).tentativas());
        assertEquals("FALHA_CONFIGURACAO_FCM", repository.erros.get(1L).mensagem());
    }

    @Test
    void limiteMaximoAtingidoNaoEProcessado() {
        repository.add(pendente(1L, "ER", 3));

        List<ResultadoProcessamentoNotificacaoDesignacao> resultados = service.processarPendentes();

        assertTrue(resultados.isEmpty());
        assertTrue(notificacaoService.designacoes.isEmpty());
        assertEquals(3, repository.maxTentativasRecebido);
    }

    @Test
    void enNaoEProcessadoNovamente() {
        repository.add(pendente(1L, "EN", 1));

        List<ResultadoProcessamentoNotificacaoDesignacao> resultados = service.processarPendentes();

        assertTrue(resultados.isEmpty());
        assertTrue(notificacaoService.designacoes.isEmpty());
    }

    @Test
    void ausenciaDeDispositivosFicaControladaComoErroSemFalhaFcm() {
        repository.add(pendente(1L, "PE", 0));
        notificacaoService.resultados.put(1L, ResultadoNotificacaoDesignacao.semDispositivos());

        ResultadoProcessamentoNotificacaoDesignacao resultado = service.processarPendentes().get(0);

        assertEquals("ER", resultado.statusFinal());
        assertEquals("SEM_DISPOSITIVO_ATIVO", resultado.mensagemErro());
    }

    @Test
    void todosTokensInvalidosFicamComoErroElegivel() {
        repository.add(pendente(1L, "PE", 0));
        notificacaoService.resultados.put(1L, resultado(2, 0, 2, 0, 0, 0, false));

        ResultadoProcessamentoNotificacaoDesignacao resultado = service.processarPendentes().get(0);

        assertEquals("ER", resultado.statusFinal());
        assertEquals("TOKENS_INVALIDOS", resultado.mensagemErro());
    }

    @Test
    void falhaTransitoriaMantemErroParaRetentativa() {
        repository.add(pendente(1L, "PE", 0));
        notificacaoService.resultados.put(1L, resultado(1, 0, 0, 1, 0, 0, false));

        service.processarPendentes();

        assertEquals("FALHA_TRANSITORIA_FCM", repository.erros.get(1L).mensagem());
        assertEquals(1, repository.erros.get(1L).tentativas());
    }

    @Test
    void falhaConfiguracaoMantemErroParaRetentativa() {
        repository.add(pendente(1L, "PE", 0));
        notificacaoService.resultados.put(1L, resultado(1, 0, 0, 0, 1, 0, false));

        service.processarPendentes();

        assertEquals("FALHA_CONFIGURACAO_FCM", repository.erros.get(1L).mensagem());
    }

    @Test
    void mensagemDeErroESanitizadaSemDadosSensiveis() {
        repository.add(pendente(1L, "PE", 0));
        notificacaoService.resultados.put(1L, resultado(1, 0, 0, 0, 0, 1, false));

        service.processarPendentes();

        String mensagem = repository.erros.get(1L).mensagem();
        assertEquals("FALHA_DESCONHECIDA_FCM", mensagem);
        assertFalse(mensagem.contains("token"));
        assertFalse(mensagem.contains("12345"));
        assertFalse(mensagem.contains("Promotoria"));
    }

    @Test
    void processamentoRespeitaBatchConfigurado() {
        ReflectionTestUtils.setField(service, "batchSize", 2);
        repository.add(pendente(1L, "PE", 0));
        repository.add(pendente(2L, "PE", 0));
        repository.add(pendente(3L, "PE", 0));

        List<ResultadoProcessamentoNotificacaoDesignacao> resultados = service.processarPendentes();

        assertEquals(2, resultados.size());
        assertEquals(2, notificacaoService.designacoes.size());
        assertEquals(2, repository.batchSizeRecebido);
    }

    @Test
    void controlesDuplicadosNaoSaoProcessadosNoMesmoLote() {
        repository.add(pendente(1L, "PE", 0));
        repository.add(pendente(1L, "PE", 0));

        List<ResultadoProcessamentoNotificacaoDesignacao> resultados = service.processarPendentes();

        assertEquals(1, resultados.size());
        assertEquals(1, notificacaoService.designacoes.size());
    }

    @Test
    void duasExecucoesConcorrentesNaoReservamOMesmoRegistro() {
        repository.add(pendente(1L, "PE", 0));
        repository.add(pendente(2L, "PE", 0));
        repository.manterReservaAposPrimeiraBusca = true;

        List<ResultadoProcessamentoNotificacaoDesignacao> primeira = service.processarPendentes();
        List<ResultadoProcessamentoNotificacaoDesignacao> segunda = service.processarPendentes();

        assertEquals(2, primeira.size());
        assertTrue(segunda.isEmpty());
        assertEquals(2, notificacaoService.designacoes.size());
    }

    @Test
    void chamadaFcmNaoOcorreDuranteReservaCurta() {
        repository.add(pendente(1L, "PE", 0));
        notificacaoService.repository = repository;

        service.processarPendentes();

        assertFalse(notificacaoService.envioDuranteReserva);
        assertEquals(1, notificacaoService.designacoes.size());
    }

    @Test
    void finalizacaoIgnoraReservaDiferente() {
        DesignacaoControlePendenteNotificacao pendente = pendente(1L, "PE", 0);
        repository.add(pendente);

        assertFalse(repository.marcarNotificacaoEnviada(1L, "reserva-incorreta"));
        assertFalse(repository.marcarNotificacaoErro(1L, "reserva-incorreta", "FALHA_TRANSITORIA_FCM"));
    }

    private static ResultadoNotificacaoDesignacao resultado(
            int dispositivosAtivos,
            int aceitos,
            int tokensInvalidos,
            int falhasTransitorias,
            int falhasConfiguracao,
            int falhasDesconhecidas,
            boolean fcmDesligado) {
        return new ResultadoNotificacaoDesignacao(
                dispositivosAtivos,
                aceitos,
                tokensInvalidos,
                falhasTransitorias,
                falhasConfiguracao,
                falhasDesconhecidas,
                fcmDesligado);
    }

    private static DesignacaoControlePendenteNotificacao pendente(Long id, String status, int tentativas) {
        return new DesignacaoControlePendenteNotificacao(
                id,
                null,
                "12345",
                status,
                tentativas,
                new DesignacaoAtivaRow(
                        id,
                        "12345",
                        "F",
                        "Função",
                        "Cargo sensivel",
                        "Promotoria de Justiça",
                        "Comarca sensivel",
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 30),
                        LocalDate.of(2026, 8, 30),
                        "ATO-123",
                        false,
                        "S",
                        "Servidor"));
    }

    private static class FakeDesignacaoControleRepository extends DesignacaoControleRepository {
        private final Map<Long, FakeControleRow> rows = new LinkedHashMap<>();
        private final Map<Long, Atualizacao> enviados = new LinkedHashMap<>();
        private final Map<Long, Atualizacao> erros = new LinkedHashMap<>();
        private int batchSizeRecebido;
        private int maxTentativasRecebido;
        private boolean reservando;
        private boolean manterReservaAposPrimeiraBusca;

        FakeDesignacaoControleRepository() {
            super(null);
        }

        void add(DesignacaoControlePendenteNotificacao pendente) {
            rows.putIfAbsent(pendente.id(), new FakeControleRow(pendente));
        }

        @Override
        public List<DesignacaoControlePendenteNotificacao> reservarPendentesParaNotificacao(
                int batchSize,
                int maxTentativas,
                String identificadorReserva) {
            batchSizeRecebido = batchSize;
            maxTentativasRecebido = maxTentativas;
            reservando = true;
            List<DesignacaoControlePendenteNotificacao> reservadas = rows.values().stream()
                    .filter(row -> !row.reservado())
                    .filter(row -> "PE".equals(row.pendente.statusNotificacao()) || "ER".equals(row.pendente.statusNotificacao()))
                    .filter(row -> row.pendente.quantidadeTentativas() < maxTentativas)
                    .limit(batchSize)
                    .map(row -> row.reservar(identificadorReserva))
                    .toList();
            reservando = false;
            return reservadas;
        }

        @Override
        public boolean marcarNotificacaoEnviada(Long id, String identificadorReserva) {
            FakeControleRow row = rows.get(id);
            if (row == null || !row.reservaConfere(identificadorReserva)) {
                return false;
            }
            enviados.put(id, new Atualizacao("EN", row.pendente.quantidadeTentativas() + 1, null));
            row.finalizar("EN", row.pendente.quantidadeTentativas() + 1);
            return true;
        }

        @Override
        public boolean marcarNotificacaoErro(Long id, String identificadorReserva, String mensagemErro) {
            FakeControleRow row = rows.get(id);
            if (row == null || !row.reservaConfere(identificadorReserva)) {
                return false;
            }
            erros.put(id, new Atualizacao("ER", row.pendente.quantidadeTentativas() + 1, mensagemErro));
            row.finalizar("ER", row.pendente.quantidadeTentativas() + 1);
            return true;
        }

        private class FakeControleRow {
            private DesignacaoControlePendenteNotificacao pendente;
            private String reserva;

            private FakeControleRow(DesignacaoControlePendenteNotificacao pendente) {
                this.pendente = pendente;
            }

            private boolean reservado() {
                return reserva != null;
            }

            private boolean reservaEncerrada() {
                return reserva == null;
            }

            private DesignacaoControlePendenteNotificacao reservar(String identificadorReserva) {
                reserva = identificadorReserva;
                pendente = new DesignacaoControlePendenteNotificacao(
                        pendente.id(),
                        identificadorReserva,
                        pendente.servidorMatricula(),
                        pendente.statusNotificacao(),
                        pendente.quantidadeTentativas(),
                        pendente.designacao());
                return pendente;
            }

            private boolean reservaConfere(String identificadorReserva) {
                return reserva != null && reserva.equals(identificadorReserva);
            }

            private void finalizar(String status, int tentativas) {
                if (!manterReservaAposPrimeiraBusca) {
                    reserva = null;
                }
                pendente = new DesignacaoControlePendenteNotificacao(
                        pendente.id(),
                        reserva,
                        pendente.servidorMatricula(),
                        status,
                        tentativas,
                        pendente.designacao());
            }
        }
    }

    private static class FakeDesignacaoNotificacaoService extends DesignacaoNotificacaoService {
        private final Map<Long, ResultadoNotificacaoDesignacao> resultados = new LinkedHashMap<>();
        private final List<DesignacaoAtivaRow> designacoes = new ArrayList<>();
        private FakeDesignacaoControleRepository repository;
        private boolean envioDuranteReserva;

        FakeDesignacaoNotificacaoService() {
            super(null, null);
        }

        @Override
        public ResultadoNotificacaoDesignacao notificarNovaDesignacao(DesignacaoAtivaRow designacao) {
            if (repository != null && repository.reservando) {
                envioDuranteReserva = true;
            }
            designacoes.add(designacao);
            return resultados.getOrDefault(designacao.id(), resultado(1, 1, 0, 0, 0, 0, false));
        }
    }

    private static class FakePreferenciasRepository extends PreferenciasNotificacaoRepository {
        private final boolean designacoes;

        FakePreferenciasRepository(boolean designacoes) {
            super(null);
            this.designacoes = designacoes;
        }

        @Override
        public Optional<PreferenciasNotificacao> buscar(String matricula) {
            return Optional.of(new PreferenciasNotificacao(true, designacoes, true, true, true));
        }
    }

    private record Atualizacao(String status, int tentativas, String mensagem) {
    }
}
