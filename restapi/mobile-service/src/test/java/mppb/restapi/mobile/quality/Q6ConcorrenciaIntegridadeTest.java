package mppb.restapi.mobile.quality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import mppb.restapi.mobile.feature.notificacao.dto.DispositivoNotificacaoResponse;
import mppb.restapi.mobile.feature.notificacao.dto.RegistrarDispositivoRequest;
import mppb.restapi.mobile.feature.notificacao.model.DispositivoFcm;
import mppb.restapi.mobile.feature.notificacao.repository.DispositivoFcmRepository;
import mppb.restapi.mobile.feature.notificacao.service.DispositivoFcmService;
import mppb.restapi.mobile.feature.pagamentos.dto.PagamentoItemDto;
import mppb.restapi.mobile.feature.pagamentos.dto.PagamentosResponseDto;
import mppb.restapi.mobile.feature.pagamentos.repository.PagamentosRepository;
import mppb.restapi.mobile.feature.pagamentos.service.PagamentosService;
import mppb.restapi.mobile.feature.perfil.dto.PerfilUsuarioDto;
import mppb.restapi.mobile.feature.perfil.repository.PerfilUsuarioRepository;
import mppb.restapi.mobile.feature.perfil.service.PerfilUsuarioService;

class Q6ConcorrenciaIntegridadeTest {

    private InMemoryDispositivoFcmRepository fcmRepository;
    private DispositivoFcmService fcmService;

    private InMemoryPerfilRepository perfilRepository;
    private PerfilUsuarioService perfilService;

    private InMemoryPagamentosRepository pagamentosRepository;
    private PagamentosService pagamentosService;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        fcmRepository = new InMemoryDispositivoFcmRepository();
        fcmService = new DispositivoFcmService(fcmRepository);

        perfilRepository = new InMemoryPerfilRepository();
        perfilService = new PerfilUsuarioService(new FakeAuthServiceClient(), perfilRepository);

        pagamentosRepository = new InMemoryPagamentosRepository();
        pagamentosService = new PagamentosService(pagamentosRepository);

