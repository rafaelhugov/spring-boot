package mppb.restapi.cursos.feature.common.service;

import mppb.restapi.cursos.feature.common.entities.Arquivo;
import mppb.restapi.cursos.feature.common.repository.ArquivoRepository;
import mppb.restapi.common.core.error.BusinessRuleException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArquivoServiceTest {

    @Mock
    private ArquivoRepository arquivoRepository;

    @InjectMocks
    private ArquivoService arquivoService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadDevePersistirMetadadosEBytesDoArquivo() throws Exception {
        // Given
        byte[] conteudo = "%PDF-1.4\ncurriculo".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "curriculo.pdf", "application/pdf", conteudo);
        Arquivo salvo = new Arquivo();
        salvo.setId(42L);
        when(arquivoRepository.save(org.mockito.ArgumentMatchers.any(Arquivo.class))).thenReturn(salvo);

        // When
        Long id = arquivoService.upload(file);

        // Then
        assertEquals(42L, id);
        ArgumentCaptor<Arquivo> captor = ArgumentCaptor.forClass(Arquivo.class);
        verify(arquivoRepository).save(captor.capture());
        Arquivo arquivo = captor.getValue();
        assertEquals("curriculo.pdf", arquivo.getNomeArquivo());
        assertEquals("application/pdf", arquivo.getMimeType());
        assertEquals(conteudo.length, arquivo.getTamanho());
        assertArrayEquals(conteudo, arquivo.getBytes());
    }

    @Test
    void uploadDeveRejeitarNomeOriginalMaliciosoComPathTraversal() {
        // Given
        byte[] conteudo = "%PDF-1.4\nconteudo".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../../webshell.pdf.jsp",
                "application/pdf",
                conteudo);

        // When / Then
        assertThrows(BusinessRuleException.class, () -> arquivoService.upload(file));
    }

    @Test
    void buscarPorIdDeveDelegarParaRepositorio() {
        // Given
        Arquivo arquivo = new Arquivo();
        when(arquivoRepository.findById(9L)).thenReturn(Optional.of(arquivo));

        // When
        Optional<Arquivo> resultado = arquivoService.buscarPorId(9L);

        // Then
        assertSame(arquivo, resultado.orElseThrow());
    }

    @Test
    void removerDeveDelegarExclusaoParaRepositorio() {
        // When
        arquivoService.remover(9L);

        // Then
        verify(arquivoRepository).deleteById(9L);
    }

    @Test
    void buscarArquivoPrivadoAutorizadoDevePermitirVinculoAdministrativoSomenteParaAdmin() {
        // Given
        autenticarComoAdmin();
        Arquivo arquivo = new Arquivo();
        when(arquivoRepository.findById(7L)).thenReturn(Optional.of(arquivo));
        when(arquivoRepository.existsCursoHeaderImageReference(7L)).thenReturn(false);
        when(arquivoRepository.existsCursoCertificateTemplateReference(7L)).thenReturn(false);
        when(arquivoRepository.existsArquivoComprobatorioReference(7L)).thenReturn(true);

        // When
        Arquivo resultado = arquivoService.buscarArquivoPrivadoAutorizado(7L);

        // Then
        assertSame(arquivo, resultado);
    }

    @Test
    void buscarArquivoPrivadoAutorizadoDeveRetornar403ParaUsuarioComumEmVinculoAdministrativo() {
        // Given
        Arquivo arquivo = new Arquivo();
        when(arquivoRepository.findById(7L)).thenReturn(Optional.of(arquivo));
        when(arquivoRepository.existsCursoHeaderImageReference(7L)).thenReturn(false);
        when(arquivoRepository.existsCursoCertificateTemplateReference(7L)).thenReturn(false);
        when(arquivoRepository.existsArquivoComprobatorioReference(7L)).thenReturn(true);

        // When / Then
        var exception = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> arquivoService.buscarArquivoPrivadoAutorizado(7L));
        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void buscarArquivoPrivadoAutorizadoDeveRetornar403QuandoArquivoNaoTemVinculo() {
        // Given
        Arquivo arquivo = new Arquivo();
        when(arquivoRepository.findById(8L)).thenReturn(Optional.of(arquivo));

        // When / Then
        var exception = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> arquivoService.buscarArquivoPrivadoAutorizado(8L));
        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void buscarArquivoPrivadoAutorizadoDeveRetornar403QuandoArquivoTemVinculoAmbiguoMesmoParaAdmin() {
        // Given
        autenticarComoAdmin();
        Arquivo arquivo = new Arquivo();
        when(arquivoRepository.findById(13L)).thenReturn(Optional.of(arquivo));
        when(arquivoRepository.existsCursoHeaderImageReference(13L)).thenReturn(true);
        when(arquivoRepository.existsCursoCertificateTemplateReference(13L)).thenReturn(true);

        // When / Then
        var exception = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> arquivoService.buscarArquivoPrivadoAutorizado(13L));
        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void buscarArquivoPrivadoAutorizadoDeveRetornar404QuandoArquivoNaoExiste() {
        // Given
        when(arquivoRepository.findById(404L)).thenReturn(Optional.empty());

        // When / Then
        var exception = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> arquivoService.buscarArquivoPrivadoAutorizado(404L));
        assertEquals(404, exception.getStatusCode().value());
        verify(arquivoRepository, never()).existsCursoHeaderImageReference(404L);
    }

    @Test
    void buscarArquivoPublicoAutorizadoDevePermitirSomenteImagemCabecalhoPublicada() {
        // Given
        Arquivo arquivo = new Arquivo();
        when(arquivoRepository.findById(11L)).thenReturn(Optional.of(arquivo));
        when(arquivoRepository.existsPublicHeaderImageReference(11L)).thenReturn(true);

        // When
        Arquivo resultado = arquivoService.buscarArquivoPublicoAutorizado(11L);

        // Then
        assertSame(arquivo, resultado);
    }

    @Test
    void buscarArquivoPublicoAutorizadoDeveRetornar403QuandoArquivoEPrivado() {
        // Given
        Arquivo arquivo = new Arquivo();
        when(arquivoRepository.findById(12L)).thenReturn(Optional.of(arquivo));
        when(arquivoRepository.existsPublicHeaderImageReference(12L)).thenReturn(false);

        // When / Then
        var exception = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> arquivoService.buscarArquivoPublicoAutorizado(12L));
        assertEquals(403, exception.getStatusCode().value());
    }

    private void autenticarComoAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin",
                "n/a",
                List.of(new SimpleGrantedAuthority("cursos.manter.cursos"))));
    }
}
