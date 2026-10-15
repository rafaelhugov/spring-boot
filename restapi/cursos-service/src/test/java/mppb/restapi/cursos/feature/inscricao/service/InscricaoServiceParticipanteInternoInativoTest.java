package mppb.restapi.cursos.feature.inscricao.service;

import jakarta.persistence.EntityManager;
import mppb.restapi.common.core.error.BusinessRuleException;
import mppb.restapi.common.infrastructure.client.authapi.dto.AcessoDataResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.UsuarioAutorizadoResponseDto;
import mppb.restapi.common.infrastructure.security.authorization.TokenAuthentication;
import mppb.restapi.cursos.feature.common.projection.ServidorProjection;
import mppb.restapi.cursos.feature.common.service.EmailService;
import mppb.restapi.cursos.feature.common.service.ServidorService;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.curso.enums.StatusCursoEnum;
import mppb.restapi.cursos.feature.curso.repository.CursoRepository;
import mppb.restapi.cursos.feature.curso.service.CursoService;
import mppb.restapi.cursos.feature.frequencia.service.FrequenciaService;
import mppb.restapi.cursos.feature.inscricao.converter.InscricaoConverter;
import mppb.restapi.cursos.feature.inscricao.dto.InscricaoDto;
import mppb.restapi.cursos.feature.inscricao.entity.Inscricao;
import mppb.restapi.cursos.feature.inscricao.repository.InscricaoConfirmacaoTokenRepository;
import mppb.restapi.cursos.feature.inscricao.repository.InscricaoRepository;
import mppb.restapi.cursos.feature.lookup.entities.PublicoAlvo;
import mppb.restapi.cursos.feature.participante.dto.CriarInscricaoDto;
import mppb.restapi.cursos.feature.participante.dto.InscricaoPublicaDto;
import mppb.restapi.cursos.feature.participante.dto.ParticipanteDto;
import mppb.restapi.cursos.feature.participante.entity.Participante;
import mppb.restapi.cursos.feature.participante.enums.TipoParticipanteEnum;
import mppb.restapi.cursos.feature.participante.repository.ParticipanteRepository;
import mppb.restapi.cursos.feature.participante.service.ParticipanteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InscricaoServiceParticipanteInternoInativoTest {

    @Mock
    private EntityManager entityManager;
    @Mock
    private InscricaoRepository inscricaoRepository;
    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private ParticipanteService participanteService;
    @Mock
    private InscricaoConverter inscricaoConverter;
    @Mock
    private InscricaoConfirmacaoTokenRepository tokenRepository;
    @Mock
    private CursoService cursoService;
    @Mock
    private ServidorService servidorService;
    @Mock
    private ParticipanteRepository participanteRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private FrequenciaService frequenciaService;

    private InscricaoService inscricaoService;
    private Curso cursoAntigo;
    private CriarInscricaoDto dto;
    private Participante participante;

    @BeforeEach
    void setUp() {
        inscricaoService = new InscricaoService(
                entityManager,
                inscricaoRepository,
                cursoRepository,
                participanteService,
                inscricaoConverter,
                tokenRepository,
                cursoService,
                servidorService,
                participanteRepository,
                emailService,
                frequenciaService);

        PublicoAlvo servidores = new PublicoAlvo();
        servidores.setNome("Servidores");
        servidores.setInternoExterno("I");

        cursoAntigo = new Curso();
        cursoAntigo.setId(1L);
        cursoAntigo.setStatus(StatusCursoEnum.PUBLICADO);
        cursoAntigo.setRealizacaoDataInicial(LocalDateTime.now().minusYears(1).minusDays(2));
        cursoAntigo.setRealizacaoDataFinal(LocalDateTime.now().minusYears(1));
        cursoAntigo.setPublicosAlvo(Set.of(servidores));

        dto = new CriarInscricaoDto();
        dto.setTipo(TipoParticipanteEnum.INTERNO.name());
        dto.setIdServidor(10);
        dto.setPcd(false);

        participante = new Participante();
        participante.setId(20L);
        participante.setTipo(TipoParticipanteEnum.INTERNO);
        participante.setIdServidor(10);
    }

    @Test
    void adminPermiteServidorInativoQueEstavaAtivoAteFimDoCursoAntigo() {
        when(cursoRepository.findById(1L)).thenReturn(Optional.of(cursoAntigo));
        when(servidorService.buscarServidorPorId(10))
                .thenReturn(Optional.of(servidor("INATIVO", cursoAntigo.getRealizacaoDataFinal().toLocalDate())));
        when(participanteService.findOrCreateParticipante(dto)).thenReturn(participante);
        when(inscricaoRepository.existsByCursoAndParticipante(cursoAntigo, participante)).thenReturn(false);
        when(inscricaoRepository.save(any(Inscricao.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inscricaoConverter.toDto(any(Inscricao.class))).thenReturn(new InscricaoDto());
        when(participanteService.toEnrichedDto(participante)).thenReturn(new ParticipanteDto());

        assertDoesNotThrow(() -> inscricaoService.inscrever(1L, dto));

        ArgumentCaptor<Inscricao> captor = ArgumentCaptor.forClass(Inscricao.class);
        verify(inscricaoRepository).save(captor.capture());
        assertFalse(captor.getValue().getCienciaCriteriosPublicoAlvo());
        assertNull(captor.getValue().getDataCienciaCriteriosPublicoAlvo());
        assertNull(captor.getValue().getCriteriosPublicoAlvoDeclarados());
    }

    @Test
    void adminBloqueiaServidorInativoComSaidaAntesDoFimDoCursoAntigo() {
        when(cursoRepository.findById(1L)).thenReturn(Optional.of(cursoAntigo));
        when(servidorService.buscarServidorPorId(10))
                .thenReturn(Optional.of(servidor("INATIVO", cursoAntigo.getRealizacaoDataFinal().toLocalDate().minusDays(1))));

        assertThrows(BusinessRuleException.class, () -> inscricaoService.inscrever(1L, dto));
    }

    @Test
    void adminBloqueiaServidorInativoEmCursoAtualOuFuturo() {
        cursoAntigo.setRealizacaoDataFinal(LocalDateTime.now().plusDays(1));
        when(cursoRepository.findById(1L)).thenReturn(Optional.of(cursoAntigo));
        when(servidorService.buscarServidorPorId(10))
                .thenReturn(Optional.of(servidor("INATIVO", cursoAntigo.getRealizacaoDataFinal().toLocalDate().plusDays(1))));

        assertThrows(BusinessRuleException.class, () -> inscricaoService.inscrever(1L, dto));
    }

    @Test
    void fluxoInternoMantemBloqueioParaServidorInativo() throws Exception {
        when(cursoRepository.findById(1L)).thenReturn(Optional.of(cursoAntigo));
        when(servidorService.buscarServidorPorId(10))
                .thenReturn(Optional.of(servidor("INATIVO", cursoAntigo.getRealizacaoDataFinal().toLocalDate())));

        Method metodoInterno = InscricaoService.class
                .getDeclaredMethod("inscrever", Long.class, CriarInscricaoDto.class, boolean.class);
        metodoInterno.setAccessible(true);

        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                () -> metodoInterno.invoke(inscricaoService, 1L, dto, false));

        org.junit.jupiter.api.Assertions.assertInstanceOf(BusinessRuleException.class, exception.getCause());
    }

    @Test
    void exigeDeclaracaoQuandoCursoPossuiCriteriosEspecificos() {
        cursoAntigo.setCriteriosEspecificosPublicoAlvo("Atuar em promotoria com atribuição criminal.");
        prepararInscricaoAdministrativaValida();

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> inscricaoService.inscrever(1L, dto));

        assertTrue(exception.getMessage().contains("declarar ciência"));
        verify(inscricaoRepository, never()).save(any());
    }

    @Test
    void registraDataECopiaImutavelDosCriteriosDeclarados() {
        cursoAntigo.setCriteriosEspecificosPublicoAlvo("  Atuar em promotoria com atribuição criminal.  ");
        dto.setDeclaracaoCriteriosPublicoAlvo(true);
        prepararInscricaoAdministrativaValida();
        when(inscricaoRepository.save(any(Inscricao.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inscricaoConverter.toDto(any(Inscricao.class))).thenReturn(new InscricaoDto());
        when(participanteService.toEnrichedDto(participante)).thenReturn(new ParticipanteDto());

        inscricaoService.inscrever(1L, dto);

        ArgumentCaptor<Inscricao> captor = ArgumentCaptor.forClass(Inscricao.class);
        verify(inscricaoRepository).save(captor.capture());
        Inscricao inscricao = captor.getValue();
        assertTrue(inscricao.getCienciaCriteriosPublicoAlvo());
        assertNotNull(inscricao.getDataCienciaCriteriosPublicoAlvo());
        assertEquals("Atuar em promotoria com atribuição criminal.",
                inscricao.getCriteriosPublicoAlvoDeclarados());

        cursoAntigo.setCriteriosEspecificosPublicoAlvo("Critério alterado posteriormente.");
        assertEquals("Atuar em promotoria com atribuição criminal.",
                inscricao.getCriteriosPublicoAlvoDeclarados());
    }

    @Test
    void fluxoInternoRegistraDeclaracaoDosCriterios() {
        UUID hash = UUID.randomUUID();
        cursoAntigo.setCriteriosEspecificosPublicoAlvo("Critério para participante interno.");
        cursoAntigo.setHashInscricao(hash);
        when(cursoRepository.findByHashInscricao(hash)).thenReturn(Optional.of(cursoAntigo));
        when(cursoService.isPeriodoInscricaoValido(cursoAntigo)).thenReturn(true);
        when(cursoRepository.findById(1L)).thenReturn(Optional.of(cursoAntigo));
        when(servidorService.findIdServidorByLogin("Interno")).thenReturn(Optional.of(10));
        when(servidorService.buscarServidorPorId(10)).thenReturn(Optional.of(servidor("ATIVO", null)));
        when(participanteService.findOrCreateParticipante(any(CriarInscricaoDto.class))).thenReturn(participante);
        when(inscricaoRepository.existsByCursoAndParticipante(cursoAntigo, participante)).thenReturn(false);
        when(inscricaoRepository.save(any(Inscricao.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inscricaoConverter.toDto(any(Inscricao.class))).thenReturn(new InscricaoDto());
        when(participanteService.toEnrichedDto(participante)).thenReturn(new ParticipanteDto());

        UsuarioAutorizadoResponseDto usuario = new UsuarioAutorizadoResponseDto(1L, "interno", "Interno");
        TokenAuthentication authentication = new TokenAuthentication(
                "token", usuario, mock(AcessoDataResponseDto.class), null, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            var dados = new mppb.restapi.cursos.feature.inscricao.dto.InscricaoAcessibilidadeDto();
            dados.setDeclaracaoCriteriosPublicoAlvo(true);

            assertDoesNotThrow(() -> inscricaoService.realizarInscricaoInterna(hash, dados));
        } finally {
            SecurityContextHolder.clearContext();
        }

        ArgumentCaptor<Inscricao> captor = ArgumentCaptor.forClass(Inscricao.class);
        verify(inscricaoRepository).save(captor.capture());
        assertTrue(captor.getValue().getCienciaCriteriosPublicoAlvo());
        assertEquals("Critério para participante interno.",
                captor.getValue().getCriteriosPublicoAlvoDeclarados());
    }

    @Test
    void fluxoExternoRejeitaInscricaoSemDeclaracaoDosCriterios() {
        UUID hash = UUID.randomUUID();
        PublicoAlvo externos = new PublicoAlvo();
        externos.setNome("Público externo");
        externos.setInternoExterno("E");
        cursoAntigo.setPublicosAlvo(Set.of(externos));
        cursoAntigo.setCriteriosEspecificosPublicoAlvo("Critério para participante externo.");
        cursoAntigo.setHashInscricao(hash);

        Participante participanteExterno = new Participante();
        participanteExterno.setId(21L);
        participanteExterno.setTipo(TipoParticipanteEnum.EXTERNO);
        InscricaoPublicaDto inscricaoPublica = new InscricaoPublicaDto();
        inscricaoPublica.setDeclaracaoCriteriosPublicoAlvo(false);

        when(cursoRepository.findByHashInscricao(hash)).thenReturn(Optional.of(cursoAntigo));
        when(cursoService.isPeriodoInscricaoValido(cursoAntigo)).thenReturn(true);
        when(participanteService.findOrCreateParticipanteExterno(inscricaoPublica)).thenReturn(participanteExterno);
        when(inscricaoRepository.existsByCursoAndParticipante(cursoAntigo, participanteExterno)).thenReturn(false);

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> inscricaoService.realizarInscricaoPublica(hash, inscricaoPublica));

        assertTrue(exception.getMessage().contains("declarar ciência"));
        verify(inscricaoRepository, never()).save(any());
    }

    private void prepararInscricaoAdministrativaValida() {
        when(cursoRepository.findById(1L)).thenReturn(Optional.of(cursoAntigo));
        when(servidorService.buscarServidorPorId(10))
                .thenReturn(Optional.of(servidor("ATIVO", null)));
        when(participanteService.findOrCreateParticipante(dto)).thenReturn(participante);
        when(inscricaoRepository.existsByCursoAndParticipante(cursoAntigo, participante)).thenReturn(false);
    }

    private ServidorProjection servidor(String situacao, LocalDate dataSaida) {
        return new ServidorProjection() {
            @Override
            public Integer getId() {
                return 10;
            }

            @Override
            public Integer getIdPessoa() {
                return 100;
            }

            @Override
            public String getNome() {
                return "Servidor Inativo";
            }

            @Override
            public String getCargoNome() {
                return "Técnico Ministerial";
            }

            @Override
            public String getTipoCargoNome() {
                return "Servidor";
            }

            @Override
            public String getServidorMatricula() {
                return "123";
            }

            @Override
            public String getSituacao() {
                return situacao;
            }

            @Override
            public String getEmail() {
                return "servidor@example.org";
            }

            @Override
            public String getTelefone() {
                return null;
            }

            @Override
            public String getCpf() {
                return "12345678901";
            }

            @Override
            public LocalDate getDataSaida() {
                return dataSaida;
            }
        };
    }
}
