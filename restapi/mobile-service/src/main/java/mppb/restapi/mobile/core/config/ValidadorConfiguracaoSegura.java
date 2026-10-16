package mppb.restapi.mobile.core.config;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ValidadorConfiguracaoSegura implements ApplicationRunner {

    private static final List<String> PROPRIEDADES_CRITICAS_PROD = List.of(
            "api.auth.url",
            "spring.datasource.url",
            "spring.datasource.username",
            "spring.datasource.password",
            "mpvirtual.recursos-humanos.base-url");

    private final Environment environment;

    public ValidadorConfiguracaoSegura(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        validar();
    }

    public void validar() {
        List<String> perfis = perfisAtivos();
        if (perfis.isEmpty()) {
            throw new IllegalStateException("SPRING_PROFILES_ACTIVE deve ser definido explicitamente.");
        }
        boolean isProd = perfis.stream().anyMatch(p -> p.equals("prod") || p.equals("production") || p.equals("prd"));
        if (isProd) {
            validarProducao(perfis);
        }
        validarReview(perfis, isProd);
        validarUsuariosSinteticos(perfis, isProd);
    }

    private void validarProducao(List<String> perfis) {
        if (perfis.contains("review")) {
            throw new IllegalStateException("Perfil review nao pode ser combinado com perfis de producao (prod, production, prd).");
        }

        String ambiente = ambienteOperacional();
        boolean reviewEnabled = Boolean.parseBoolean(valor("review.enabled"));
        boolean modoHibridoAtivo = modoReviewHibridoAtivo();
        boolean isHomologOperacional = "homolog".equalsIgnoreCase(ambiente);

        if (reviewEnabled) {
            if ("prod".equalsIgnoreCase(ambiente) || "production".equalsIgnoreCase(ambiente) || "prd".equalsIgnoreCase(ambiente)) {
                throw new IllegalStateException("Store Review nao pode ser habilitado no ambiente operacional de producao (MPPB_ENVIRONMENT=" + ambiente + ").");
            }
            if (!isHomologOperacional) {
                throw new IllegalStateException("Store Review nao pode ser habilitado em perfis de producao (prod, production, prd) sem MPPB_ENVIRONMENT=homolog (ambiente atual: " + ambiente + ").");
            }
        }

        boolean syntheticEnabled = Boolean.parseBoolean(valor("uat.synthetic-users.enabled"));
        if (syntheticEnabled && !(isHomologOperacional && reviewEnabled) && !modoHibridoAtivo) {
            throw new IllegalStateException("Usuarios sinteticos UAT/Review nao podem ser habilitados em perfis de producao (prod, production, prd).");
        }

        if (perfis.contains("insecure")) {
            throw new IllegalStateException("Perfil insecure nao pode ser combinado com prod.");
        }
        if (perfis.contains("trace")) {
            throw new IllegalStateException("Perfil trace nao pode ser combinado com prod.");
        }
        if (Boolean.parseBoolean(valor("spring.jpa.show-sql"))) {
            throw new IllegalStateException("spring.jpa.show-sql deve ser false em prod.");
        }
        if (Boolean.parseBoolean(valor("spring.jpa.properties.hibernate.format_sql"))) {
            throw new IllegalStateException("hibernate.format_sql deve ser false em prod.");
        }
        for (String propriedade : PROPRIEDADES_CRITICAS_PROD) {
            exigirConfiguracaoExplicita(propriedade);
        }
        rejeitarValor("spring.datasource.password", "senha");
        rejeitarValor("jcr.user", "admin");
        rejeitarValor("jcr.password", "admin");
        rejeitarUrlLocal("spring.datasource.url");
        validarCors();
    }

    private void validarReview(List<String> perfis, boolean isProd) {
        boolean reviewProfile = perfis.contains("review");
        boolean reviewEnabled = Boolean.parseBoolean(valor("review.enabled"));

        if (!reviewProfile && !reviewEnabled) {
            return;
        }

        String ambiente = ambienteOperacional();
        boolean isHomologOperacional = "homolog".equalsIgnoreCase(ambiente);

        if (reviewEnabled) {
            if ("prod".equalsIgnoreCase(ambiente) || "production".equalsIgnoreCase(ambiente) || "prd".equalsIgnoreCase(ambiente)) {
                throw new IllegalStateException("Store Review nao pode ser habilitado no ambiente operacional de producao (MPPB_ENVIRONMENT=" + ambiente + ").");
            }
            if (isProd && !isHomologOperacional) {
                throw new IllegalStateException("Store Review em perfil prod exige definicao explicita de MPPB_ENVIRONMENT=homolog.");
            }
            if (ambiente != null && !isHomologOperacional) {
                throw new IllegalStateException("Store Review nao pode ser habilitado em ambiente operacional desconhecido ou invalido: " + ambiente + ". Apenas MPPB_ENVIRONMENT=homolog e permitido.");
            }
        }

        if (isProd && !isHomologOperacional) {
            throw new IllegalStateException("Store Review nao pode ser habilitado em perfis de producao (prod, production, prd).");
        }

        List<String> allowlist = List.of("review", "test", "dev", "local", "homolog", "uat", "prod");
        boolean allowlisted = perfis.stream().anyMatch(allowlist::contains);
        if (!allowlisted) {
            throw new IllegalStateException("Store Review somente pode ser ativado em profiles review, test, dev, local, homolog, uat, prod.");
        }

        String jwtSecretReview = valor("review.jwt-secret");
        if (jwtSecretReview == null || jwtSecretReview.isBlank()) {
            throw new IllegalStateException("review.jwt-secret obrigatorio quando Store Review esta habilitado (STORE_REVIEW_JWT_SECRET ausente).");
        }

        String jwtSecretUat = valor("uat.synthetic-users.jwt-secret");
        if (jwtSecretUat != null && !jwtSecretUat.isBlank() && jwtSecretReview.trim().equals(jwtSecretUat.trim())) {
            throw new IllegalStateException("A chave de assinatura do Store Review deve ser exclusiva e diferente da chave de UAT.");
        }

        Map<String, String> senhasReview = Map.of(
                "uat.synthetic-users.passwords.google-review-membro", "GOOGLE_REVIEW_MEMBER_PASSWORD",
                "uat.synthetic-users.passwords.google-review-servidor", "GOOGLE_REVIEW_SERVER_PASSWORD",
                "uat.synthetic-users.passwords.apple-review-membro", "APPLE_REVIEW_MEMBER_PASSWORD",
                "uat.synthetic-users.passwords.apple-review-servidor", "APPLE_REVIEW_SERVER_PASSWORD");

        for (Map.Entry<String, String> entry : senhasReview.entrySet()) {
            String chave = entry.getKey();
            String envVar = entry.getValue();
            String senha = valor(chave);
            if (senha == null || senha.isBlank()) {
                throw new IllegalStateException("Senha obrigatoria ausente para Store Review: " + envVar + " (" + chave + ")");
            }
        }
    }

    private void validarUsuariosSinteticos(List<String> perfis, boolean isProd) {
        if (!Boolean.parseBoolean(valor("uat.synthetic-users.enabled"))) {
            return;
        }

        String ambiente = ambienteOperacional();
        boolean isHomologOperacional = "homolog".equalsIgnoreCase(ambiente);
        boolean reviewEnabled = Boolean.parseBoolean(valor("review.enabled"));
        boolean hybridEnabled = Boolean.parseBoolean(valor("review.hybrid-enabled"));
        boolean appleHybridEnabled = Boolean.parseBoolean(valor("review.apple-hybrid-enabled"));
        boolean modoHibridoAtivo = hybridEnabled || appleHybridEnabled;

        if (isProd && !(isHomologOperacional && reviewEnabled) && !modoHibridoAtivo) {
            throw new IllegalStateException("Usuarios sinteticos UAT/Review nao podem ser habilitados em perfis de producao (prod, production, prd).");
        }

        List<String> allowlist = List.of("homolog", "uat", "review", "test", "dev", "local", "prod");
        boolean allowlisted = perfis.stream().anyMatch(allowlist::contains);
        if (!allowlisted) {
            throw new IllegalStateException("Usuarios sinteticos UAT/Review somente podem ser ativados em profiles homolog, uat, review, test, dev, local, prod.");
        }

        boolean apenasReview = (perfis.contains("review") || (isHomologOperacional && reviewEnabled) || hybridEnabled || appleHybridEnabled)
                && !perfis.contains("uat") && !perfis.contains("homolog");
        if (!apenasReview) {
            String jwtSecret = valor("uat.synthetic-users.jwt-secret");
            if (jwtSecret == null || jwtSecret.isBlank()) {
                throw new IllegalStateException("uat.synthetic-users.jwt-secret obrigatorio quando usuarios sinteticos estao habilitados.");
            }

            List<String> chavesSenhasUat = List.of(
                    "uat.synthetic-users.passwords.uat-membro",
                    "uat.synthetic-users.passwords.uat-servidor");

            for (String chave : chavesSenhasUat) {
                String senha = valor(chave);
                if (senha == null || senha.isBlank()) {
                    throw new IllegalStateException("Senha obrigatoria ausente para usuarios sinteticos: " + chave);
                }
            }
        }

        if (hybridEnabled || appleHybridEnabled) {
            String jwtSecretReview = valor("review.jwt-secret");
            if (jwtSecretReview == null || jwtSecretReview.isBlank()) {
                throw new IllegalStateException("review.jwt-secret obrigatorio quando Store Review hibrido esta habilitado (STORE_REVIEW_JWT_SECRET ausente).");
            }
            List<String> chavesSenhasReview = hybridEnabled
                    ? List.of(
                            "uat.synthetic-users.passwords.google-review-membro",
                            "uat.synthetic-users.passwords.google-review-servidor",
                            "uat.synthetic-users.passwords.apple-review-membro",
                            "uat.synthetic-users.passwords.apple-review-servidor")
                    : List.of(
                            "uat.synthetic-users.passwords.apple-review-membro",
                            "uat.synthetic-users.passwords.apple-review-servidor");
            for (String chave : chavesSenhasReview) {
                String senha = valor(chave);
                if (senha == null || senha.isBlank()) {
                    throw new IllegalStateException("Senha obrigatoria ausente para Store Review hibrido: " + chave);
                }
            }
        }
    }

    private List<String> perfisAtivos() {
        return Arrays.stream(environment.getActiveProfiles())
                .map(perfil -> perfil == null ? "" : perfil.trim().toLowerCase(Locale.ROOT))
                .filter(perfil -> !perfil.isBlank())
                .toList();
    }

    private boolean modoReviewHibridoAtivo() {
        return Boolean.parseBoolean(valor("review.hybrid-enabled"))
                || Boolean.parseBoolean(valor("review.apple-hybrid-enabled"));
    }

    private void exigirConfiguracaoExplicita(String propriedade) {
        String valor = valor(propriedade);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Configuracao obrigatoria ausente: " + propriedade);
        }
    }

    private void rejeitarValor(String propriedade, String valorProibido) {
        String valor = valor(propriedade);
        if (valorProibido.equalsIgnoreCase(valor == null ? "" : valor.trim())) {
            throw new IllegalStateException("Configuracao insegura em prod: " + propriedade);
        }
    }

    private void rejeitarUrlLocal(String propriedade) {
        String valor = valor(propriedade);
        if (valor == null) {
            return;
        }
        String normalizado = valor.toLowerCase(Locale.ROOT);
        if (normalizado.contains("localhost") || normalizado.contains("127.0.0.1")) {
            throw new IllegalStateException("URL local nao permitida em prod: " + propriedade);
        }
    }

    private void validarCors() {
        boolean corsAtivo = Boolean.parseBoolean(valor("cors.enabled"));
        boolean credenciais = Boolean.parseBoolean(valor("cors.access-control-headers.allow-credentials"));
        String origem = valor("cors.access-control-headers.allow-origin");
        if (corsAtivo && credenciais && "*".equals(origem)) {
            throw new IllegalStateException("CORS com credenciais nao pode usar origem wildcard em prod.");
        }
    }

    private String valor(String propriedade) {
        String val = environment.getProperty(propriedade);
        if (val != null && !val.isBlank()) {
            return val;
        }
        return switch (propriedade) {
            case "review.enabled" -> {
                String envVal = environment.getProperty("STORE_REVIEW_ENABLED");
                yield (envVal != null && !envVal.isBlank()) ? envVal : environment.getProperty("REVIEW_ENABLED");
            }
            case "review.hybrid-enabled" -> {
                String envVal = environment.getProperty("STORE_REVIEW_HYBRID_ENABLED");
                yield (envVal != null && !envVal.isBlank()) ? envVal : environment.getProperty("REVIEW_HYBRID_ENABLED");
            }
            case "review.apple-hybrid-enabled" -> {
                String envVal = environment.getProperty("APPLE_REVIEW_HYBRID_ENABLED");
                yield (envVal != null && !envVal.isBlank()) ? envVal : environment.getProperty("REVIEW_APPLE_HYBRID_ENABLED");
            }
            case "review.jwt-secret" -> {
                String envVal = environment.getProperty("STORE_REVIEW_JWT_SECRET");
                yield (envVal != null && !envVal.isBlank()) ? envVal : environment.getProperty("REVIEW_JWT_SECRET");
            }
            case "review.valid-until" -> {
                String envVal = environment.getProperty("STORE_REVIEW_VALID_UNTIL");
                yield (envVal != null && !envVal.isBlank()) ? envVal : environment.getProperty("REVIEW_VALID_UNTIL");
            }
            case "uat.synthetic-users.passwords.google-review-membro" -> environment.getProperty("GOOGLE_REVIEW_MEMBER_PASSWORD");
            case "uat.synthetic-users.passwords.google-review-servidor" -> environment.getProperty("GOOGLE_REVIEW_SERVER_PASSWORD");
            case "uat.synthetic-users.passwords.apple-review-membro" -> environment.getProperty("APPLE_REVIEW_MEMBER_PASSWORD");
            case "uat.synthetic-users.passwords.apple-review-servidor" -> environment.getProperty("APPLE_REVIEW_SERVER_PASSWORD");
            case "uat.synthetic-users.passwords.uat-membro" -> environment.getProperty("UAT_MEMBER_PASSWORD");
            case "uat.synthetic-users.passwords.uat-servidor" -> environment.getProperty("UAT_SERVER_PASSWORD");
            case "uat.synthetic-users.jwt-secret" -> environment.getProperty("UAT_SYNTHETIC_JWT_SECRET");
            case "uat.synthetic-users.enabled" -> {
                String uat = environment.getProperty("UAT_SYNTHETIC_USERS_ENABLED");
                if (uat != null && !uat.isBlank()) {
                    yield uat;
                }
                String storeReview = environment.getProperty("STORE_REVIEW_ENABLED");
                if (storeReview != null && !storeReview.isBlank()) {
                    yield storeReview;
                }
                String hybrid = environment.getProperty("STORE_REVIEW_HYBRID_ENABLED");
                if (hybrid != null && !hybrid.isBlank()) {
                    yield hybrid;
                }
                yield environment.getProperty("APPLE_REVIEW_HYBRID_ENABLED");
            }
            case "mppb.environment" -> environment.getProperty("MPPB_ENVIRONMENT");
            default -> null;
        };
    }

    private String ambienteOperacional() {
        String env = valor("mppb.environment");
        if (env == null || env.isBlank()) {
            env = environment.getProperty("MPPB_ENVIRONMENT");
        }
        if (env == null || env.isBlank()) {
            return null;
        }
        return env.trim().toLowerCase(Locale.ROOT);
    }
}
