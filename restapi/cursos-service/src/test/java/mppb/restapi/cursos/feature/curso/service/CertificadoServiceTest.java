package mppb.restapi.cursos.feature.curso.service;

import mppb.restapi.cursos.feature.common.service.ArquivoService;
import mppb.restapi.cursos.feature.common.service.ServidorService;
import mppb.restapi.cursos.feature.common.projection.ServidorProjection;
import mppb.restapi.cursos.feature.common.entities.Arquivo;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.curso.entity.CursoMinistrante;
import mppb.restapi.cursos.feature.curso.repository.CursoMinistranteRepository;
import mppb.restapi.cursos.feature.curso.repository.CursoRepository;
import mppb.restapi.cursos.feature.inscricao.entity.Inscricao;
import mppb.restapi.cursos.feature.inscricao.repository.InscricaoRepository;
import mppb.restapi.cursos.feature.lookup.entities.Modalidade;
import mppb.restapi.cursos.feature.lookup.entities.PapelMinistrante;
import mppb.restapi.cursos.feature.participante.entity.Participante;
import mppb.restapi.cursos.feature.ministrante.entity.Ministrante;
import mppb.restapi.cursos.feature.participante.repository.ParticipanteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CertificadoServiceTest {

    @Mock
    private InscricaoRepository inscricaoRepository;
    @Mock
    private ArquivoService arquivoService;
    @Mock
    private TemplateEngine templateEngine;
    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private ServidorService servidorService;
    @Mock
    private CursoMinistranteRepository cursoMinistranteRepository;
    @Mock
    private ParticipanteRepository participanteRepository;

    @InjectMocks
    private CertificadoService certificadoService;

    private Curso curso;
    private Participante participante;
    private Inscricao inscricao;
    private Arquivo arquivoTemplate;

    @BeforeEach
    void setUp() {
        curso = new Curso();
        curso.setId(1L);
        curso.setTitulo("Curso Teste");
        curso.setCargaHoraria("40:00:00");
        curso.setRealizacaoDataFinal(LocalDateTime.of(2026, 3, 31, 12, 0));
        curso.setCertificateTemplateId(500L);
        curso.setStatus(mppb.restapi.cursos.feature.curso.enums.StatusCursoEnum.PRONTO_PUBLICACAO);
        
        Modalidade modalidade = new Modalidade();
        modalidade.setId(1L); // Online
        modalidade.setDescricao("online");
        curso.setModalidade(modalidade);

        participante = new Participante();
        participante.setId(10L);
        participante.setNome("Participante Teste");

        inscricao = new Inscricao();
        inscricao.setCurso(curso);
        inscricao.setParticipante(participante);

        arquivoTemplate = new Arquivo();
        arquivoTemplate.setBytes(new byte[]{1, 2, 3});
    }

    @Test
    void deveGerarContextoCorretoParaParticipante() throws Exception {
        // GIVEN
        when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
        when(inscricaoRepository.findByCursoIdAndParticipanteId(1L, 10L)).thenReturn(Optional.of(inscricao));
        when(arquivoService.buscarPorId(anyLong())).thenReturn(Optional.of(arquivoTemplate));
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html></html>");

        // WHEN
        certificadoService.gerarCertificadoPdf(1L, 10L);

        // THEN
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("certificado"), contextCaptor.capture());
        
        Context context = contextCaptor.getValue();
        assertEquals("participante", context.getVariable("papel"));
        assertEquals("Participante Teste", context.getVariable("nomeParticipante"));
        assertEquals("João Pessoa - PB", context.getVariable("cidade"));
        assertEquals("online", context.getVariable("modalidade"));
        assertTrue(context.getVariable("dataExtenso").toString().contains("de março de 2026"));
    }

    @Test
    void deveGerarContextoCorretoParaMinistrante() throws Exception {
        // GIVEN
        when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
        // Simular que não é aluno
        when(inscricaoRepository.findByCursoIdAndParticipanteId(1L, 10L)).thenReturn(Optional.empty());
        
        // Simular que é um ministrante
        Ministrante m = new Ministrante();
        m.setId(10L);
        m.setNome("Ministrante Teste");
        
        CursoMinistrante cm = new CursoMinistrante();
        PapelMinistrante papel = new PapelMinistrante();
        papel.setNome("Palestrante");
        cm.setPapel(papel);
        cm.setMinistrante(m);
        cm.setCurso(curso);
        
        when(cursoMinistranteRepository.findByCursoId(1L)).thenReturn(Collections.singletonList(cm));
        when(arquivoService.buscarPorId(anyLong())).thenReturn(Optional.of(arquivoTemplate));
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html></html>");

        // WHEN
        certificadoService.gerarCertificadoPdf(1L, 10L);

        // THEN
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("certificado"), contextCaptor.capture());
        
        Context context = contextCaptor.getValue();
        assertEquals("palestrante", context.getVariable("papel"));
        assertEquals("Ministrante Teste", context.getVariable("nomeParticipante"));
    }

    @Test
    void deveBuscarNomeNoServidorSeParticipanteNaoTiverNome() throws Exception {
        // GIVEN
        participante.setNome(null);
        participante.setIdServidor(999);
        when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
        when(inscricaoRepository.findByCursoIdAndParticipanteId(1L, 10L)).thenReturn(Optional.of(inscricao));
        
        ServidorProjection servidor = mock(ServidorProjection.class);
        when(servidor.getNome()).thenReturn("Nome do RH");
        when(servidorService.buscarServidorPorId(anyInt())).thenReturn(Optional.of(servidor));
        
        when(arquivoService.buscarPorId(anyLong())).thenReturn(Optional.of(arquivoTemplate));
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html></html>");

        // WHEN
        certificadoService.gerarCertificadoPdf(1L, 10L);

        // THEN
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("certificado"), contextCaptor.capture());
        
        Context context = contextCaptor.getValue();
        assertEquals("Nome do RH", context.getVariable("nomeParticipante"));
    }
}
