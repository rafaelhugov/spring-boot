package mppb.restapi.cursos.feature.curso.service;

import mppb.restapi.common.core.error.BusinessRuleException;
import mppb.restapi.common.core.error.NotFoundException;
import mppb.restapi.cursos.feature.common.entities.Arquivo;
import mppb.restapi.cursos.feature.common.service.ArquivoService;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.curso.entity.CursoArquivoComprobatorio;
import mppb.restapi.cursos.feature.curso.enums.StatusCursoEnum;
import mppb.restapi.cursos.feature.curso.repository.CursoArquivoComprobatorioRepository;
import mppb.restapi.cursos.feature.curso.repository.CursoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CursoArquivoComprobatorioServiceUploadSecurityTest {

    @Mock
    private CursoRepository cursoRepository;

    @Mock
    private CursoArquivoComprobatorioRepository repository;

    @Mock
    private ArquivoService arquivoService;

    @InjectMocks
    private CursoArquivoComprobatorioService service;

    private Curso cursoFinalizado;

    @BeforeEach
    void setUp() {
        cursoFinalizado = new Curso();
        cursoFinalizado.setId(1L);
        cursoFinalizado.setStatus(StatusCursoEnum.FINALIZADO);
    }

    @Test
    void uploadDeveAceitarPdfComMimePermitido() throws Exception {
        // Given
        MockMultipartFile file = arquivo("comprovante.pdf", "application/pdf", "%PDF-1.4\npdf");
        Arquivo arquivo = new Arquivo();
        arquivo.setId(10L);
        arquivo.setNomeArquivo("comprovante.pdf");
        arquivo.setMimeType("application/pdf");
        arquivo.setTamanho(file.getSize());
        when(cursoRepository.findById(1L)).thenReturn(Optional.of(cursoFinalizado));
        when(arquivoService.upload(file)).thenReturn(10L);
        when(arquivoService.buscarPorId(10L)).thenReturn(Optional.of(arquivo));
        when(repository.save(any(CursoArquivoComprobatorio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        var dto = service.upload(1L, file);

        // Then
        assertEquals(10L, dto.getArquivoId());
        assertEquals("application/pdf", dto.getMimeType());
    }

    @Test
    void uploadDeveRejeitarArquivoVazio() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "vazio.pdf", "application/pdf", new byte[0]);
        when(cursoRepository.findById(1L)).thenReturn(Optional.of(cursoFinalizado));

        // When / Then
        assertThrows(BusinessRuleException.class, () -> service.upload(1L, file));
        verify(arquivoService, never()).upload(any());
    }

    @Test
    void uploadDeveRejeitarMimeTypeNaoPermitidoMesmoComExtensaoPdf() throws Exception {
        // Given
        MockMultipartFile file = arquivo("comprovante.pdf", "application/x-msdownload", "MZ");
        when(cursoRepository.findById(1L)).thenReturn(Optional.of(cursoFinalizado));

        // When / Then
        assertThrows(BusinessRuleException.class, () -> service.upload(1L, file));
        verify(arquivoService, never()).upload(any());
    }

    @Test
    void uploadDeveRejeitarExtensaoDuplaMesmoComMimeTypeImagem() throws Exception {
        // Given
        MockMultipartFile file = arquivo("avatar.png.jsp", "image/png", "conteudo");
        when(cursoRepository.findById(1L)).thenReturn(Optional.of(cursoFinalizado));

        // When / Then
        assertThrows(BusinessRuleException.class, () -> service.upload(1L, file));
        verify(arquivoService, never()).upload(any());
    }

    @Test
    void uploadDeveRetornarNotFoundQuandoCursoNaoExiste() throws Exception {
        // Given
        when(cursoRepository.findById(99L)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class, () -> service.upload(99L, arquivo("x.pdf", "application/pdf", "%PDF-1.4\nx")));
        verify(arquivoService, never()).upload(any());
    }

    private MockMultipartFile arquivo(String nome, String mimeType, String conteudo) {
        return new MockMultipartFile("file", nome, mimeType, conteudo.getBytes(StandardCharsets.UTF_8));
    }
}
