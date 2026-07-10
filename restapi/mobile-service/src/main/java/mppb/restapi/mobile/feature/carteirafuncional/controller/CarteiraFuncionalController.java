package mppb.restapi.mobile.feature.carteirafuncional.controller;

import mppb.restapi.mobile.feature.carteirafuncional.dto.CarteiraFuncionalTO;
import mppb.restapi.mobile.feature.carteirafuncional.service.CarteiraService;

import mppb.restapi.common.infrastructure.client.authapi.AuthServiceClient;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoRequestDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoResponseDto;
import mppb.restapi.common.infrastructure.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import jakarta.validation.constraints.NotNull;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import javax.imageio.ImageIO;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping({"/carteiras-funcionais", "/" + Constants.API_PREFIX + "/carteiras-funcionais"})
@RequiredArgsConstructor
public class CarteiraFuncionalController {

    private final CarteiraService carteiraService;
    private final AuthServiceClient authServiceClient;


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
    public ResponseEntity<?> dadosCarteira(@RequestParam("token") @NotNull String token) {
        try {
            String login = getLoginByToken(token);
            if (login == null) {
                return ResponseEntity.status(401).build(); // Unauthorized
            }

            CarteiraFuncionalTO carteira = carteiraService.recuperaCarteiraFuncionalByLogin(login);
            if (carteira == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(carteira);
        } catch (Exception e) {
            log.error("Erro interno ao buscar dados da carteira para o token: " + token, e);
            return ResponseEntity.status(500).body("Erro interno do servidor: " + e.getMessage());
        }
    }

    @GetMapping(value = "/foto", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> fotoServidor(@RequestParam("token") @NotNull String token) {
        try {
            String login = getLoginByToken(token);
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
            log.error("Erro ao buscar foto da carteira para o token: " + token, e);
            return ResponseEntity.status(500).build();
        }
    }

    private String getLoginByToken(String token) {
        ConsultaAutorizacaoRequestDto req = new ConsultaAutorizacaoRequestDto(token, "ANDROID");

        ConsultaAutorizacaoResponseDto autorizacao = null;
        try {
            autorizacao = authServiceClient.consultaAutorizacao(req);
        } catch (Exception e) {
            log.error("Erro ao consultar autorizacao no auth-service", e);
        }

        if (autorizacao == null || autorizacao.getUsuario() == null || autorizacao.getUsuario().getLogin() == null) {
            return null; 
        }
        
        return autorizacao.getUsuario().getLogin();
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
