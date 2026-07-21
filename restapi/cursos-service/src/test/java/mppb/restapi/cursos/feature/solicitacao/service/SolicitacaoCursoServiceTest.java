package mppb.restapi.cursos.feature.solicitacao.service;

import mppb.restapi.common.core.error.BusinessRuleException;
import mppb.restapi.cursos.feature.cadastro.entities.EntidadeTutora;
import mppb.restapi.cursos.feature.cadastro.repository.EntidadeTutoraRepository;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.curso.service.CursoService;
import mppb.restapi.cursos.feature.lookup.entities.Modalidade;
import mppb.restapi.cursos.feature.lookup.entities.Municipio;
import mppb.restapi.cursos.feature.lookup.entities.StatusTramitacao;
import mppb.restapi.cursos.feature.lookup.repository.MunicipioRepository;
import mppb.restapi.cursos.feature.lookup.repository.StatusTramitacaoRepository;
import mppb.restapi.cursos.feature.frequencia.repository.AulaRepository;
import mppb.restapi.cursos.feature.solicitacao.entity.SolicitacaoAgenda;
import mppb.restapi.cursos.feature.solicitacao.entity.SolicitacaoCurso;
import mppb.restapi.cursos.feature.solicitacao.entity.SolicitacaoMinistrante;
import mppb.restapi.cursos.feature.solicitacao.repository.SolicitacaoCursoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SolicitacaoCursoServiceTest {

    @Mock
    private SolicitacaoCursoRepository solicitacaoRepository;

    @Mock
    private CursoService cursoService;

    @Mock
    private StatusTramitacaoRepository statusTramitacaoRepository;

    @Mock
    private MunicipioRepository municipioRepository;

    @Mock
    private EntidadeTutoraRepository entidadeTutoraRepository;

    @Mock
    private AulaRepository aulaRepository;

    @InjectMocks
    private SolicitacaoCursoService solicitacaoCursoService;

    private SolicitacaoCurso solicitacao;
    private StatusTramitacao statusProtocolado;
    private StatusTramitacao statusConvertido;

    @BeforeEach
    void setUp() {
        statusProtocolado = new StatusTramitacao();
        statusProtocolado.setId(3L);
        statusProtocolado.setDescricao("PROTOCOLADO");

        statusConvertido = new StatusTramitacao();
        statusConvertido.setId(6L);
        statusConvertido.setDescricao("CONVERTIDO");

        solicitacao = new SolicitacaoCurso();
        solicitacao.setId(1L);
        solicitacao.setNomeCurso("Curso de Teste");
        solicitacao.setStatus(statusProtocolado);
        solicitacao.setNumeroPgaMpVirtual("2026.001.001");
        solicitacao.setPublicosAlvo(new ArrayList<>());
        
        Modalidade modalidade = new Modalidade();
        modalidade.setId(1L);
        modalidade.setDescricao("Presencial");
        solicitacao.setModalidade(modalidade);

        EntidadeTutora ceaf = new EntidadeTutora();
        ceaf.setId(1L);
        ceaf.setNome("CEAF");
        solicitacao.setEntidadeTutora(ceaf);
    }

    @Test
    void deveConverterSolicitacaoProtocoladaParaCurso() {
        // GIVEN
        // Adicionar uma agenda para testar o cálculo de carga horária
        SolicitacaoAgenda agenda = new SolicitacaoAgenda();
        agenda.setDataInicio(LocalDate.of(2026, 10, 1));
        agenda.setDataFim(LocalDate.of(2026, 10, 1));
        agenda.setHorarioInicio(LocalTime.of(8, 0));
        agenda.setHorarioFim(LocalTime.of(12, 0));
        solicitacao.addAgenda(agenda);

        // Adicionar um ministrante
        SolicitacaoMinistrante ministrante = new SolicitacaoMinistrante();
        ministrante.setNomeMinistrante("Ministrante Teste");
        solicitacao.addMinistrante(ministrante);

        Curso cursoSalvo = new Curso();
        cursoSalvo.setId(100L);
        cursoSalvo.setTitulo(solicitacao.getNomeCurso());

        when(solicitacaoRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(solicitacao));
        when(cursoService.save(any(Curso.class))).thenReturn(cursoSalvo);
        when(statusTramitacaoRepository.findById(6L)).thenReturn(Optional.of(statusConvertido));

        // WHEN
        Long cursoId = solicitacaoCursoService.converterParaCurso(1L);

        // THEN
        assertEquals(100L, cursoId);
        assertEquals(statusConvertido, solicitacao.getStatus());
        assertEquals(100L, solicitacao.getIdCursoGerado());

        verify(cursoService).save(any(Curso.class));
        verify(solicitacaoRepository).save(solicitacao);
        
        // Verificar se a carga horária foi calculada como 04:00:00 (8h às 12h = 4h)
        verify(cursoService).save(argThat(curso -> 
            "04:00:00".equals(curso.getCargaHoraria()) &&
            "Curso de Teste".equals(curso.getTitulo())
        ));
    }

    @Test
    void deveLancarExcecaoQuandoStatusNaoForProtocolado() {
        // GIVEN
        StatusTramitacao statusRascunho = new StatusTramitacao();
        statusRascunho.setId(1L);
        solicitacao.setStatus(statusRascunho);

        when(solicitacaoRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(solicitacao));

        // WHEN & THEN
        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
            solicitacaoCursoService.converterParaCurso(1L);
        });

        assertTrue(exception.getMessage().contains("Apenas solicitações PROTOCOLADAS"));
        verify(cursoService, never()).save(any());
    }

    @Test
    void deveConverterSolicitacaoParaUmCursoPorMunicipioSelecionado() {
        // GIVEN
        Municipio joaoPessoa = new Municipio();
        joaoPessoa.setId(1L);
        joaoPessoa.setNome("João Pessoa");

        Municipio cajazeiras = new Municipio();
        cajazeiras.setId(2L);
        cajazeiras.setNome("Cajazeiras");

        Curso cursoJoaoPessoa = new Curso();
        cursoJoaoPessoa.setId(100L);

        Curso cursoCajazeiras = new Curso();
        cursoCajazeiras.setId(101L);

        when(solicitacaoRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(solicitacao));
        when(municipioRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(joaoPessoa, cajazeiras));
        when(cursoService.save(any(Curso.class))).thenReturn(cursoJoaoPessoa, cursoCajazeiras);
        when(statusTramitacaoRepository.findById(6L)).thenReturn(Optional.of(statusConvertido));

        // WHEN
        List<Long> cursoIds = solicitacaoCursoService.converterParaCursos(1L, List.of(1L, 2L));

        // THEN
        assertEquals(List.of(100L, 101L), cursoIds);
        assertEquals(statusConvertido, solicitacao.getStatus());
        assertEquals(100L, solicitacao.getIdCursoGerado());

        verify(cursoService, times(2)).save(any(Curso.class));
        verify(cursoService).save(argThat(curso -> joaoPessoa.equals(curso.getMunicipio())));
        verify(cursoService).save(argThat(curso -> cajazeiras.equals(curso.getMunicipio())));
        verify(solicitacaoRepository).save(solicitacao);
    }
}
