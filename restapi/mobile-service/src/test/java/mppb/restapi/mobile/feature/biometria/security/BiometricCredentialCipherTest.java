package mppb.restapi.mobile.feature.biometria.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class BiometricCredentialCipherTest {

    private static final String KEY = Base64.getEncoder()
            .encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    @Test
    void cifraEDecifraTokenComAesGcm() {
        BiometricCredentialCipher cipher = new BiometricCredentialCipher(KEY);

        String encrypted = cipher.cifrar("token-institucional");

        assertEquals("token-institucional", cipher.decifrar(encrypted));
    }

    @Test
    void rejeitaPayloadCifradoAdulterado() {
        BiometricCredentialCipher cipher = new BiometricCredentialCipher(KEY);
        String encrypted = cipher.cifrar("token-institucional");
        char replacement = encrypted.endsWith("A") ? 'B' : 'A';
        String tampered = encrypted.substring(0, encrypted.length() - 1) + replacement;

        assertThrows(IllegalStateException.class, () -> cipher.decifrar(tampered));
    }

    @Test
    void exigeChaveDe256Bits() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);

        assertThrows(IllegalStateException.class, () -> new BiometricCredentialCipher(shortKey));
    }
}
