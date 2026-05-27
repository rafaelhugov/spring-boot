package mppb.restapi.cursos.feature.carteirafuncional.controller;

import mppb.restapi.cursos.feature.carteirafuncional.dto.CarteiraFuncionalTO;
import mppb.restapi.cursos.feature.carteirafuncional.service.CarteiraService;
import mppb.restapi.cursos.feature.carteirafuncional.service.GoogleWalletService;
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
    private final GoogleWalletService googleWalletService;

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

    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> dadosCarteiraPdf(@RequestParam("token") @NotNull String token) {
        String login = getLoginByToken(token);
        if (login == null) {
            return ResponseEntity.status(401).build(); // Unauthorized
        }

        CarteiraFuncionalTO carteira = carteiraService.recuperaCarteiraFuncionalByLogin(login);
        if (carteira == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] bytes = carteiraService.getDadosCarteiraPDF(login, carteira);
            return ResponseEntity.ok(bytes);
        } catch (Exception e) {
            log.error("Erro ao gerar PDF da carteira funcional", e);
            return ResponseEntity.internalServerError().build();
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

    @GetMapping("/google-wallet")
    public ResponseEntity<String> obterLinkGoogleWallet(@RequestParam("token") @NotNull String token) {
        try {
            String login = getLoginByToken(token);
            if (login == null) {
                return ResponseEntity.status(401).build();
            }

            CarteiraFuncionalTO carteira = carteiraService.recuperaCarteiraFuncionalByLogin(login);
            if (carteira == null) {
                return ResponseEntity.notFound().build();
            }

            String link = googleWalletService.gerarLinkGoogleWallet(carteira);
            return ResponseEntity.ok(link);
        } catch (Exception e) {
            log.error("Erro ao gerar link do Google Wallet para o token: " + token, e);
            return ResponseEntity.status(500).body("Erro ao gerar passe do Google Wallet: " + e.getMessage());
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

    @GetMapping(value = "/public/banner/{md5}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> gerarBannerPublico(@PathVariable("md5") String md5) {
        try {
            CarteiraFuncionalTO carteira = carteiraService.recuperaCarteiraFuncionalByHash(md5);
            if (carteira == null) {
                return ResponseEntity.notFound().build();
            }

            boolean isMembro = !"S".equals(carteira.getTipoCargo());

            // Criar imagem de 1032x336 (tamanho padrão de Hero Image do Google Wallet)
            BufferedImage img = new BufferedImage(1032, 336, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Cores de fundo (Membro = Burgundy, Servidor = Beige)
            if (isMembro) {
                g2.setColor(new Color(90, 11, 11)); // Burgundy #5A0B0B
            } else {
                g2.setColor(new Color(232, 220, 209)); // Beige #E8DCD1
            }
            g2.fillRect(0, 0, 1032, 336);

            // Texturas geométricas sutis (padrão de segurança)
            if (isMembro) {
                g2.setColor(new Color(255, 255, 255, 12));
            } else {
                g2.setColor(new Color(0, 0, 0, 12));
            }
            for (int i = 0; i < 1032; i += 30) {
                g2.drawLine(i, 0, i + 100, 336);
            }

            // Marca d'água (Brasão do MPPB de fundo centralizado)
            byte[] bytesMarcaDagua = getBytesArquivoClasspath("reports/carteira-funcional/img/brasao_mppb_sem_fundo.png");
            if (bytesMarcaDagua != null && bytesMarcaDagua.length > 0) {
                try (InputStream in = new ByteArrayInputStream(bytesMarcaDagua)) {
                    BufferedImage logoDagua = ImageIO.read(in);
                    if (logoDagua != null) {
                        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, isMembro ? 0.04f : 0.03f));
                        g2.drawImage(logoDagua, (1032 - 280) / 2, (336 - 280) / 2, 280, 280, null);
                        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f)); // Reset opacity
                    }
                }
            }

            // Desenhar Logo (Brasão do MPPB ou República no topo esquerdo)
            String pathLogo = isMembro ? "reports/carteira-funcional/img/Coat_of_arms_of_Brazil.svg.png" : "reports/carteira-funcional/img/brasao_mppb_sem_fundo.png";
            byte[] bytesLogo = getBytesArquivoClasspath(pathLogo);
            if (bytesLogo != null && bytesLogo.length > 0) {
                try (InputStream in = new ByteArrayInputStream(bytesLogo)) {
                    BufferedImage logoImg = ImageIO.read(in);
                    if (logoImg != null) {
                        g2.drawImage(logoImg, 60, 35, 64, 64, null);
                    }
                }
            }

            // Desenhar Título do Cabeçalho
            g2.setColor(isMembro ? Color.WHITE : new Color(62, 39, 35)); // #3E2723
            g2.setFont(new Font("SansSerif", Font.BOLD, 22));
            g2.drawString("MINISTÉRIO PÚBLICO DA PARAÍBA", 140, 62);

            g2.setColor(isMembro ? new Color(212, 175, 55) : new Color(109, 76, 65)); // Dourado vs Marrom #6D4C41
            g2.setFont(new Font("SansSerif", Font.BOLD, 17));
            g2.drawString("CARTEIRA DE IDENTIDADE FUNCIONAL", 140, 88);

            // Badge MEMBRO ou SERVIDOR
            int badgeX = 60;
            int badgeY = 120;
            int badgeW = 120;
            int badgeH = 26;
            g2.setColor(isMembro ? new Color(212, 175, 55) : new Color(228, 213, 205)); // Dourado vs Creme #E4D5CD
            g2.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 10, 10);
            
            g2.setColor(isMembro ? new Color(90, 11, 11) : new Color(62, 39, 35));
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            String textoBadge = isMembro ? "MEMBRO" : "SERVIDOR";
            FontMetrics fm = g2.getFontMetrics();
            int textX = badgeX + (badgeW - fm.stringWidth(textoBadge)) / 2;
            int textY = badgeY + (badgeH - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(textoBadge, textX, textY);

            // Nome e Cargo
            g2.setColor(isMembro ? Color.WHITE : new Color(62, 39, 35));
            g2.setFont(new Font("SansSerif", Font.BOLD, 28));
            g2.drawString(carteira.getNome(), 60, 185);

            g2.setColor(isMembro ? new Color(212, 175, 55) : new Color(109, 76, 65));
            g2.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2.drawString(carteira.getCargo(), 60, 215);

            // Linha divisória
            g2.setColor(isMembro ? new Color(255, 255, 255, 50) : new Color(0, 0, 0, 30));
            g2.setStroke(new BasicStroke(1));
            g2.drawLine(60, 235, 700, 235);

            // Matrícula e Emissão
            // Rótulo MATRÍCULA
            g2.setColor(isMembro ? new Color(255, 255, 255, 160) : new Color(109, 76, 65));
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2.drawString("MATRÍCULA", 60, 260);
            
            // Valor MATRÍCULA
            g2.setColor(isMembro ? Color.WHITE : new Color(62, 39, 35));
            g2.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2.drawString(carteira.getMatricula(), 60, 285);

            // Rótulo EMISSÃO
            g2.setColor(isMembro ? new Color(255, 255, 255, 160) : new Color(109, 76, 65));
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2.drawString("EMISSÃO", 240, 260);

            // Valor EMISSÃO
            g2.setColor(isMembro ? Color.WHITE : new Color(62, 39, 35));
            g2.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2.drawString("22/05/2026", 240, 285);

            // Carregar e desenhar a foto do usuário à direita (alinhada)
            byte[] bytesFoto = carteiraService.getBytesFotoServidorPorCarteira(carteira);
            if (bytesFoto == null || bytesFoto.length == 0) {
                bytesFoto = getBytesArquivoClasspath("reports/carteira-funcional/img/nofoto.jpg");
            }

            if (bytesFoto != null && bytesFoto.length > 0) {
                try (InputStream in = new ByteArrayInputStream(bytesFoto)) {
                    BufferedImage fotoOriginal = ImageIO.read(in);
                    if (fotoOriginal != null) {
                        int size = Math.min(fotoOriginal.getWidth(), fotoOriginal.getHeight());
                        BufferedImage squareFoto = fotoOriginal.getSubimage(0, 0, size, size);
                        
                        // Desenhar a foto (tamanho proporcional na direita)
                        int fotoX = 740;
                        int fotoY = 45;
                        int fotoW = 210;
                        int fotoH = 246;
                        g2.drawImage(squareFoto, fotoX, fotoY, fotoW, fotoH, null);
                        
                        // Moldura/borda da foto (dourada para membro, creme para servidor)
                        g2.setColor(isMembro ? new Color(212, 175, 55) : new Color(228, 213, 205));
                        g2.setStroke(new BasicStroke(3));
                        g2.drawRoundRect(fotoX, fotoY, fotoW, fotoH, 8, 8);
                    }
                }
            }

            g2.dispose();

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(img, "png", baos);
                return ResponseEntity.ok(baos.toByteArray());
            }
        } catch (Exception e) {
            log.error("Erro ao gerar banner dinâmico para Google Wallet: ", e);
            return ResponseEntity.status(500).build();
        }
    }
}
