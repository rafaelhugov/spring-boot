package mppb.restapi.mobile.feature.synthetic.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "uat.synthetic-users")
public class SyntheticUsersProperties {

    /**
     * Habilitação explícita da infraestrutura de usuários sintéticos para UAT interno e Store Review.
     * Padrão obrigatório: FALSE. Bloqueado em perfil de produção.
     */
    private boolean enabled = false;

    /**
     * Segredo HMAC-SHA256 para assinatura dos tokens sintéticos de sessão.
     * Obrigatório via segredo externo (UAT_SYNTHETIC_JWT_SECRET) quando a feature for ativada.
     */
    private String jwtSecret;

    /**
     * Senhas de acesso para as identidades sintéticas carregadas de variáveis de ambiente.
     * Chaves: uat-membro, uat-servidor, google-review-membro, google-review-servidor,
     * apple-review-membro, apple-review-servidor.
     */
    private Map<String, String> passwords = new HashMap<>();

    public String getSenha(String chave) {
        if (chave == null) {
            return null;
        }
        String senha = passwords.get(chave);
        if (senha != null && !senha.isBlank()) {
            return senha;
        }
        return switch (chave) {
            case "google-review-membro" -> System.getenv("GOOGLE_REVIEW_MEMBER_PASSWORD");
            case "google-review-servidor" -> System.getenv("GOOGLE_REVIEW_SERVER_PASSWORD");
            case "apple-review-membro" -> System.getenv("APPLE_REVIEW_MEMBER_PASSWORD");
            case "apple-review-servidor" -> System.getenv("APPLE_REVIEW_SERVER_PASSWORD");
            case "uat-membro" -> System.getenv("UAT_MEMBER_PASSWORD");
            case "uat-servidor" -> System.getenv("UAT_SERVER_PASSWORD");
            default -> null;
        };
    }
}
