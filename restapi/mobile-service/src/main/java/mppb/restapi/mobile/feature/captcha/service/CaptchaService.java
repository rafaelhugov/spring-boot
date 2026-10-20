package mppb.restapi.mobile.feature.captcha.service;

import mppb.restapi.mobile.feature.captcha.dto.CaptchaResponseDTO;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;

@Service
public class CaptchaService {

    static {
        System.setProperty("java.awt.headless", "true");
        javax.imageio.ImageIO.setUseCache(false);
    }

    private static class CaptchaInfo {
        String text;
        long expiryTime;
        CaptchaInfo(String text, long expiryTime) {
            this.text = text;
            this.expiryTime = expiryTime;
        }
    }

    // Armazena captchas na memória, expira em 5 minutos.
    private final Map<String, CaptchaInfo> captchaStore = new LinkedHashMap<>();
    private static final long CAPTCHA_EXPIRATION_MS = 5 * 60 * 1000;
    private final SecureRandom random = new SecureRandom();

    @Value("${security.public-api.captcha.max-pending:5000}")
    private int maxPendingCaptchas = 5000;

    public synchronized CaptchaResponseDTO generateCaptcha() {
        String token = UUID.randomUUID().toString();
        String text = generateRandomText(6);

        cleanUpExpired();
        int capacidade = Math.max(1, maxPendingCaptchas);
        while (captchaStore.size() >= capacidade && !captchaStore.isEmpty()) {
            Iterator<String> oldest = captchaStore.keySet().iterator();
            oldest.next();
            oldest.remove();
        }

        captchaStore.put(token, new CaptchaInfo(text, System.currentTimeMillis() + CAPTCHA_EXPIRATION_MS));

        String base64Image = createCaptchaImageBase64(text);
        return new CaptchaResponseDTO(token, "data:image/png;base64," + base64Image);
    }

    public synchronized boolean validateCaptcha(String token, String text) {
        if (token == null || text == null) return false;
        
        CaptchaInfo info = captchaStore.get(token);
        if (info == null) return false;

        // Limpa independentemente se acerta ou erra para uso único
        captchaStore.remove(token);

        if (System.currentTimeMillis() > info.expiryTime) {
            return false;
        }

        byte[] expected = info.text.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        byte[] actual = text.trim().toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    private void cleanUpExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, CaptchaInfo>> it = captchaStore.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, CaptchaInfo> entry = it.next();
            if (now > entry.getValue().expiryTime) {
                it.remove();
            }
        }
    }

    private String generateRandomText(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789"; // Removed confusable like 1, i, l, 0, o, O
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String createCaptchaImageBase64(String text) {
        int width = 180;
        int height = 50;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Background
        g2d.setColor(new Color(245, 245, 245));
        g2d.fillRect(0, 0, width, height);

        // Noise lines
        g2d.setColor(new Color(180, 180, 180));
        for (int i = 0; i < 15; i++) {
            int x1 = random.nextInt(width);
            int y1 = random.nextInt(height);
            int x2 = random.nextInt(width);
            int y2 = random.nextInt(height);
            g2d.drawLine(x1, y1, x2, y2);
        }

        // Draw Text
        Font font = new Font("Courier", Font.BOLD, 36);
        g2d.setFont(font);

        int x = 15;
        for (int i = 0; i < text.length(); i++) {
            AffineTransform affineTransform = new AffineTransform();
            affineTransform.rotate(Math.toRadians(random.nextInt(30) - 15), 0, 0);
            Font rotatedFont = font.deriveFont(affineTransform);
            g2d.setFont(rotatedFont);

            // Random color for text
            g2d.setColor(new Color(random.nextInt(100), random.nextInt(100), random.nextInt(100)));
            g2d.drawString(String.valueOf(text.charAt(i)), x, 38 + random.nextInt(10) - 5);
            x += 25;
        }

        g2d.dispose();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            throw new RuntimeException("Error generating Captcha image", e);
        }
    }
}
