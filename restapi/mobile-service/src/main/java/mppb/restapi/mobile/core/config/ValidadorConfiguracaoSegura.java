package mppb.restapi.mobile.core.config;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

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
        if (perfis.stream().anyMatch(p -> p.equals("prod") || p.equals("production") || p.equals("prd"))) {
            validarProducao(perfis);
        }
        validarUsuariosSinteticos(perfis);
    }

    private void validarProducao(List<String> perfis) {
        if (Boolean.parseBoolean(valor("uat.synthetic-users.enabled"))) {
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

    private void validarUsuariosSinteticos(List<String> perfis) {
        if (!Boolean.parseBoolean(valor("uat.synthetic-users.enabled"))) {
            return;
        }

        boolean isProd = perfis.stream().anyMatch(p -> p.equals("prod") || p.equals("production") || p.equals("prd"));
        if (isProd) {
            throw new IllegalStateException("Usuarios sinteticos UAT/Review nao podem ser habilitados em perfis de producao (prod, production, prd).");
        }

        List<String> allowlist = List.of("homolog", "uat", "review", "test", "dev", "local");
        boolean allowlisted = perfis.stream().anyMatch(allowlist::contains);
        if (!allowlisted) {
            throw new IllegalStateException("Usuarios sinteticos UAT/Review somente podem ser ativados em profiles homolog, uat, review, test, dev, local.");
        }

        String jwtSecret = valor("uat.synthetic-users.jwt-secret");
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("uat.synthetic-users.jwt-secret obrigatorio quando usuarios sinteticos estao habilitados.");
        }

        List<String> chavesSenhas = List.of(
                "uat.synthetic-users.passwords.uat-membro",
                "uat.synthetic-users.passwords.uat-servidor",
                "uat.synthetic-users.passwords.google-review-membro",
                "uat.synthetic-users.passwords.google-review-servidor",
                "uat.synthetic-users.passwords.apple-review-membro",
                "uat.synthetic-users.passwords.apple-review-servidor");

        for (String chave : chavesSenhas) {
            String senha = valor(chave);
            if (senha == null || senha.isBlank()) {
                throw new IllegalStateException("Senha obrigatoria ausente para usuarios sinteticos: " + chave);
            }
        }
    }

    private List<String> perfisAtivos() {
        return Arrays.stream(environment.getActiveProfiles())
                .map(perfil -> perfil == null ? "" : perfil.trim().toLowerCase(Locale.ROOT))
                .filter(perfil -> !perfil.isBlank())
                .toList();
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
        return environment.getProperty(propriedade);
    }
}
