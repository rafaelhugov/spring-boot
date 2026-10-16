package mppb.restapi.mobile.feature.synthetic.config;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "review")
public class StoreReviewProperties {

    /**
     * Habilitação explícita da funcionalidade de Store Review.
     * Padrão obrigatório: FALSE. Bloqueado em perfis de produção (prod, production, prd).
     */
    private boolean enabled = false;

    /**
     * Modo híbrido para revisão das lojas: aceita identidades sintéticas de review
     * e preserva autenticação institucional para os demais usuários.
     */
    private boolean hybridEnabled = false;

    /**
     * Modo híbrido legado para App Store: aceita somente as identidades sintéticas Apple.
     */
    private boolean appleHybridEnabled = false;

    /**
     * Chave de assinatura exclusiva para tokens Store Review.
     * Obrigatória via segredo externo (REVIEW_JWT_SECRET) quando a feature for ativada.
     * Deve ser diferente da chave UAT e nunca versionada.
     */
    private String jwtSecret;

    /**
     * Data/hora limite de expiração operacional das credenciais de Store Review.
     * Formato ISO-8601 (ex.: 2026-12-31T23:59:59Z ou 2026-12-31).
     * Se definida e a data atual for posterior, todas as autenticações de Review são rejeitadas.
     */
    private String validUntil;

    /**
     * TTL dos tokens Store Review em segundos. Padrão: 86400 (24 horas).
     */
    private long tokenTtlSeconds = 86400;

    /**
     * Emissor (issuer) canônico do token de Store Review.
     */
    private String issuer = "mppb-store-review";

    /**
     * Audiência (audience) canônica do token de Store Review.
     */
    private String audience = "mppb-mobile-review";

    public boolean isEnabled() {
        if (enabled) {
            return true;
        }
        String env = System.getenv("STORE_REVIEW_ENABLED");
        return "true".equalsIgnoreCase(env);
    }

    public boolean isAppleHybridEnabled() {
        if (appleHybridEnabled) {
            return true;
        }
        String env = System.getenv("APPLE_REVIEW_HYBRID_ENABLED");
        return "true".equalsIgnoreCase(env);
    }

    public boolean isHybridEnabled() {
        if (hybridEnabled) {
            return true;
        }
        String env = System.getenv("STORE_REVIEW_HYBRID_ENABLED");
        return "true".equalsIgnoreCase(env);
    }

    public String getJwtSecret() {
        if (jwtSecret != null && !jwtSecret.isBlank()) {
            return jwtSecret;
        }
        return System.getenv("STORE_REVIEW_JWT_SECRET");
    }

    public String getValidUntil() {
        if (validUntil != null && !validUntil.isBlank()) {
            return validUntil;
        }
        return System.getenv("STORE_REVIEW_VALID_UNTIL");
    }

    /**
     * Verifica se a janela de validade do Store Review expirou.
     */
    public boolean isExpirado() {
        String vu = getValidUntil();
        if (vu == null || vu.isBlank()) {
            return false;
        }
        try {
            String trimmed = vu.trim();
            Instant limite;
            if (trimmed.length() <= 10) {
                // Formato LocalDate yyyy-MM-dd -> considerar fim do dia UTC
                limite = LocalDate.parse(trimmed).atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
            } else {
                limite = Instant.parse(trimmed);
            }
            return Instant.now().isAfter(limite);
        } catch (DateTimeParseException e) {
            // Em caso de formato inválido, bloquear por princípio de menor privilégio
            return true;
        }
    }
}
