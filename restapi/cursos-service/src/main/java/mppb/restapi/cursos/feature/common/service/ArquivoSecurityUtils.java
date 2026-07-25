package mppb.restapi.cursos.feature.common.service;

import mppb.restapi.common.core.error.BusinessRuleException;
import mppb.restapi.cursos.feature.common.entities.Arquivo;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ArquivoSecurityUtils {

    public static final long MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L;
    public static final String HEADER_NOSNIFF = "X-Content-Type-Options";

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg");
    private static final Map<String, String> EXTENSION_TO_MIME = Map.of(
            "pdf", MediaType.APPLICATION_PDF_VALUE,
            "png", MediaType.IMAGE_PNG_VALUE,
            "jpg", MediaType.IMAGE_JPEG_VALUE,
            "jpeg", MediaType.IMAGE_JPEG_VALUE
    );

    private ArquivoSecurityUtils() {
    }

    public static ValidatedUpload validateUpload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Selecione um arquivo para envio.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessRuleException("Arquivo excede o tamanho máximo permitido.");
        }

        String safeName = sanitizeFileName(file.getOriginalFilename());
        String extension = extensionOf(safeName);
        validateDeclaredMime(file.getContentType(), extension);

        byte[] bytes = file.getBytes();
        String detectedMime = detectMime(bytes);
        String expectedMime = EXTENSION_TO_MIME.get(extension);
        if (!expectedMime.equals(detectedMime)) {
            throw new BusinessRuleException("Conteúdo do arquivo não corresponde ao tipo permitido.");
        }

        return new ValidatedUpload(safeName, expectedMime, bytes.length, bytes);
    }

    public static String sanitizeFileName(String originalName) {
        String name = originalName == null ? "" : originalName.trim();
        if (name.isEmpty()) {
            throw new BusinessRuleException("Nome de arquivo inválido.");
        }
        if (name.contains("/") || name.contains("\\") || name.contains("..") || containsControlChar(name)) {
            throw new BusinessRuleException("Nome de arquivo inválido.");
        }

        String sanitized = name.replaceAll("[^A-Za-z0-9._ -]", "_")
                .replaceAll("\\s+", " ")
                .trim();
        if (sanitized.isEmpty()) {
            throw new BusinessRuleException("Nome de arquivo inválido.");
        }
        if (sanitized.indexOf('.') != sanitized.lastIndexOf('.')) {
            throw new BusinessRuleException("Extensão dupla não é permitida.");
        }
        if (!ALLOWED_EXTENSIONS.contains(extensionOf(sanitized))) {
            throw new BusinessRuleException("Extensão de arquivo não permitida.");
        }
        return sanitized;
    }

    public static ResponseEntity<byte[]> secureDownloadResponse(Arquivo arquivo, boolean publicCache) {
        String fileName = sanitizeDownloadName(arquivo.getNomeArquivo());
        MediaType contentType = safeMediaType(arquivo.getMimeType());

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .header(HEADER_NOSNIFF, "nosniff")
                .cacheControl(publicCache
                        ? CacheControl.maxAge(Duration.ofHours(1)).cachePublic()
                        : CacheControl.noStore())
                .body(arquivo.getBytes());
    }

    static String sanitizeDownloadName(String originalName) {
        try {
            return sanitizeFileName(originalName);
        } catch (BusinessRuleException exception) {
            return "arquivo.bin";
        }
    }

    private static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0 || dot == fileName.length() - 1) {
            throw new BusinessRuleException("Extensão de arquivo não permitida.");
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static void validateDeclaredMime(String declaredMime, String extension) {
        String expectedMime = EXTENSION_TO_MIME.get(extension);
        String normalized = declaredMime == null ? "" : declaredMime.toLowerCase(Locale.ROOT);
        if (!expectedMime.equals(normalized)) {
            throw new BusinessRuleException("MIME type declarado não corresponde à extensão.");
        }
    }

    private static MediaType safeMediaType(String mimeType) {
        if (mimeType == null) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        String normalized = mimeType.toLowerCase(Locale.ROOT);
        if (EXTENSION_TO_MIME.containsValue(normalized)) {
            return MediaType.parseMediaType(normalized);
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private static boolean containsControlChar(String value) {
        return value.chars().anyMatch(ch -> ch < 32 || ch == 127);
    }

    private static String detectMime(byte[] bytes) {
        if (bytes.length >= 5
                && bytes[0] == 0x25
                && bytes[1] == 0x50
                && bytes[2] == 0x44
                && bytes[3] == 0x46
                && bytes[4] == 0x2D) {
            return MediaType.APPLICATION_PDF_VALUE;
        }
        if (bytes.length >= 8
                && (bytes[0] & 0xff) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A) {
            return MediaType.IMAGE_PNG_VALUE;
        }
        if (bytes.length >= 3
                && (bytes[0] & 0xff) == 0xff
                && (bytes[1] & 0xff) == 0xd8
                && (bytes[2] & 0xff) == 0xff) {
            return MediaType.IMAGE_JPEG_VALUE;
        }
        return "application/octet-stream";
    }

    public record ValidatedUpload(String safeName, String mimeType, long size, byte[] bytes) {
    }
}
