package mppb.restapi.mobile.feature.mensagem.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import mppb.restapi.mobile.feature.mensagem.entity.MensagemAnexo;
import mppb.restapi.mobile.feature.mensagem.enums.CategoriaAnexo;

@Service
@RequiredArgsConstructor
public class MensagemStorageService {

    public record ArquivoArmazenado(
            CategoriaAnexo categoria,
            String nomeOriginal,
            String mimeType,
            long tamanhoBytes,
            String caminhoStorage,
            byte[] conteudoBytes) {
    }

    private static final Map<String, CategoriaAnexo> EXTENSOES_PERMITIDAS = Map.ofEntries(
            Map.entry("jpg", CategoriaAnexo.IMAGEM),
            Map.entry("jpeg", CategoriaAnexo.IMAGEM),
            Map.entry("png", CategoriaAnexo.IMAGEM),
            Map.entry("webp", CategoriaAnexo.IMAGEM),
            Map.entry("heic", CategoriaAnexo.IMAGEM),
            Map.entry("mp4", CategoriaAnexo.VIDEO),
            Map.entry("mov", CategoriaAnexo.VIDEO),
            Map.entry("webm", CategoriaAnexo.VIDEO),
            Map.entry("m4a", CategoriaAnexo.AUDIO),
            Map.entry("aac", CategoriaAnexo.AUDIO),
            Map.entry("ogg", CategoriaAnexo.AUDIO),
            Map.entry("opus", CategoriaAnexo.AUDIO),
            Map.entry("mp3", CategoriaAnexo.AUDIO),
            Map.entry("wav", CategoriaAnexo.AUDIO),
            Map.entry("pdf", CategoriaAnexo.DOCUMENTO),
            Map.entry("doc", CategoriaAnexo.DOCUMENTO),
            Map.entry("docx", CategoriaAnexo.DOCUMENTO),
            Map.entry("xls", CategoriaAnexo.DOCUMENTO),
            Map.entry("xlsx", CategoriaAnexo.DOCUMENTO),
            Map.entry("ppt", CategoriaAnexo.DOCUMENTO),
            Map.entry("pptx", CategoriaAnexo.DOCUMENTO),
            Map.entry("odt", CategoriaAnexo.DOCUMENTO),
            Map.entry("ods", CategoriaAnexo.DOCUMENTO),
            Map.entry("odp", CategoriaAnexo.DOCUMENTO),
            Map.entry("txt", CategoriaAnexo.DOCUMENTO),
            Map.entry("csv", CategoriaAnexo.DOCUMENTO),
            Map.entry("md", CategoriaAnexo.DOCUMENTO));

    private static final Set<String> MIME_PREFIXES_VALIDOS = Set.of(
            "image/", "video/", "audio/", "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument", "application/vnd.ms-",
            "application/vnd.oasis.opendocument", "text/plain", "text/csv", "text/markdown",
            "application/octet-stream");

    private final MensagemStorageProperties properties;

    public ArquivoArmazenado armazenar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("O arquivo enviado está vazio.");
        }

        String nomeOriginal = limparNomeArquivo(arquivo.getOriginalFilename());
        String extensao = extrairExtensao(nomeOriginal);
        CategoriaAnexo categoria = EXTENSOES_PERMITIDAS.get(extensao);
        if (categoria == null) {
            throw new IllegalArgumentException("Extensão de arquivo não permitida: ." + extensao);
        }

        String mimeType = normalizarMime(arquivo.getContentType(), extensao);
        if (!mimePermitido(mimeType)) {
            throw new IllegalArgumentException("Tipo MIME não permitido: " + mimeType);
        }

        long tamanho = arquivo.getSize();
        long limite = limitePorCategoria(categoria);
        if (tamanho > limite) {
            throw new IllegalArgumentException(
                    "Arquivo excede o limite permitido para " + categoria.name() + " (" + (limite / (1024 * 1024)) + " MB).");
        }

        String nomeSeguro = "db://" + UUID.randomUUID() + "." + extensao;
        byte[] bytes;
        try {
            bytes = arquivo.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao ler os bytes do arquivo de mídia.", e);
        }

        return new ArquivoArmazenado(categoria, nomeOriginal, mimeType, tamanho, nomeSeguro, bytes);
    }

    public Resource carregarRecurso(MensagemAnexo anexo) {
        if (anexo == null) {
            throw new IllegalArgumentException("Anexo inválido.");
        }
        if (anexo.getConteudoBytes() != null && anexo.getConteudoBytes().length > 0) {
            return new ByteArrayResource(anexo.getConteudoBytes());
        }
        return carregarRecurso(anexo.getCaminhoStorage());
    }

    public Resource carregarThumbnail(MensagemAnexo anexo) {
        if (anexo == null) {
            throw new IllegalArgumentException("Anexo inválido.");
        }
        if (anexo.getThumbnailBytes() != null && anexo.getThumbnailBytes().length > 0) {
            return new ByteArrayResource(anexo.getThumbnailBytes());
        }
        return carregarRecurso(anexo.getCaminhoThumbnail());
    }

    public Resource carregarRecurso(String caminhoRelativo) {
        if (caminhoRelativo == null || caminhoRelativo.isBlank() || caminhoRelativo.startsWith("db://")) {
            throw new IllegalArgumentException("Arquivo de mídia não encontrado.");
        }
        Path raiz = raizStorage();
        Path alvo = raiz.resolve(caminhoRelativo).normalize();
        if (!alvo.startsWith(raiz) || !Files.exists(alvo)) {
            throw new IllegalArgumentException("Arquivo de mídia não encontrado.");
        }
        return new FileSystemResource(alvo);
    }

    public long limitePorCategoria(CategoriaAnexo categoria) {
        return switch (categoria) {
            case IMAGEM -> properties.getMaxImagemBytes();
            case AUDIO -> properties.getMaxAudioBytes();
            case DOCUMENTO -> properties.getMaxDocumentoBytes();
            case VIDEO -> properties.getMaxVideoBytes();
        };
    }

    private Path raizStorage() {
        return Paths.get(properties.getStoragePath()).toAbsolutePath().normalize();
    }

    private String limparNomeArquivo(String original) {
        String base = (original == null || original.isBlank()) ? "anexo" : Paths.get(original).getFileName().toString();
        return base.replaceAll("[\\r\\n\\\\/]+", "_");
    }

    private String extrairExtensao(String nome) {
        int idx = nome.lastIndexOf('.');
        if (idx < 0 || idx == nome.length() - 1) {
            throw new IllegalArgumentException("Arquivo sem extensão identificável.");
        }
        return nome.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizarMime(String contentType, String extensao) {
        if (contentType != null && !contentType.isBlank()) {
            return contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
        }
        return "md".equals(extensao) ? "text/markdown" : "application/octet-stream";
    }

    private boolean mimePermitido(String mime) {
        return MIME_PREFIXES_VALIDOS.stream().anyMatch(mime::startsWith);
    }
}

