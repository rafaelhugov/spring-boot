package mppb.restapi.mobile.feature.processoconsulta.controller;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Interceptor simples para Rate Limiting baseado em IP.
 * Limita a 10 requisições por minuto por IP para o endpoint de consulta pública.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Map<String, TokenBucket> cache = new ConcurrentHashMap<>();

    private static class TokenBucket {
        AtomicInteger tokens = new AtomicInteger(10);
        long lastRefillTime = System.currentTimeMillis();

        synchronized boolean tryConsume() {
            refill();
            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            if (now - lastRefillTime > 60000) { // 1 minuto
                tokens.set(10); // Reseta para 10
                lastRefillTime = now;
            }
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (request.getRequestURI().contains("/api/public/v1/processos/consulta")) {
            String clientIp = getClientIP(request);
            TokenBucket bucket = cache.computeIfAbsent(clientIp, k -> new TokenBucket());

            if (!bucket.tryConsume()) {
                response.setStatus(429); // Too Many Requests
                response.getWriter().write("Muitas requisições. Tente novamente em um minuto.");
                return false;
            }
        }
        return true;
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
