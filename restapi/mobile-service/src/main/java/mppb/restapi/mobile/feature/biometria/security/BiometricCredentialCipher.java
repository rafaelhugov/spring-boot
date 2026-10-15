package mppb.restapi.mobile.feature.biometria.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "biometria", name = "enabled", havingValue = "true")
public class BiometricCredentialCipher {

    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final byte[] AAD = "mppb-biometria-v1".getBytes(StandardCharsets.UTF_8);

    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public BiometricCredentialCipher(
            @Value("${biometria.credential.encryption-key:}") String encodedKey) {
        try {
            byte[] rawKey = Base64.getDecoder().decode(encodedKey == null ? "" : encodedKey.trim());
            if (rawKey.length != 32) {
                throw new IllegalStateException("BIOMETRIC_CREDENTIAL_ENCRYPTION_KEY deve conter 32 bytes em Base64");
            }
            this.key = new SecretKeySpec(rawKey, "AES");
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("BIOMETRIC_CREDENTIAL_ENCRYPTION_KEY invalida", e);
        }
    }

    public String cifrar(String plainText) {
        try {
            byte[] nonce = new byte[NONCE_LENGTH];
            secureRandom.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            cipher.updateAAD(AAD);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(ByteBuffer.allocate(nonce.length + encrypted.length)
                            .put(nonce)
                            .put(encrypted)
                            .array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao cifrar credencial biometrica", e);
        }
    }

    public String decifrar(String encoded) {
        try {
            byte[] payload = Base64.getUrlDecoder().decode(encoded);
            if (payload.length <= NONCE_LENGTH) {
                throw new IllegalStateException("Credencial biometrica cifrada invalida");
            }
            byte[] nonce = new byte[NONCE_LENGTH];
            byte[] encrypted = new byte[payload.length - NONCE_LENGTH];
            System.arraycopy(payload, 0, nonce, 0, nonce.length);
            System.arraycopy(payload, nonce.length, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            cipher.updateAAD(AAD);
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Falha ao decifrar credencial biometrica", e);
        }
    }
}
