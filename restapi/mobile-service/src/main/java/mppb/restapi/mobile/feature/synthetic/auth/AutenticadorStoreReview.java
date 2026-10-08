package mppb.restapi.mobile.feature.synthetic.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.mobile.feature.synthetic.audit.AuditoriaUsuarioSintetico;
import mppb.restapi.mobile.feature.synthetic.config.StoreReviewProperties;
import mppb.restapi.mobile.feature.synthetic.config.SyntheticUsersProperties;
import mppb.restapi.mobile.feature.synthetic.model.IdentidadeSintetica;
import mppb.restapi.mobile.feature.synthetic.model.OrigemConta;

@Slf4j
@Component
@ConditionalOnProperty(name = "uat.synthetic-users.enabled", havingValue = "true")
@RequiredArgsConstructor
public class AutenticadorStoreReview {

    public static final String REVIEW_TOKEN_PREFIX = "REVIEW-SYNTH.";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final StoreReviewProperties reviewProperties;
    private final SyntheticUsersProperties syntheticProperties;
    private final AuditoriaUsuarioSintetico auditoria;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isHabilitado() {
        return reviewProperties.isEnabled() && !reviewProperties.isExpirado();
    }

    public boolean isTokenReview(String token) {
        return token != null && token.startsWith(REVIEW_TOKEN_PREFIX);
    }

    public Optional<String> autenticarReview(String login, String senha) {
        if (!reviewProperties.isEnabled()) {
            auditoria.registrarFalhaStoreReview(login, "STORE_REVIEW_DESABILITADO");
            return Optional.empty();
        }

        if (reviewProperties.isExpirado()) {
            auditoria.registrarFalhaStoreReview(login, "STORE_REVIEW_EXPIRADO");
            return Optional.empty();
        }

        if (login == null || login.isBlank() || senha == null || senha.isBlank()) {
            auditoria.registrarFalhaStoreReview(login, "CREDENCIAL_EM_BRANCO");
            return Optional.empty();
        }

        // Allowlist estrita: aceita apenas as 4 identidades exatas de Store Review
        if (!IdentidadeSintetica.isLoginReviewExato(login)) {
            auditoria.registrarFalhaStoreReview(login, "LOGIN_NAO_PERMITIDO_EM_STORE_REVIEW");
            return Optional.empty();
        }

        Optional<IdentidadeSintetica> optIdentidade = IdentidadeSintetica.porLogin(login);
        if (optIdentidade.isEmpty()) {
            auditoria.registrarFalhaStoreReview(login, "IDENTIDADE_NAO_ENCONTRADA");
            return Optional.empty();
        }

        IdentidadeSintetica identidade = optIdentidade.get();
        String senhaEsperada = syntheticProperties.getSenha(identidade.getChaveSenha());

        if (senhaEsperada == null || senhaEsperada.isBlank()) {
            auditoria.registrarFalhaStoreReview(login, "SENHA_NAO_CONFIGURADA");
            log.warn("Tentativa de login para identidade Store Review {} rejeitada: senha nao definida no ambiente.", login);
            return Optional.empty();
        }

        boolean senhaCorreta = MessageDigest.isEqual(
                senha.getBytes(StandardCharsets.UTF_8),
                senhaEsperada.getBytes(StandardCharsets.UTF_8));

        if (!senhaCorreta) {
            auditoria.registrarFalhaStoreReview(login, "SENHA_INVALIDA");
            return Optional.empty();
        }

        String token = gerarTokenReview(identidade);
        auditoria.registrarStoreReview(identidade, "AUTENTICACAO_SUCESSO");
        return Optional.of(token);
    }

    public String gerarTokenReview(IdentidadeSintetica identidade) {
        try {
            long now = Instant.now().getEpochSecond();
            long exp = now + reviewProperties.getTokenTtlSeconds();

            ObjectNode header = objectMapper.createObjectNode();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("iss", reviewProperties.getIssuer());
            payload.put("aud", reviewProperties.getAudience());
            payload.put("sub", identidade.getLogin());
            payload.put("origem", identidade.getOrigem().name());
            payload.put("origin", identidade.getOrigem() == OrigemConta.REVIEW_GOOGLE ? "GOOGLE_REVIEW" : "APPLE_REVIEW");
            payload.put("perfil", identidade.getPerfil().name());
            payload.put("profile", identidade.getPerfil().name());
            payload.put("matricula", identidade.getMatricula());
            payload.put("identificador", identidade.getIdentificador());
            payload.put("jti", UUID.randomUUID().toString());
            payload.put("iat", now);
            payload.put("exp", exp);

            String headerB64 = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(header));
            String payloadB64 = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(payload));

