package mppb.restapi.mobile.quality;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.core.context.SecurityContextHolder;

import mppb.restapi.common.infrastructure.client.authapi.dto.AcessoDataResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.UsuarioAutorizadoResponseDto;
import mppb.restapi.common.infrastructure.security.authorization.TokenAuthentication;
import mppb.restapi.common.infrastructure.security.util.SecurityUtils;
import mppb.restapi.mobile.feature.carteirafuncional.controller.CarteiraFuncionalController;
import mppb.restapi.mobile.feature.carteirafuncional.dto.CarteiraFuncionalTO;
import mppb.restapi.mobile.feature.carteirafuncional.service.CarteiraService;
import mppb.restapi.mobile.feature.synthetic.audit.AuditoriaUsuarioSintetico;
import mppb.restapi.mobile.feature.synthetic.auth.AutenticadorStoreReview;
import mppb.restapi.mobile.feature.synthetic.auth.AutenticadorUsuarioSintetico;
import mppb.restapi.mobile.feature.synthetic.auth.SyntheticTokenAuthenticationProvider;
import mppb.restapi.mobile.feature.synthetic.config.StoreReviewProperties;
import mppb.restapi.mobile.feature.synthetic.config.SyntheticUsersProperties;
import mppb.restapi.mobile.feature.synthetic.dataset.SyntheticDataService;
import mppb.restapi.mobile.feature.synthetic.model.IdentidadeSintetica;

@DisplayName("STORE-REVIEW-R02-AJUSTE: Fotografias Sintéticas das Personas Review")
public class StoreReviewFotografiaTest {

    private StoreReviewProperties reviewProperties;
    private SyntheticUsersProperties syntheticProperties;
    private AuditoriaUsuarioSintetico auditoria;
    private AutenticadorStoreReview autenticadorReview;
    private AutenticadorUsuarioSintetico autenticadorUat;
    private SyntheticDataService dataService;
    private SyntheticTokenAuthenticationProvider authProvider;
    private CarteiraService carteiraService;
    private CarteiraFuncionalController carteiraController;
    private CountingJdbcTemplate countingJdbcTemplate;