        executor = Executors.newFixedThreadPool(8);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        SecurityContextHolder.clearContext();
        if (executor != null) {
            executor.shutdown();
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }
    }

    // =========================================================================
    // Q6-T08 / Q6-T09 / Q6-T11: Requisições Duplicadas / Concorrentes de FCM
    // =========================================================================

    @Test
    @DisplayName("Q6-T08/T09: Chamadas simultaneas de registro com mesmo identificador e token convergem sem duplicidade")
    void testRegistroDispositivoConcorrenteMesmoTokenReutilizaRegistro() throws Exception {
        String matricula = "99001";
        String tokenFcm = "fcm-token-concorrente-123456789";
        String identificador = "device-uuid-fixed-001";
        RegistrarDispositivoRequest request = new RegistrarDispositivoRequest(identificador, tokenFcm, "ANDROID");

        int totalThreads = 4;
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(totalThreads);
        List<DispositivoNotificacaoResponse> responses = Collections.synchronizedList(new ArrayList<>());
        List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < totalThreads; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    DispositivoNotificacaoResponse res = fcmService.registrar(matricula, request);
                    responses.add(res);
                } catch (Throwable t) {
                    exceptions.add(t);
                } finally {
                    endGate.countDown();
                }
            });
        }

        // Libera todas as threads simultaneamente
        startGate.countDown();
        boolean completed = endGate.await(5, TimeUnit.SECONDS);

        assertThat(completed).isTrue();
        assertThat(exceptions).isEmpty();
        assertThat(responses).hasSize(totalThreads);

        // Todas as chamadas devem retornar o mesmo identificador
        for (DispositivoNotificacaoResponse resp : responses) {
            assertThat(resp.identificador()).isEqualTo(identificador);
            assertThat(resp.plataforma()).isEqualTo("ANDROID");
            assertThat(resp.ativo()).isTrue();
        }

        // O repositório subjacente deve conter apenas 1 registro único ativo (idempotência)
        List<DispositivoFcm> registros = fcmRepository.findAtivosByMatricula(matricula);
        assertThat(registros).hasSize(1);
        assertThat(registros.get(0).identificador()).isEqualTo(identificador);
    }

    // =========================================================================
    // Q6-T11: FCM Idempotência em Desativações Repetidas
    // =========================================================================

    @Test
    @DisplayName("Q6-T11: Desativacao repetida do mesmo dispositivo e segura e idempotente")
    void testDesativacaoDispositivoRepetidaEIdempotente() {
        String matricula = "99002";
        String identificador = "device-uuid-to-deactivate";
        fcmService.registrar(matricula, new RegistrarDispositivoRequest(identificador, "token-desativar-xyz", "ANDROID"));

        // Primeira desativação: deve desativar com sucesso
        boolean desativouPrimeira = fcmService.desativar(matricula, identificador);
        assertThat(desativouPrimeira).isTrue();

        // Segunda desativação idêntica subsequente: não lança erro, retorna false (já inativo)
        boolean desativouSegunda = assertDoesNotThrow(() -> fcmService.desativar(matricula, identificador));
        assertThat(desativouSegunda).isFalse();

        // Estado final permanece íntegro e inativo
        List<DispositivoFcm> ativos = fcmRepository.findAtivosByMatricula(matricula);
        assertThat(ativos).isEmpty();
    }

    // =========================================================================
    // Q6-T14: Transações Backend / Simulação de Falha e Integridade
    // =========================================================================

    @Test
    @DisplayName("Q6-T14: Validacao e protecao transacional impedem persistencia de dados invalidos")
    void testTransacaoValidacaoImpedePersistenciaInvalida() {
        String matricula = "99003";

        // Requisição inválida (plataforma desconhecida / token em branco)
        RegistrarDispositivoRequest requestInvalido = new RegistrarDispositivoRequest("dev-inv", "", "WINDOWS_PHONE");

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
            fcmService.registrar(matricula, requestInvalido);
        });

        // Nenhum registro corrompido foi inserido
        assertThat(fcmRepository.findAtivosByMatricula(matricula)).isEmpty();
    }

    // =========================================================================
    // Q6-T15: Constraints e Unicidade por Token Hash
    // =========================================================================

    @Test
    @DisplayName("Q6-T15: Token FCM registrado novamente por outro identificador reativa e atualiza sem violar unicidade")
    void testConstraintUnicidadeTokenFcmAtualizaSemViolacao() {
        String matricula = "99004";
        String token = "token-compartilhado-456";

        // Registro original
        fcmService.registrar(matricula, new RegistrarDispositivoRequest("identificador-antigo", token, "ANDROID"));

        // Novo registro com mesmo token mas novo identificador (ex: reinstalação)
        DispositivoNotificacaoResponse novo = fcmService.registrar(matricula,
                new RegistrarDispositivoRequest("identificador-novo", token, "ANDROID"));

        assertThat(novo.ativo()).isTrue();
        // O repositório não duplicou por token
        assertThat(fcmRepository.findAtivosByMatricula(matricula)).hasSize(1);
    }

    // =========================================================================
    // Q6-T16: Concorrência de Leitura sem Contaminação de Estado
    // =========================================================================

    @Test
    @DisplayName("Q6-T16: Consultas concorrentes de perfil para usuarios distintos nao cruzam dados")
    void testConcorrenciaLeituraSimultaneaPerfilUsuariosDistintos() throws Exception {
        int totalRequests = 10;
        List<CompletableFuture<PerfilUsuarioDto>> futures = new ArrayList<>();

        for (int i = 0; i < totalRequests; i++) {
            final String login = "promotor." + i;
            final String matricula = "MAT-" + (1000 + i);
            perfilRepository.cadastrar(login, "Promotor " + i, matricula);

            final String token = login;
            futures.add(CompletableFuture.supplyAsync(() -> perfilService.buscarPerfilAutenticado(token), executor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        for (int i = 0; i < totalRequests; i++) {
            PerfilUsuarioDto dto = futures.get(i).get();
            assertThat(dto.getLogin()).isEqualTo("promotor." + i);
            assertThat(dto.getNome()).isEqualTo("Promotor " + i);
            assertThat(dto.getMatricula()).isEqualTo("MAT-" + (1000 + i));
        }
    }

    @Test
    @DisplayName("Q6-T16: Consultas concorrentes de pagamentos nao misturam exercicios nem CPFs")
    void testConcorrenciaLeituraSimultaneaPagamentos() throws Exception {
        pagamentosRepository.cadastrarLoginCpf("user.a", "111.111.111-11");
        pagamentosRepository.cadastrarLoginCpf("user.b", "222.222.222-22");

        pagamentosRepository.cadastrarPagamento("111.111.111-11", "2024", "PROC-A", new BigDecimal("1500.00"));
        pagamentosRepository.cadastrarPagamento("222.222.222-22", "2025", "PROC-B", new BigDecimal("2500.00"));

        CompletableFuture<PagamentosResponseDto> futureA = CompletableFuture.supplyAsync(
                () -> pagamentosService.listarPagamentos("2024", "111.111.111-11"), executor);

        CompletableFuture<PagamentosResponseDto> futureB = CompletableFuture.supplyAsync(
                () -> pagamentosService.listarPagamentos("2025", "222.222.222-22"), executor);

        CompletableFuture.allOf(futureA, futureB).join();

        PagamentosResponseDto resA = futureA.get();
        PagamentosResponseDto resB = futureB.get();

        assertThat(resA.getTotalBruto()).isEqualByComparingTo("1500.00");
        assertThat(resA.getPagamentos()).hasSize(1);
        assertThat(resA.getPagamentos().get(0).getNuProcesso()).isEqualTo("PROC-A");

        assertThat(resB.getTotalBruto()).isEqualByComparingTo("2500.00");
        assertThat(resB.getPagamentos()).hasSize(1);
        assertThat(resB.getPagamentos().get(0).getNuProcesso()).isEqualTo("PROC-B");
    }

    // =========================================================================
    // Repositórios Fakes Thread-Safe para Testes Determinísticos
    // =========================================================================

    private static class InMemoryDispositivoFcmRepository extends DispositivoFcmRepository {

        private final Map<Long, DispositivoFcm> store = new ConcurrentHashMap<>();
        private final AtomicLong idGenerator = new AtomicLong(1);

        InMemoryDispositivoFcmRepository() {
            super(null);
        }

        @Override
        public synchronized Optional<DispositivoFcm> findByTokenHash(String tokenFcmHash) {
            return store.values().stream()
                    .filter(d -> tokenFcmHash.equals(d.tokenFcmHash()))
                    .findFirst();
        }

        @Override
        public synchronized Optional<DispositivoFcm> findByIdentificador(String identificador) {
            return store.values().stream()
                    .filter(d -> identificador.equals(d.identificador()))
                    .findFirst();
        }

        @Override
        public synchronized List<DispositivoFcm> findAtivosByMatricula(String matricula) {
            return store.values().stream()
                    .filter(d -> matricula.equals(d.servidorMatricula()) && Boolean.TRUE.equals(d.ativo()))
                    .toList();
        }

        @Override
        public synchronized DispositivoFcm inserir(
                String identificador,
                String matricula,
                String tokenFcm,
                String tokenFcmHash,
                String plataforma) {
            long id = idGenerator.getAndIncrement();
            LocalDateTime agora = LocalDateTime.now();
            DispositivoFcm item = new DispositivoFcm(id, identificador, matricula, tokenFcm, tokenFcmHash, plataforma,
                    true, agora, agora, agora);
            store.put(id, item);
            return item;
        }

        @Override
        public synchronized DispositivoFcm atualizar(
                Long id,
                String matricula,
                String tokenFcm,
                String tokenFcmHash,
                String plataforma) {
            DispositivoFcm anterior = store.get(id);
            LocalDateTime agora = LocalDateTime.now();
            DispositivoFcm atualizado = new DispositivoFcm(
                    id,
                    anterior != null ? anterior.identificador() : "id-" + id,
                    matricula,
                    tokenFcm,
                    tokenFcmHash,
                    plataforma,
                    true,
                    anterior != null ? anterior.dataCriacao() : agora,
                    agora,
                    agora);
            store.put(id, atualizado);
            return atualizado;
        }

        @Override
        public synchronized boolean desativar(String matricula, String identificador) {
            Optional<DispositivoFcm> opt = store.values().stream()
                    .filter(d -> matricula.equals(d.servidorMatricula()) && identificador.equals(d.identificador())
                            && Boolean.TRUE.equals(d.ativo()))
                    .findFirst();
            if (opt.isPresent()) {
                DispositivoFcm item = opt.get();
                LocalDateTime agora = LocalDateTime.now();
                DispositivoFcm inativo = new DispositivoFcm(
                        item.id(), item.identificador(), item.servidorMatricula(), item.tokenFcm(), item.tokenFcmHash(),
                        item.plataforma(), false, item.dataCriacao(), agora, item.dataUltimoUso());
                store.put(item.id(), inativo);
                return true;
            }
            return false;
        }
    }

    private static class InMemoryPerfilRepository extends PerfilUsuarioRepository {

        private final Map<String, PerfilUsuarioDto> perfis = new ConcurrentHashMap<>();

        InMemoryPerfilRepository() {
            super(null);
        }

        void cadastrar(String login, String nome, String matricula) {
            PerfilUsuarioDto dto = new PerfilUsuarioDto();
            dto.setLogin(login);
            dto.setNome(nome);
            dto.setMatricula(matricula);
            dto.setSituacao("Ativo");
            perfis.put(login, dto);
        }

        @Override
        public PerfilUsuarioDto buscarPorLogin(String login) {
            return perfis.get(login);
        }
    }

    private static class InMemoryPagamentosRepository extends PagamentosRepository {

        private final Map<String, String> loginCpf = new ConcurrentHashMap<>();
        private final Map<String, List<PagamentoItemDto>> pagamentos = new ConcurrentHashMap<>();

        InMemoryPagamentosRepository() {
            super(null);
        }

        void cadastrarLoginCpf(String login, String cpf) {
            loginCpf.put(login, cpf);
        }

        void cadastrarPagamento(String cpf, String exercicio, String processo, BigDecimal valor) {
            String key = cpf + ":" + exercicio;
            PagamentoItemDto item = new PagamentoItemDto();
            item.setNuExercicio(exercicio);
            item.setNuProcesso(processo);
            item.setVlBruto(valor);
            item.setVlLiquido(valor.multiply(new BigDecimal("0.85")));
            pagamentos.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }

        @Override
        public String getCpfByLogin(String login) {
            return loginCpf.get(login);
        }

        @Override
        public List<Integer> findAnosComPagamentos(String cpf) {
            return List.of(2024, 2025);
        }

        @Override
        public List<PagamentoItemDto> findPagamentosByExercicioAndCpf(String exercicio, String cpf) {
            return pagamentos.getOrDefault(cpf + ":" + exercicio, List.of());
        }
    }

    private static class FakeAuthServiceClient implements mppb.restapi.common.infrastructure.client.authapi.AuthServiceClient {
        @Override
        public mppb.restapi.common.infrastructure.client.authapi.dto.AutorizaResponseDto autoriza(
                mppb.restapi.common.infrastructure.client.authapi.dto.AutorizaRequestDto autorizaRequestTO) {
            return null;
        }

        @Override
        public mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoResponseDto consultaAutorizacao(
                mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoRequestDto req) {
            String login = req.getToken();
            return new mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoResponseDto(
                    new mppb.restapi.common.infrastructure.client.authapi.dto.AcessoDataResponseDto(List.of(), List.of(), List.of(), List.of()),
                    new mppb.restapi.common.infrastructure.client.authapi.dto.UsuarioAutorizadoResponseDto(1L, "Nome " + login, login));
        }
    }
}
