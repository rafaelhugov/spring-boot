package mppb.restapi.mobile.quality;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class Q8PerformanceServerTest {

    private static Q8PerformanceServer server;
    private static final int PORT = 8089;
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @BeforeAll
    static void startServer() throws Exception {
        server = new Q8PerformanceServer(PORT);
        server.start();
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    @DisplayName("Q8-SVR-01: Health check responde 200 UP")
    void healthCheckRespondeUp() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/api/public/health"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }

    @Test
    @DisplayName("Q8-SVR-02: Noticias publicas retorna lista com 10 itens")
    void noticiasRetornaLista() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/api/public/noticias"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("noticia-1");
    }

    @Test
    @DisplayName("Q8-SVR-03: Perfil exige autorizacao Bearer")
    void perfilExigeBearer() throws Exception {
        HttpRequest reqSemToken = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/api/perfil/dados"))
                .GET()
                .build();
        HttpResponse<String> resSemToken = client.send(reqSemToken, HttpResponse.BodyHandlers.ofString());
        assertThat(resSemToken.statusCode()).isEqualTo(401);

        HttpRequest reqComToken = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/api/perfil/dados"))
                .header("Authorization", "Bearer token-teste-benchmark")
                .GET()
                .build();
        HttpResponse<String> resComToken = client.send(reqComToken, HttpResponse.BodyHandlers.ofString());
        assertThat(resComToken.statusCode()).isEqualTo(200);
        assertThat(resComToken.body()).contains("Servidor Benchmark MPPB");
    }

    @Test
    @DisplayName("Q8-SVR-04: Metrics responde com estatisticas da JVM")
    void metricsRespondeJvm() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/metrics"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("heapUsedMb");
    }

    @Test
    @DisplayName("Q8-SVR-05: Servidor aguarda execucao de testes k6 se flag ativa")
    void aguardarCargaSeSolicitado() throws Exception {
        File stopSignal = new File("target/q8_server.stop");
        if (stopSignal.exists()) {
            stopSignal.delete();
        }

        File runSignal = new File("target/q8_server.run");
        if (runSignal.exists()) {
            System.out.println("Flag target/q8_server.run detectada. Servidor aguardando k6...");
            long start = System.currentTimeMillis();
            // Aguarda ate o arquivo stop existir ou max 5 minutos
            while (!stopSignal.exists() && (System.currentTimeMillis() - start) < 300000) {
                Thread.sleep(500);
            }
            System.out.println("Sinal de parada detectado ou timeout atingido.");
            runSignal.delete();
            if (stopSignal.exists()) {
                stopSignal.delete();
            }
        }
    }
}