            String contentToSign = headerB64 + "." + payloadB64;
            String signatureB64 = assinar(contentToSign, reviewProperties.getJwtSecret());

            return REVIEW_TOKEN_PREFIX + contentToSign + "." + signatureB64;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar token Store Review", e);
        }
    }

    public Optional<IdentidadeSintetica> validarTokenReview(String token) {
        if (!reviewProperties.isEnabled() || reviewProperties.isExpirado() || token == null || !token.startsWith(REVIEW_TOKEN_PREFIX)) {
            return Optional.empty();
        }

        try {
            String jwtPart = token.substring(REVIEW_TOKEN_PREFIX.length());
            String[] parts = jwtPart.split("\\.");
            if (parts.length != 3) {
                auditoria.registrarFalhaStoreReview("N/A", "TOKEN_FORMATO_INVALIDO");
                return Optional.empty();
            }

            String headerB64 = parts[0];
            String payloadB64 = parts[1];
            String signatureB64 = parts[2];

            String contentToSign = headerB64 + "." + payloadB64;
            String expectedSig = assinar(contentToSign, reviewProperties.getJwtSecret());

            if (!MessageDigest.isEqual(
                    signatureB64.getBytes(StandardCharsets.UTF_8),
                    expectedSig.getBytes(StandardCharsets.UTF_8))) {
                auditoria.registrarFalhaStoreReview("N/A", "TOKEN_ASSINATURA_INVALIDA");
                return Optional.empty();
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(payloadB64);
            JsonNode payload = objectMapper.readTree(payloadBytes);

            // Validar expiração
            long exp = payload.path("exp").asLong(0);
            if (exp > 0 && Instant.now().getEpochSecond() > exp) {
                auditoria.registrarFalhaStoreReview(payload.path("sub").asText(), "TOKEN_EXPIRADO");
                return Optional.empty();
            }

            // Validar Issuer
            String iss = payload.path("iss").asText();
            if (!reviewProperties.getIssuer().equals(iss)) {
                auditoria.registrarFalhaStoreReview(payload.path("sub").asText(), "TOKEN_ISSUER_INVALIDO");
                return Optional.empty();
            }

            // Validar Audience
            String aud = payload.path("aud").asText();
            if (!reviewProperties.getAudience().equals(aud)) {
                auditoria.registrarFalhaStoreReview(payload.path("sub").asText(), "TOKEN_AUDIENCE_INVALIDO");
                return Optional.empty();
            }

            String sub = payload.path("sub").asText();
            if (!IdentidadeSintetica.isLoginReviewExato(sub)) {
                auditoria.registrarFalhaStoreReview(sub, "LOGIN_NAO_PERMITIDO_EM_STORE_REVIEW");
                return Optional.empty();
            }

            Optional<IdentidadeSintetica> optIdentidade = IdentidadeSintetica.porLogin(sub);
            if (optIdentidade.isPresent()) {
                IdentidadeSintetica id = optIdentidade.get();
                String origem = payload.path("origem").asText();
                String perfil = payload.path("perfil").asText();
                String matricula = payload.path("matricula").asText();
                if (!id.getOrigem().name().equalsIgnoreCase(origem)
                        || !id.getPerfil().name().equalsIgnoreCase(perfil)
                        || !id.getMatricula().equalsIgnoreCase(matricula)) {
                    auditoria.registrarFalhaStoreReview(sub, "TOKEN_PAYLOAD_DIVERGENTE");
                    return Optional.empty();
                }
                auditoria.registrarStoreReview(id, "VALIDACAO_TOKEN_SUCESSO");
                return optIdentidade;
            }

            auditoria.registrarFalhaStoreReview(sub, "IDENTIDADE_NAO_ENCONTRADA");
            return Optional.empty();
        } catch (Exception e) {
            auditoria.registrarFalhaStoreReview("N/A", "TOKEN_ERRO_VALIDACAO");
            return Optional.empty();
        }
    }

    private String assinar(String content, String secret) throws Exception {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Chave de assinatura de Store Review ausente.");
        }
        Mac hmac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        hmac.init(secretKey);
        byte[] rawHmac = hmac.doFinal(content.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
    }
}
