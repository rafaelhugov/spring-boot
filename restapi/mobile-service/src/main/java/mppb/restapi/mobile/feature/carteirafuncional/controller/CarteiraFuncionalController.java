package mppb.restapi.mobile.feature.carteirafuncional.controller;

import mppb.restapi.mobile.feature.carteirafuncional.dto.CarteiraFuncionalTO;
import mppb.restapi.mobile.feature.carteirafuncional.service.CarteiraService;

import mppb.restapi.common.infrastructure.util.Constants;
import mppb.restapi.mobile.core.security.AutenticacaoAtual;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.io.InputStream;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping({"/carteiras-funcionais", "/" + Constants.API_PREFIX + "/carteiras-funcionais"})
@RequiredArgsConstructor
public class CarteiraFuncionalController {

    private final CarteiraService carteiraService;


    @GetMapping(value="/assinatura-para-servidor", produces= MediaType.IMAGE_PNG_VALUE)
    public byte[] dadosAssinaturaParaServidor() {
        return getBytesArquivoClasspath("reports/carteira-funcional/img/assinaturapgj_black.png");
    }

    @GetMapping(value="/assinatura-para-membro", produces= MediaType.IMAGE_PNG_VALUE)
    public byte[] dadosAssinaturaParaMembro() {
        return getBytesArquivoClasspath("reports/carteira-funcional/img/assinaturapgj_white.png");
    }

    @GetMapping(value="/public/logo", produces= MediaType.IMAGE_PNG_VALUE)
    public byte[] dadosLogoPublico() {
        return getBytesArquivoClasspath("reports/carteira-funcional/img/brasao_mppb_sem_fundo.png");
    }

    private byte[] getBytesArquivoClasspath(String path) {
        try (InputStream inputStream = new ClassPathResource(path).getInputStream()) {
            return inputStream.readAllBytes();
        } catch(Exception e) {
            log.error("Erro ao recuperar imagem no classpath: " + path, e);
            return null;
        }
    }



    @GetMapping("/dados")
    public ResponseEntity<?> dadosCarteira() {
        try {
            String login = AutenticacaoAtual.login().orElse(null);
            if (login == null) {
                return ResponseEntity.status(401).build(); // Unauthorized
            }

            CarteiraFuncionalTO carteira = carteiraService.recuperaCarteiraFuncionalByLogin(login);
            if (carteira == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(carteira);
        } catch (Exception e) {
            log.error("Erro interno ao buscar dados da carteira", e);
            return ResponseEntity.status(500).body("Não foi possível carregar a carteira funcional.");
        }
    }

    @GetMapping(value = "/foto", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> fotoServidor() {
        try {
            String login = AutenticacaoAtual.login().orElse(null);
            if (login == null) {
                return ResponseEntity.status(401).build();
            }

            byte[] bytes = carteiraService.getBytesFotoServidorPorLogin(login);
            if (bytes == null || bytes.length == 0) {
                bytes = getBytesArquivoClasspath("reports/carteira-funcional/img/nofoto.jpg");
                if (bytes == null) {
                    return ResponseEntity.notFound().build();
                }
            }

            return ResponseEntity.ok(bytes);
        } catch (Exception e) {
            log.error("Erro ao buscar foto da carteira", e);
            return ResponseEntity.status(500).build();
        }
    }



    @GetMapping(value = "/public/foto/{md5}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> fotoPublicaServidor(@PathVariable("md5") String md5) {
        try {
            CarteiraFuncionalTO carteira = carteiraService.recuperaCarteiraFuncionalByHash(md5);
            if (carteira == null) {
                return ResponseEntity.notFound().build();
            }

            byte[] bytes = carteiraService.getBytesFotoServidorPorCarteira(carteira);
            if (bytes == null || bytes.length == 0) {
                bytes = getBytesArquivoClasspath("reports/carteira-funcional/img/nofoto.jpg");
                if (bytes == null) {
                    return ResponseEntity.notFound().build();
                }
            }

            return ResponseEntity.ok(bytes);
        } catch (Exception e) {
            log.error("Erro ao buscar foto publica pelo md5: " + md5, e);
            return ResponseEntity.status(500).build();
        }
    }


}
