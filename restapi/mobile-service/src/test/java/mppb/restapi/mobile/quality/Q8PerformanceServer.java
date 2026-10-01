package mppb.restapi.mobile.quality;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import mppb.restapi.mobile.feature.noticias.dto.NoticiaDto;
import mppb.restapi.mobile.feature.pagamentos.dto.PagamentoItemDto;
import mppb.restapi.mobile.feature.perfil.dto.PerfilUsuarioDto;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Servidor HTTP controlado para testes de performance, carga, stress, spike e soak (Fase Q8).
 * Isola o mobile-service contra chamadas externas em producao e downstream reais.
 */
public class Q8PerformanceServer {

    private final int port;
    private HttpServer server;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicLong requestCounter = new AtomicLong(0);
    private final AtomicLong errorCounter = new AtomicLong(0);
    private ThreadPoolExecutor threadPool;

    public Q8PerformanceServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(200);
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(threadPool);

        // Endpoints simulados com fidelidade contratual
        server.createContext("/api/public/health", new HealthHandler());
        server.createContext("/api/public/noticias", new NoticiasHandler());
        server.createContext("/api/perfil/dados", new PerfilHandler());
        server.createContext("/carteiras-funcionais/dados", new CarteiraHandler());
        server.createContext("/pagamentos/exercicios", new PagamentosExerciciosHandler());
        server.createContext("/pagamentos/2026", new PagamentosDadosHandler());
        server.createContext("/notificacoes/dispositivos", new NotificacaoHandler());
        server.createContext("/metrics", new MetricsHandler());

        server.start();
        System.out.println("Q8PerformanceServer iniciado em http://localhost:" + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
            threadPool.shutdown();
            System.out.println("Q8PerformanceServer encerrado.");
        }
    }

    public static void main(String[] args) throws Exception {
        int p = 8089;
        if (args.length > 0) {
            p = Integer.parseInt(args[0]);
        }
        Q8PerformanceServer s = new Q8PerformanceServer(p);
        s.start();

        Runtime.getRuntime().addShutdownHook(new Thread(s::stop));

        // Mantem vivo ate sinal de interrupcao
        Thread.currentThread().join();
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object body) throws IOException {
        requestCounter.incrementAndGet();
        if (statusCode >= 400) {
            errorCounter.incrementAndGet();
        }
        byte[] bytes = objectMapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("error", "Method Not Allowed"));
                return;
            }
            sendJsonResponse(exchange, 200, Map.of(
                    "status", "UP",
                    "timestamp", Instant.now().toString()
            ));
        }
    }

    private class NoticiasHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("error", "Method Not Allowed"));
                return;
            }
            List<NoticiaDto> noticias = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                noticias.add(NoticiaDto.builder()
                        .chaveExterna("noticia-" + i)
                        .titulo("Notícia Institucional MPPB " + i + " para Teste de Carga")
                        .link("https://www.mppb.mp.br/noticia/" + i)
                        .resumo("Resumo da notícia para benchmark e medição de alocação de heap número " + i)
                        .dataPublicacao("30/09/2026 12:00")
                        .categoria("Institucional")
                        .imagemUrl("https://www.mppb.mp.br/images/noticia-" + i + ".jpg")
                        .build());
            }
            sendJsonResponse(exchange, 200, noticias);
        }
    }

    private class PerfilHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("error", "Method Not Allowed"));
                return;
            }
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                sendJsonResponse(exchange, 401, Map.of("error", "Unauthorized", "message", "Token ausente ou invalido"));
                return;
            }
            PerfilUsuarioDto dto = new PerfilUsuarioDto();
            dto.setLogin("servidor.teste");
            dto.setNome("Servidor Benchmark MPPB");
            dto.setMatricula("1234567");
            dto.setCargo("Analista Ministerial");
            dto.setTipoCargo("Efetivo");
            dto.setEmail("servidor.teste@mppb.mp.br");
            dto.setSetorNome("Diretoria de Tecnologia da Informação");
            dto.setSetorSigla("DTI");
            dto.setOrgaoNome("Ministério Público da Paraíba");
            dto.setOrgaoSigla("MPPB");
            sendJsonResponse(exchange, 200, dto);
        }
    }

    private class CarteiraHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("error", "Method Not Allowed"));
                return;
            }
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                sendJsonResponse(exchange, 401, Map.of("error", "Unauthorized"));
                return;
            }
            Map<String, Object> carteira = Map.of(
                    "matricula", "1234567",
                    "nome", "Servidor Benchmark MPPB",
                    "cargo", "Analista Ministerial",
                    "expedicao", "2026-01-15",
                    "validade", "2030-01-15",
                    "qrCode", "https://valida.mppb.mp.br/carteira/1234567/hash-validacao-benchmark"
            );
            sendJsonResponse(exchange, 200, carteira);
        }
    }

    private class PagamentosExerciciosHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendJsonResponse(exchange, 200, List.of(2026, 2025, 2024));
        }
    }

    private class PagamentosDadosHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<PagamentoItemDto> itens = new ArrayList<>();
            for (int m = 1; m <= 12; m++) {
                PagamentoItemDto item = new PagamentoItemDto();
                item.setNuExercicio("2026");
                item.setNuProcesso(String.format("PROC-2026-%02d", m));
                item.setVlBruto(new BigDecimal("15450.00"));
                item.setVlLiquido(new BigDecimal("12250.00"));
                itens.add(item);
            }
            sendJsonResponse(exchange, 200, Map.of(
                    "exercicio", 2026,
                    "itens", itens
            ));
        }
    }

    private class NotificacaoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("error", "Method Not Allowed"));
                return;
            }
            sendJsonResponse(exchange, 200, Map.of(
                    "identificador", UUID.randomUUID().toString(),
                    "plataforma", "ANDROID",
                    "status", "REGISTRADO",
                    "timestamp", Instant.now().toString()
            ));
        }
    }

    private class MetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
            long heapMax = memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024);
            long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed() / (1024 * 1024);

            Map<String, Object> metrics = new HashMap<>();
            metrics.put("heapUsedMb", heapUsed);
            metrics.put("heapMaxMb", heapMax);
            metrics.put("nonHeapUsedMb", nonHeapUsed);
            metrics.put("activeThreads", threadPool != null ? threadPool.getActiveCount() : 0);
            metrics.put("poolSize", threadPool != null ? threadPool.getPoolSize() : 0);
            metrics.put("totalThreadsJvm", threadBean.getThreadCount());
            metrics.put("peakThreadsJvm", threadBean.getPeakThreadCount());
            metrics.put("totalRequestsServed", requestCounter.get());
            metrics.put("totalErrors", errorCounter.get());

            sendJsonResponse(exchange, 200, metrics);
        }
    }
}
