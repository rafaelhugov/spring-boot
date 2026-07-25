package mppb.restapi.cursos.feature.common.service;

import mppb.restapi.common.core.error.BusinessRuleException;
import mppb.restapi.cursos.feature.common.entities.Arquivo;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArquivoSecurityUtilsTest {

    private static final byte[] PDF = "%PDF-1.4\nconteudo".getBytes(StandardCharsets.UTF_8);
    private static final byte[] PNG = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3
    };

    @Test
    void validaArquivoPdfPermitido() throws Exception {
        var file = new MockMultipartFile("file", "comprovante.pdf", MediaType.APPLICATION_PDF_VALUE, PDF);

        var upload = ArquivoSecurityUtils.validateUpload(file);

        assertEquals("comprovante.pdf", upload.safeName());
        assertEquals(MediaType.APPLICATION_PDF_VALUE, upload.mimeType());
        assertArrayEquals(PDF, upload.bytes());
    }

    @Test
    void rejeitaArquivoVazio() {
        var file = new MockMultipartFile("file", "vazio.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[0]);

        assertThrows(BusinessRuleException.class, () -> ArquivoSecurityUtils.validateUpload(file));
    }

    @Test
    void rejeitaArquivoAcimaDoLimite() {
        var bytes = new byte[(int) ArquivoSecurityUtils.MAX_FILE_SIZE_BYTES + 1];
        bytes[0] = '%';
        bytes[1] = 'P';
        bytes[2] = 'D';
        bytes[3] = 'F';
        bytes[4] = '-';
        var file = new MockMultipartFile("file", "grande.pdf", MediaType.APPLICATION_PDF_VALUE, bytes);

        assertThrows(BusinessRuleException.class, () -> ArquivoSecurityUtils.validateUpload(file));
    }

    @Test
    void rejeitaExtensaoNaoPermitida() {
        var file = new MockMultipartFile("file", "script.jsp", "application/octet-stream", PDF);

        assertThrows(BusinessRuleException.class, () -> ArquivoSecurityUtils.validateUpload(file));
    }

    @Test
    void rejeitaExtensaoDupla() {
        var file = new MockMultipartFile("file", "avatar.png.jsp", MediaType.IMAGE_PNG_VALUE, PNG);

        assertThrows(BusinessRuleException.class, () -> ArquivoSecurityUtils.validateUpload(file));
    }

    @Test
    void rejeitaMimeDeclaradoIncompativel() {
        var file = new MockMultipartFile("file", "comprovante.pdf", MediaType.IMAGE_PNG_VALUE, PDF);

        assertThrows(BusinessRuleException.class, () -> ArquivoSecurityUtils.validateUpload(file));
    }

    @Test
    void rejeitaConteudoIncompativelComExtensao() {
        var file = new MockMultipartFile("file", "comprovante.pdf", MediaType.APPLICATION_PDF_VALUE, "MZ".getBytes(StandardCharsets.UTF_8));

        assertThrows(BusinessRuleException.class, () -> ArquivoSecurityUtils.validateUpload(file));
    }

    @Test
    void rejeitaPathTraversalUnix() {
        var file = new MockMultipartFile("file", "../arquivo.pdf", MediaType.APPLICATION_PDF_VALUE, PDF);

        assertThrows(BusinessRuleException.class, () -> ArquivoSecurityUtils.validateUpload(file));
    }

    @Test
    void rejeitaPathTraversalWindows() {
        var file = new MockMultipartFile("file", "..\\arquivo.pdf", MediaType.APPLICATION_PDF_VALUE, PDF);

        assertThrows(BusinessRuleException.class, () -> ArquivoSecurityUtils.validateUpload(file));
    }

    @Test
    void rejeitaNomeComSeparadores() {
        var file = new MockMultipartFile("file", "pasta/arquivo.pdf", MediaType.APPLICATION_PDF_VALUE, PDF);

        assertThrows(BusinessRuleException.class, () -> ArquivoSecurityUtils.validateUpload(file));
    }

    @Test
    void rejeitaNomeComCaracteresDeControle() {
        var file = new MockMultipartFile("file", "arquivo\u0000.pdf", MediaType.APPLICATION_PDF_VALUE, PDF);

        assertThrows(BusinessRuleException.class, () -> ArquivoSecurityUtils.validateUpload(file));
    }

    @Test
    void downloadSeguroDefineHeadersObrigatoriosENomeSanitizado() {
        Arquivo arquivo = new Arquivo();
        arquivo.setNomeArquivo("comprovante.pdf");
        arquivo.setMimeType(MediaType.APPLICATION_PDF_VALUE);
        arquivo.setBytes(PDF);

        var response = ArquivoSecurityUtils.secureDownloadResponse(arquivo, false);

        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertEquals("nosniff", response.getHeaders().getFirst(ArquivoSecurityUtils.HEADER_NOSNIFF));
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("attachment"));
        assertFalse(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains(".."));
    }
}
