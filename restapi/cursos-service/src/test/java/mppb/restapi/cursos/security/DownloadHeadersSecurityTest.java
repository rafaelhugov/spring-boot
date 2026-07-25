package mppb.restapi.cursos.security;

import mppb.restapi.cursos.feature.common.controller.ArquivoController;
import mppb.restapi.cursos.feature.common.entities.Arquivo;
import mppb.restapi.cursos.feature.common.service.ArquivoService;
import mppb.restapi.cursos.feature.curso.controller.CertificadoController;
import mppb.restapi.cursos.feature.curso.repository.CursoRepository;
import mppb.restapi.cursos.feature.curso.service.CertificadoService;
import mppb.restapi.cursos.feature.documentos.controller.DocumentosController;
import mppb.restapi.cursos.feature.documentos.service.DocumentosService;
import mppb.restapi.cursos.feature.publico.controller.PublicController;
import mppb.restapi.cursos.feature.curso.service.CursoService;
import mppb.restapi.cursos.feature.frequencia.converter.AulaConverter;
import mppb.restapi.cursos.feature.frequencia.service.ChamadaService;
import mppb.restapi.cursos.feature.frequencia.service.FrequenciaService;
import mppb.restapi.cursos.feature.inscricao.service.InscricaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DownloadHeadersSecurityTest {

    @Mock
    private ArquivoService arquivoService;
    @Mock
    private DocumentosService documentosService;
    @Mock
    private CertificadoService certificadoService;
    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private CursoService cursoService;
    @Mock
    private InscricaoService inscricaoService;
    @Mock
    private ChamadaService chamadaService;
    @Mock
    private FrequenciaService frequenciaService;
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private AulaConverter aulaConverter;

    private MockMvc arquivoMvc;
    private MockMvc documentosMvc;
    private MockMvc certificadoMvc;
    private MockMvc publicMvc;

    @BeforeEach
    void setUp() {
        arquivoMvc = standaloneSetup(new ArquivoController(arquivoService)).build();
        documentosMvc = standaloneSetup(new DocumentosController(documentosService)).build();
        certificadoMvc = standaloneSetup(new CertificadoController(certificadoService, arquivoService, cursoRepository)).build();
        publicMvc = standaloneSetup(new PublicController(
                cursoService,
                inscricaoService,
                chamadaService,
                frequenciaService,
                arquivoService,
                modelMapper,
                aulaConverter)).build();
    }

    @Test
    void downloadGenericoDefineContentDispositionNosniffECachePrivado() throws Exception {
        when(arquivoService.buscarArquivoPrivadoAutorizado(10L)).thenReturn(arquivo("application/pdf", "comprovante.pdf", "pdf"));

        arquivoMvc.perform(get("/api/arquivos/10"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/pdf"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("comprovante.pdf")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));
    }

    @Test
    void downloadGenericoArquivoInexistenteRetorna404() throws Exception {
        when(arquivoService.buscarArquivoPrivadoAutorizado(404L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo não encontrado."));

        arquivoMvc.perform(get("/api/arquivos/404"))
                .andExpect(status().isNotFound());
    }

    @Test
    void downloadPublicoPorIdDefineHeadersSegurosQuandoArquivoEPublico() throws Exception {
        when(arquivoService.buscarArquivoPublicoAutorizado(20L)).thenReturn(arquivo("image/png", "cabecalho.png", "png"));

        publicMvc.perform(get("/api/public/arquivos/20"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("max-age=3600")));
    }

    @Test
    void downloadPublicoArquivoPrivadoRetorna403() throws Exception {
        when(arquivoService.buscarArquivoPublicoAutorizado(21L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Arquivo privado."));

        publicMvc.perform(get("/api/public/arquivos/21"))
                .andExpect(status().isForbidden());
    }

    @Test
    void documentoGeradoDefinePdfEContentDisposition() throws Exception {
        when(documentosService.gerarDocumento(any(), any())).thenReturn("%PDF".getBytes(StandardCharsets.UTF_8));

        documentosMvc.perform(post("/api/cursos/1/documentos/gerar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoDocumento\":\"LISTA_PRESENCA\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("documento_1.pdf")));
    }

    @Test
    void certificadoDefinePdfEContentDisposition() throws Exception {
        when(certificadoService.gerarCertificadoPdf(1L, 2L)).thenReturn("%PDF".getBytes(StandardCharsets.UTF_8));

        certificadoMvc.perform(get("/api/cursos/1/participantes/2/certificado/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("certificado.pdf")));
    }

    private Arquivo arquivo(String mimeType, String nome, String body) {
        Arquivo arquivo = new Arquivo();
        arquivo.setNomeArquivo(nome);
        arquivo.setMimeType(mimeType);
        arquivo.setBytes(body.getBytes(StandardCharsets.UTF_8));
        return arquivo;
    }
}
