package mppb.restapi.mobile.feature.processoconsulta.controller;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Limite local e limitado em memoria para os endpoints publicos de processos.
 *
 * <p>O endereco usado e sempre o peer TCP observado pelo servlet. Cabecalhos
 * encaminhados pelo cliente nao sao confiaveis neste nivel e, por isso, nao
 * participam da chave. Se houver proxy reverso, o limite por IP real deve ser
 * aplicado nele, onde a cadeia de proxies confiaveis pode ser validada.</p>
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final long WINDOW_MILLIS = 60_000L;

    private final int searchLimit;
    private final int detailsLimit;
    private final int captchaLimit;
    private final Map<ClientScope, FixedWindow> clients;

    public RateLimitInterceptor(
            @Value("${security.public-api.rate-limit.search-per-minute:10}") int searchLimit,
            @Value("${security.public-api.rate-limit.details-per-minute:10}") int detailsLimit,
            @Value("${security.public-api.rate-limit.captcha-per-minute:20}") int captchaLimit,
            @Value("${security.public-api.rate-limit.max-clients:10000}") int maxClients) {
        this.searchLimit = positive(searchLimit, "search-per-minute");
        this.detailsLimit = positive(detailsLimit, "details-per-minute");
        this.captchaLimit = positive(captchaLimit, "captcha-per-minute");
        int boundedMaxClients = positive(maxClients, "max-clients");
        this.clients = new LinkedHashMap<>(256, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<ClientScope, FixedWindow> eldest) {
                return size() > boundedMaxClients;
            }
        };
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Scope scope = scopeFor(request.getRequestURI());
        if (scope == null) {
            return true;
        }

        String clientAddress = normalizedRemoteAddress(request.getRemoteAddr());
        boolean allowed;
        synchronized (clients) {
            ClientScope key = new ClientScope(clientAddress, scope);
            FixedWindow window = clients.computeIfAbsent(key, ignored -> new FixedWindow());
            allowed = window.tryAcquire(limitFor(scope), System.currentTimeMillis());
        }

        if (allowed) {
            return true;
        }

        response.setStatus(429);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/problem+json");
        response.setHeader("Retry-After", "60");
        response.getWriter().write(
                "{\"status\":429,\"title\":\"Muitas requisições\","
                        + "\"detail\":\"Aguarde um minuto antes de tentar novamente.\"}");
        return false;
    }

    int trackedClients() {
        synchronized (clients) {
            return clients.size();
        }
    }

    private Scope scopeFor(String uri) {
        if ("/api/public/v1/processos/consulta".equals(uri)) {
            return Scope.SEARCH;
        }
        if ("/api/public/v1/captcha".equals(uri)) {
            return Scope.CAPTCHA;
        }
        if (uri != null
                && uri.startsWith("/api/public/v1/processos/")
                && uri.endsWith("/detalhes")) {
            return Scope.DETAILS;
        }
        return null;
    }

    private int limitFor(Scope scope) {
        return switch (scope) {
            case SEARCH -> searchLimit;
            case DETAILS -> detailsLimit;
            case CAPTCHA -> captchaLimit;
        };
    }

    private String normalizedRemoteAddress(String remoteAddress) {
        if (remoteAddress == null || remoteAddress.isBlank()) {
            return "unknown";
        }
        String normalized = remoteAddress.trim();
        return normalized.length() <= 64 ? normalized : normalized.substring(0, 64);
    }

    private static int positive(int value, String property) {
        if (value <= 0) {
            throw new IllegalArgumentException(property + " deve ser maior que zero");
        }
        return value;
    }

    private enum Scope {
        SEARCH,
        DETAILS,
        CAPTCHA
    }

    private record ClientScope(String address, Scope scope) {
    }

    private static final class FixedWindow {
        private long startedAt = System.currentTimeMillis();
        private int requests;

        private boolean tryAcquire(int limit, long now) {
            if (now - startedAt >= WINDOW_MILLIS) {
                startedAt = now;
                requests = 0;
            }
            if (requests >= limit) {
                return false;
            }
            requests++;
            return true;
        }
    }
}
