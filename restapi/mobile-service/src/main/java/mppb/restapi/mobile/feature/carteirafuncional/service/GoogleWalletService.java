package mppb.restapi.cursos.feature.carteirafuncional.service;

import mppb.restapi.cursos.feature.carteirafuncional.dto.CarteiraFuncionalTO;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.walletobjects.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

@Slf4j
@Service
public class GoogleWalletService {

    @Value("${google.wallet.issuer-id:}")
    private String issuerId;

    @Value("${google.wallet.class-id:}")
    private String classId;

    @Value("${google.wallet.credentials-path:google-credentials.json}")
    private String credentialsPath;

    @Value("${google.wallet.public-base-url:http://localhost:8086}")
    private String publicBaseUrl;

    @Value("${google.wallet.origins:}")
    private String walletOrigins;

    public String gerarLinkGoogleWallet(CarteiraFuncionalTO carteira) {
        try {
            Map<String, String> creds = carregarCredenciais();
            String serviceAccountEmail = creds.get("client_email");
            RSAPrivateKey privateKey = parsePrivateKey(creds.get("private_key"));

            String objectId = issuerId + "." + UUID.randomUUID().toString().replace("-", "");

            String logoUrl = publicBaseUrl + "/carteiras-funcionais/public/logo";
            String heroImageUrl = publicBaseUrl + "/carteiras-funcionais/public/banner/" + carteira.getMd5();
            String fotoUrl = publicBaseUrl + "/carteiras-funcionais/public/foto/" + carteira.getMd5();

            GenericObject genericObject = new GenericObject()
                    .setId(objectId)
                    .setClassId(classId)
                    .setState("ACTIVE")
                    .setCardTitle(new LocalizedString().setDefaultValue(new TranslatedString().setLanguage("pt-BR").setValue("Ministério Público da Paraíba")))
                    .setHeader(new LocalizedString().setDefaultValue(new TranslatedString().setLanguage("pt-BR").setValue(carteira.getNome())))
                    .setSubheader(new LocalizedString().setDefaultValue(new TranslatedString().setLanguage("pt-BR").setValue(carteira.getCargo())))
                    .setLogo(criarImagem(logoUrl, "Brasão do Ministério Público da Paraíba"))
                    .setHeroImage(criarImagem(heroImageUrl, "Carteira funcional do Ministério Público da Paraíba"))
                    .setBarcode(new Barcode()
                            .setType("QR_CODE")
                            .setValue("https://mobile-api.mppb.mp.br/public/validar-carteira/" + carteira.getMd5())
                            .setAlternateText(carteira.getMatricula()));

            List<TextModuleData> textModules = new ArrayList<>();
            textModules.add(new TextModuleData().setHeader("NOME").setBody(carteira.getNome()));
            textModules.add(new TextModuleData().setHeader("CARGO").setBody(carteira.getCargo()));
            textModules.add(new TextModuleData().setHeader("MATRÍCULA").setBody(carteira.getMatricula()));
            genericObject.setTextModulesData(textModules);

            ImageModuleData imageModule = new ImageModuleData()
                    .setId("foto_usuario")
                    .setMainImage(criarImagem(fotoUrl, "Foto do titular da carteira funcional"));
            genericObject.setImageModulesData(Collections.singletonList(imageModule));

            GenericClass genericClass = new GenericClass()
                    .setId(classId);
            genericClass.set("issuerName", "Ministério Público da Paraíba");

            Map<String, Object> payload = new HashMap<>();
            
            Map<String, Object> genericObjectsMap = new HashMap<>();
            genericObjectsMap.put("genericClasses", Collections.singletonList(genericClass));
            genericObjectsMap.put("genericObjects", Collections.singletonList(genericObject));
            
            payload.put("iss", serviceAccountEmail);
            payload.put("aud", "google");
            payload.put("typ", "savetowallet");
            payload.put("iat", System.currentTimeMillis() / 1000);
            payload.put("origins", recuperarOrigins());
            payload.put("payload", genericObjectsMap);

            String privateKeyId = creds.get("private_key_id");
            Algorithm algorithm = Algorithm.RSA256(null, privateKey);
            String token = JWT.create()
                    .withHeader(Collections.singletonMap("kid", privateKeyId))
                    .withPayload(payload)
                    .sign(algorithm);

            return "https://pay.google.com/gp/v/save/" + token;
        } catch (Exception e) {
            log.error("Erro ao gerar link do Google Wallet", e);
            throw new RuntimeException("Erro ao gerar passe do Google Wallet", e);
        }
    }

    private Image criarImagem(String url, String descricao) {
        return new Image()
                .setSourceUri(new ImageUri().setUri(url))
                .setContentDescription(new LocalizedString()
                        .setDefaultValue(new TranslatedString()
                                .setLanguage("pt-BR")
                                .setValue(descricao)));
    }

    private List<String> recuperarOrigins() {
        LinkedHashSet<String> origins = new LinkedHashSet<>();
        adicionarOriginsConfiguradas(origins);
        adicionarOriginDaUrl(origins, publicBaseUrl);

        if (origins.isEmpty()) {
            origins.add("http://localhost:8080");
        }

        return new ArrayList<>(origins);
    }

    private void adicionarOriginsConfiguradas(Set<String> origins) {
        if (walletOrigins == null || walletOrigins.isBlank()) {
            return;
        }

        Arrays.stream(walletOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .forEach(origins::add);
    }

    private void adicionarOriginDaUrl(Set<String> origins, String url) {
        if (url == null || url.isBlank()) {
            return;
        }

        try {
            java.net.URI uri = java.net.URI.create(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                return;
            }

            StringBuilder origin = new StringBuilder()
                    .append(scheme)
                    .append("://")
                    .append(host);
            if (uri.getPort() > -1) {
                origin.append(":").append(uri.getPort());
            }
            origins.add(origin.toString());
        } catch (IllegalArgumentException e) {
            log.warn("URL pública inválida para origins do Google Wallet: {}", url);
        }
    }

    private Map<String, String> carregarCredenciais() throws Exception {
        InputStream is;
        if (credentialsPath.startsWith("classpath:")) {
            String path = credentialsPath.substring(10);
            is = new ClassPathResource(path).getInputStream();
        } else {
            is = new FileInputStream(credentialsPath);
        }
        try (is) {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(is, Map.class);
        }
    }

    private RSAPrivateKey parsePrivateKey(String pemKey) throws Exception {
        String privateKeyPem = pemKey
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        
        byte[] decoded = Base64.getDecoder().decode(privateKeyPem);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) kf.generatePrivate(spec);
    }
}
