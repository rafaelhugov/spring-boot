package mppb.restapi.mobile.feature.synthetic.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

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
import mppb.restapi.mobile.feature.synthetic.config.SyntheticUsersProperties;
import mppb.restapi.mobile.feature.synthetic.model.IdentidadeSintetica;

@Slf4j
@Component
@ConditionalOnProperty(name = "uat.synthetic-users.enabled", havingValue = "true")
@RequiredArgsConstructor
public class AutenticadorUsuarioSintetico {

    public static final String TOKEN_PREFIX = "UAT-SYNTH.";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long DEFAULT_TTL_SECONDS = 7 * 24 * 3600; // 7 dias

    private final SyntheticUsersProperties properties;
    private final AuditoriaUsuarioSintetico auditoria;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isHabilitado() {
        return properties.isEnabled();
    }

    public Optional<String> autenticar(String login, String senha) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }

        if (login == null || login.isBlank() || senha == null || senha.isBlank()) {
            auditoria.registrarFalha(login, "CREDENCIAL_EM_BRANCO");
            return Optional.empty();
        }

        Optional<IdentidadeSintetica> optIdentidade = IdentidadeSintetica.porLogin(login);
        if (optIdentidade.isEmpty()) {
            return Optional.empty();
        }

        IdentidadeSintetica identidade = optIdentidade.get();
        String senhaEsperada = properties.getSenha(identidade.getChaveSenha());

        if (senhaEsperada == null || senhaEsperada.isBlank()) {
            auditoria.registrarFalha(login, "SENHA_NAO_CONFIGURADA_EM_AMBIENTE");
            log.warn("Tentativa de login para usuario sintetico {} rejeitada: senha nao definida no ambiente.", login);
            return Optional.empty();
        }

        boolean senhaCorreta = MessageDigest.isEqual(
                senha.getBytes(StandardCharsets.UTF_8),
                senhaEsperada.getBytes(StandardCharsets.UTF_8));

        if (!senhaCorreta) {
            auditoria.registrarFalha(login, "SENHA_INVALIDA");
            return Optional.empty();
        }

        String token = gerarToken(identidade);
        auditoria.registrar(identidade, "AUTENTICACAO_SUCESSO");
        return Optional.of(token);
    }

    public String gerarToken(IdentidadeSintetica identidade) {
        try {
            long now = Instant.now().getEpochSecond();
            long exp = now + DEFAULT_TTL_SECONDS;

            ObjectNode header = objectMapper.createObjectNode();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("sub", identidade.getLogin());
            payload.put("identificador", identidade.getIdentificador());
            payload.put("origem", identidade.getOrigem().name());
            payload.put("perfil", identidade.getPerfil().name());
            payload.put("matricula", identidade.getMatricula());
            payload.put("jti", java.util.UUID.randomUUID().toString());
            payload.put("iat", now);
            payload.put("exp", exp);

            String headerB64 = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(header));
            String payloadB64 = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(payload));

            String contentToSign = headerB64 + "." + payloadB64;
            String signatureB64 = assinar(contentToSign, properties.getJwtSecret());

            return TOKEN_PREFIX + contentToSign + "." + signatureB64;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar token sintetico", e);
        }
    }

    public Optional<IdentidadeSintetica> validarToken(String token) {
        if (!properties.isEnabled() || token == null || !token.startsWith(TOKEN_PREFIX)) {
            return Optional.empty();
        }

        try {
            String jwtPart = token.substring(TOKEN_PREFIX.length());
            String[] parts = jwtPart.split("\\.");
            if (parts.length != 3) {
                auditoria.registrarFalha("N/A", "TOKEN_SINTETICO_FORMATO_INVALIDO");
                return Optional.empty();
            }

            String headerB64 = parts[0];
            String payloadB64 = parts[1];
            String signatureB64 = parts[2];

            String contentToSign = headerB64 + "." + payloadB64;
            String expectedSig = assinar(contentToSign, properties.getJwtSecret());

            if (!MessageDigest.isEqual(
                    signatureB64.getBytes(StandardCharsets.UTF_8),
                    expectedSig.getBytes(StandardCharsets.UTF_8))) {
                auditoria.registrarFalha("N/A", "TOKEN_SINTETICO_ASSINATURA_INVALIDA");
                return Optional.empty();
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(payloadB64);
            JsonNode payload = objectMapper.readTree(payloadBytes);

            long exp = payload.path("exp").asLong(0);
            if (exp > 0 && Instant.now().getEpochSecond() > exp) {
                auditoria.registrarFalha(payload.path("sub").asText(), "TOKEN_SINTETICO_EXPIRADO");
                return Optional.empty();
            }

            String sub = payload.path("sub").asText();
            Optional<IdentidadeSintetica> optIdentidade = IdentidadeSintetica.porLogin(sub);
            if (optIdentidade.isPresent()) {
                IdentidadeSintetica id = optIdentidade.get();
                String origem = payload.path("origem").asText();
                String perfil = payload.path("perfil").asText();
                String matricula = payload.path("matricula").asText();
                if (!id.getOrigem().name().equalsIgnoreCase(origem)
                        || !id.getPerfil().name().equalsIgnoreCase(perfil)
                        || !id.getMatricula().equalsIgnoreCase(matricula)) {
                    auditoria.registrarFalha(sub, "TOKEN_SINTETICO_PAYLOAD_DIVERGENTE");
                    return Optional.empty();
                }
                return optIdentidade;
            }

            auditoria.registrarFalha(sub, "IDENTIDADE_SINTETICA_DESCONHECIDA");
            return Optional.empty();
        } catch (Exception e) {
            auditoria.registrarFalha("N/A", "TOKEN_SINTETICO_ERRO_VALIDACAO");
            return Optional.empty();
        }
    }

    public boolean isLoginSintetico(String login) {
        return properties.isEnabled() && IdentidadeSintetica.porLogin(login).isPresent();
    }

    public boolean isMatriculaSintetica(String matricula) {
        return properties.isEnabled() && IdentidadeSintetica.porMatricula(matricula).isPresent();
    }

    public boolean isMatriculaSintetica(Integer matricula) {
        if (matricula == null) return false;
        return isMatriculaSintetica(matricula.toString());
    }

    public boolean isCpfSintetico(String cpf) {
        return properties.isEnabled() && IdentidadeSintetica.porCpf(cpf).isPresent();
    }

    public boolean isTokenSintetico(String token) {
        return properties.isEnabled() && token != null && token.startsWith(TOKEN_PREFIX);
    }

    private String assinar(String content, String secret) throws Exception {
        Mac hmac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        hmac.init(secretKey);
        byte[] rawHmac = hmac.doFinal(content.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
    }
}