    @BeforeEach
    void setUp() {
        reviewProperties = new StoreReviewProperties();
        reviewProperties.setEnabled(true);
        reviewProperties.setJwtSecret("SecretParaStoreReviewIsolado123456789012345678901234567890!");
        reviewProperties.setTokenTtlSeconds(3600);
        reviewProperties.setIssuer("mppb-store-review");
        reviewProperties.setAudience("mppb-mobile-review");

        syntheticProperties = new SyntheticUsersProperties();
        syntheticProperties.setEnabled(true);
        syntheticProperties.setJwtSecret("SecretParaUatInterno123456789012345678901234567890!");
        syntheticProperties.setPasswords(Map.of(
                "membro.google", "SenhaGoogleMembro456!",
                "servidor.google", "SenhaGoogleServidor456!",
                "membro.apple", "SenhaAppleMembro789!",
                "servidor.apple", "SenhaAppleServidor789!"
        ));

        auditoria = new AuditoriaUsuarioSintetico();
        autenticadorReview = new AutenticadorStoreReview(reviewProperties, syntheticProperties, auditoria);
        autenticadorUat = new AutenticadorUsuarioSintetico(syntheticProperties, auditoria);

        dataService = new SyntheticDataService(autenticadorUat, auditoria, autenticadorReview);

        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("review");
        authProvider = new SyntheticTokenAuthenticationProvider(autenticadorUat, autenticadorReview, auditoria, env);

        countingJdbcTemplate = new CountingJdbcTemplate();
        // CarteiraService inicializado com jdbcTemplate e jcrService nulos para garantir isolamento
        carteiraService = new CarteiraService(countingJdbcTemplate, null, dataService);
        carteiraController = new CarteiraFuncionalController(carteiraService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(String login, String token) {
        UsuarioAutorizadoResponseDto usuario = new UsuarioAutorizadoResponseDto(1L, "Nome Ficticio", login);
        AcessoDataResponseDto acesso = new AcessoDataResponseDto(null, List.of(), null, null);
        TokenAuthentication auth = new TokenAuthentication(token, usuario, acesso, null, SecurityUtils.getAuthorities(acesso));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("1. membro.google retorna FOTO_MEMBRO")
    void test01_reviewGoogleMembro_retornaFotoMembro() {
        byte[] foto = dataService.getFotoByLogin("membro.google");
        assertNotNull(foto);
        byte[] fotoEsperada = dataService.getFotoReviewMembro();
        assertArrayEquals(fotoEsperada, foto);

        // Teste via CarteiraService
        byte[] fotoService = carteiraService.getBytesFotoServidorPorLogin("membro.google");
        assertArrayEquals(fotoEsperada, fotoService);
    }

    @Test
    @DisplayName("2. membro.apple retorna exatamente FOTO_MEMBRO")
    void test02_reviewAppleMembro_retornaExatamenteFotoMembro() {
        byte[] fotoGoogle = dataService.getFotoByLogin("membro.google");
        byte[] fotoApple = dataService.getFotoByLogin("membro.apple");

        assertNotNull(fotoApple);
        assertArrayEquals(fotoGoogle, fotoApple);
        assertArrayEquals(dataService.getFotoReviewMembro(), fotoApple);
    }

    @Test
    @DisplayName("3. servidor.google retorna FOTO_SERVIDOR")
    void test03_reviewGoogleServidor_retornaFotoServidor() {
        byte[] foto = dataService.getFotoByLogin("servidor.google");
        assertNotNull(foto);
        byte[] fotoEsperada = dataService.getFotoReviewServidor();
        assertArrayEquals(fotoEsperada, foto);

        // Teste via CarteiraService
        byte[] fotoService = carteiraService.getBytesFotoServidorPorLogin("servidor.google");
        assertArrayEquals(fotoEsperada, fotoService);
    }

    @Test
    @DisplayName("4. servidor.apple retorna exatamente FOTO_SERVIDOR")
    void test04_reviewAppleServidor_retornaExatamenteFotoServidor() {
        byte[] fotoGoogle = dataService.getFotoByLogin("servidor.google");
        byte[] fotoApple = dataService.getFotoByLogin("servidor.apple");

        assertNotNull(fotoApple);
        assertArrayEquals(fotoGoogle, fotoApple);
        assertArrayEquals(dataService.getFotoReviewServidor(), fotoApple);
    }

    @Test
    @DisplayName("5. FOTO_MEMBRO possui bytes > 0")
    void test05_fotoMembro_possuiBytesMaiorQueZero() {
        byte[] foto = dataService.getFotoReviewMembro();
        assertNotNull(foto);
        assertTrue(foto.length > 1000, "FOTO_MEMBRO deve ter tamanho real de imagem (> 1000 bytes)");
    }

    @Test
    @DisplayName("6. FOTO_SERVIDOR possui bytes > 0")
    void test06_fotoServidor_possuiBytesMaiorQueZero() {
        byte[] foto = dataService.getFotoReviewServidor();
        assertNotNull(foto);
        assertTrue(foto.length > 1000, "FOTO_SERVIDOR deve ter tamanho real de imagem (> 1000 bytes)");
    }

    @Test
    @DisplayName("7. MIME type correto (image/jpeg)")
    void test07_mimeTypeCorreto() {
        String tokenMembro = autenticadorReview.gerarTokenReview(IdentidadeSintetica.REVIEW_GOOGLE_MEMBRO);
        autenticarComo("membro.google", tokenMembro);

        ResponseEntity<byte[]> response = carteiraController.fotoServidor(null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);

        // Verifica magic bytes de JPEG (0xFF, 0xD8)
        byte[] bytes = response.getBody();
        assertEquals((byte) 0xFF, bytes[0]);
        assertEquals((byte) 0xD8, bytes[1]);
    }

    @Test
    @DisplayName("8. membro e servidor possuem imagens diferentes")
    void test08_membroEServidor_possuemImagensDiferentes() {
        byte[] fotoMembro = dataService.getFotoReviewMembro();
        byte[] fotoServidor = dataService.getFotoReviewServidor();

        assertNotNull(fotoMembro);
        assertNotNull(fotoServidor);
        assertFalse(Arrays.equals(fotoMembro, fotoServidor), "Imagens de Membro e Servidor devem ser distintas");
    }

    @Test
    @DisplayName("9. nenhuma consulta a repositorio institucional para foto Review")
    void test09_nenhumaConsultaARepositorioInstitucionalParaFotoReview() {
        countingJdbcTemplate.queriesCount = 0;

        for (String login : List.of("membro.google", "membro.apple", "servidor.google", "servidor.apple")) {
            byte[] foto = carteiraService.getBytesFotoServidorPorLogin(login);
            assertNotNull(foto);
        }

        assertEquals(0, countingJdbcTemplate.queriesCount, "Zero consultas JDBC/SQL devem ocorrer para fotos Review");
    }

    @Test
    @DisplayName("10. nenhum acesso a servico externo")
    void test10_nenhumAcessoAServicoExterno() {
        // CarteiraService instanciado sem jcrService (null); chamadas nao lancam NPE pois jcr nunca e tocado
        byte[] fotoMembro = carteiraService.getBytesFotoServidorPorLogin("membro.google");
        assertNotNull(fotoMembro);

        byte[] fotoServidor = carteiraService.getBytesFotoServidorPorLogin("servidor.apple");
        assertNotNull(fotoServidor);
    }

    @Test
    @DisplayName("11. identidade Review desconhecida continua rejeitada")
    void test11_identidadeReviewDesconhecida_continuaRejeitada() {
        // Login nao cadastrado na allowlist exata
        byte[] foto = carteiraService.getBytesFotoServidorPorLogin("review.desconhecido");
        assertNull(foto);

        // Tentativa no controller sem autenticacao
        ResponseEntity<byte[]> response = carteiraController.fotoServidor(null);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("12. token adulterado nao permite obter fotografia")
    void test12_tokenAdulterado_naoPermiteObterFotografia() {
        String tokenValido = autenticadorReview.gerarTokenReview(IdentidadeSintetica.REVIEW_GOOGLE_MEMBRO);
        String tokenAdulterado = tokenValido.substring(0, tokenValido.length() - 5) + "XXXXX";

        ResponseEntity<byte[]> response = carteiraController.fotoServidor(tokenAdulterado);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("13. cross-profile nao permite trocar arbitrariamente a foto")
    void test13_crossProfile_naoPermiteTrocarArbitrariamenteAFoto() {
        String tokenMembro = autenticadorReview.gerarTokenReview(IdentidadeSintetica.REVIEW_GOOGLE_MEMBRO);
        autenticarComo("membro.google", tokenMembro);

        ResponseEntity<byte[]> responseMembro = carteiraController.fotoServidor(null);
        assertEquals(HttpStatus.OK, responseMembro.getStatusCode());
        assertArrayEquals(dataService.getFotoReviewMembro(), responseMembro.getBody());
        assertFalse(Arrays.equals(dataService.getFotoReviewServidor(), responseMembro.getBody()));

        String tokenServidor = autenticadorReview.gerarTokenReview(IdentidadeSintetica.REVIEW_APPLE_SERVIDOR);
        autenticarComo("servidor.apple", tokenServidor);

        ResponseEntity<byte[]> responseServidor = carteiraController.fotoServidor(null);
        assertEquals(HttpStatus.OK, responseServidor.getStatusCode());
        assertArrayEquals(dataService.getFotoReviewServidor(), responseServidor.getBody());
        assertFalse(Arrays.equals(dataService.getFotoReviewMembro(), responseServidor.getBody()));
    }

    @Test
    @DisplayName("14. ausencia do recurso sintetico nunca provoca fallback para dado real")
    void test14_ausenciaDoRecursoSintetico_nuncaProvocaFallbackParaDadoReal() {
        // Criando CarteiraService com dataService sem imagem (retornando null)
        SyntheticDataService dataServiceSemImagem = new SyntheticDataService(autenticadorUat, auditoria, autenticadorReview) {
            @Override
            public byte[] getFotoReviewMembro() {
                return null;
            }
        };

        countingJdbcTemplate.queriesCount = 0;
        CarteiraService serviceSemImagem = new CarteiraService(countingJdbcTemplate, null, dataServiceSemImagem);
        byte[] foto = serviceSemImagem.getBytesFotoServidorPorLogin("membro.google");

        assertNull(foto, "Ausência da foto sintética deve retornar null controlado");
        assertEquals(0, countingJdbcTemplate.queriesCount, "Nenhuma consulta JDBC para buscar foto institucional real");

        // Controller deve retornar 404 Not Found, nunca imagem institucional
        CarteiraFuncionalController controllerSemImagem = new CarteiraFuncionalController(serviceSemImagem);
        String tokenMembro = autenticadorReview.gerarTokenReview(IdentidadeSintetica.REVIEW_GOOGLE_MEMBRO);
        autenticarComo("membro.google", tokenMembro);

        ResponseEntity<byte[]> response = controllerSemImagem.fotoServidor(null);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("15. uat.membro e uat.servidor retornam fotos humanas sintéticas válidas")
    void test15_uatMembroEUatServidor_retornamFotosHumanasSinteticasValidas() {
        byte[] fotoUatMembro = dataService.getFotoByLogin("uat.membro");
        assertNotNull(fotoUatMembro);
        assertTrue(fotoUatMembro.length > 1000, "Foto UAT Membro deve ser foto humana real (> 1000 bytes)");
        assertEquals((byte) 0xFF, fotoUatMembro[0]);
        assertEquals((byte) 0xD8, fotoUatMembro[1]);

        byte[] fotoUatServidor = dataService.getFotoByLogin("uat.servidor");
        assertNotNull(fotoUatServidor);
        assertTrue(fotoUatServidor.length > 1000, "Foto UAT Servidor deve ser foto humana real (> 1000 bytes)");
        assertEquals((byte) 0xFF, fotoUatServidor[0]);
        assertEquals((byte) 0xD8, fotoUatServidor[1]);

        assertFalse(Arrays.equals(fotoUatMembro, fotoUatServidor), "Fotos de membro e servidor em UAT devem ser distintas");
    }

    @Test
    @DisplayName("16. todas as 6 identidades sintéticas cobertas com fotos válidas")
    void test16_todasAsSeisIdentidadesSinteticas_cobertasComFotosValidas() {
        List<String> logins = List.of(
                "membro.google",
                "servidor.google",
                "membro.apple",
                "servidor.apple",
                "uat.membro",
                "uat.servidor"
        );

        for (String login : logins) {
            byte[] foto = carteiraService.getBytesFotoServidorPorLogin(login);
            assertNotNull(foto, "Foto não pode ser nula para: " + login);
            assertTrue(foto.length > 1000, "Foto deve possuir tamanho JPEG válido para: " + login);
            assertEquals((byte) 0xFF, foto[0]);
            assertEquals((byte) 0xD8, foto[1]);
        }
    }

    @Test
    @DisplayName("17. controller retorna HTTP 200 com Content-Type JPEG e RawContentLength > 0")
    void test17_controllerRetornaHttp200ComContentTypeJpegERawContentLength() {
        // Teste para Membro Review
        String tokenMembro = autenticadorReview.gerarTokenReview(IdentidadeSintetica.REVIEW_GOOGLE_MEMBRO);
        autenticarComo("membro.google", tokenMembro);

        ResponseEntity<byte[]> respMembro = carteiraController.fotoServidor(null);
        assertEquals(HttpStatus.OK, respMembro.getStatusCode());
        assertNotNull(respMembro.getBody());
        assertTrue(respMembro.getBody().length > 1000);

        // Teste para Servidor Review
        String tokenServidor = autenticadorReview.gerarTokenReview(IdentidadeSintetica.REVIEW_GOOGLE_SERVIDOR);
        autenticarComo("servidor.google", tokenServidor);

        ResponseEntity<byte[]> respServidor = carteiraController.fotoServidor(null);
        assertEquals(HttpStatus.OK, respServidor.getStatusCode());
        assertNotNull(respServidor.getBody());
        assertTrue(respServidor.getBody().length > 1000);

        assertFalse(Arrays.equals(respMembro.getBody(), respServidor.getBody()));
    }

    private static class CountingJdbcTemplate extends JdbcTemplate {
        int queriesCount = 0;

        @Override
        public <T> List<T> query(String sql, Object[] args, RowMapper<T> rowMapper) {
            queriesCount++;
            return List.of();
        }

        @Override
        public <T> List<T> query(String sql, RowMapper<T> rowMapper) {
            queriesCount++;
            return List.of();
        }
    }
}
