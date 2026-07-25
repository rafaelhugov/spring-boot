package mppb.restapi.cursos.feature.frequencia.service;

import jakarta.persistence.EntityManager;
import mppb.restapi.common.core.error.BusinessRuleException;
import mppb.restapi.common.core.error.NotFoundException;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.frequencia.entity.Aula;
import mppb.restapi.cursos.feature.frequencia.entity.Chamada;
import mppb.restapi.cursos.feature.frequencia.enums.TipoChamadaEnum;
import mppb.restapi.cursos.feature.frequencia.repository.ChamadaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChamadaServiceTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private ChamadaRepository chamadaRepository;

    @InjectMocks
    private ChamadaService chamadaService;

    @Test
    void buscarPorTokenDeveRetornarChamadaExistente() {
        // Given
        Chamada chamada = new Chamada();
        when(chamadaRepository.findByToken("abc")).thenReturn(Optional.of(chamada));

        // When
        Chamada resultado = chamadaService.buscarPorToken("abc");

        // Then
        assertSame(chamada, resultado);
    }

    @Test
    void buscarPorTokenDeveFalharQuandoTokenNaoExistir() {
        // Given
        when(chamadaRepository.findByToken("invalido")).thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class, () -> chamadaService.buscarPorToken("invalido"));
    }

    @Test
    void abrirChamadaDeveCriarChamadaQuandoTipoAindaNaoExisteNaAula() {
        // Given
        Aula aula = new Aula();
        when(entityManager.find(Aula.class, 10L)).thenReturn(aula);
        when(chamadaRepository.findAllByAulaId(10L)).thenReturn(List.of());
        when(chamadaRepository.save(any(Chamada.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Chamada resultado = chamadaService.abrirChamada(10L, TipoChamadaEnum.INICIO);

        // Then
        assertSame(aula, resultado.getAula());
        assertEquals(TipoChamadaEnum.INICIO, resultado.getTipo());
        assertTrue(resultado.isAberta());
        verify(chamadaRepository).save(resultado);
    }

    @Test
    void abrirChamadaDeveImpedirTipoDuplicadoNaMesmaAula() {
        // Given
        Aula aula = new Aula();
        Chamada existente = Chamada.builder().aula(aula).tipo(TipoChamadaEnum.INICIO).build();
        when(entityManager.find(Aula.class, 10L)).thenReturn(aula);
        when(chamadaRepository.findAllByAulaId(10L)).thenReturn(List.of(existente));

        // When / Then
        assertThrows(BusinessRuleException.class, () -> chamadaService.abrirChamada(10L, TipoChamadaEnum.INICIO));
    }

    @Test
    void encerrarChamadaDeveMarcarComoFechada() {
        // Given
        Chamada chamada = Chamada.builder().aberta(true).build();
        when(chamadaRepository.findById(5L)).thenReturn(Optional.of(chamada));
        when(chamadaRepository.save(chamada)).thenReturn(chamada);

        // When
        Chamada resultado = chamadaService.encerrarChamada(5L);

        // Then
        assertFalse(resultado.isAberta());
        verify(chamadaRepository).save(chamada);
    }

    @Test
    void getDadosChamadaPublicaDeveMapearDadosEssenciaisDaAula() {
        // Given
        Curso curso = new Curso();
        curso.setTitulo("Curso Seguro");
        Aula aula = Aula.builder()
                .curso(curso)
                .descricao("Aula 1")
                .data(LocalDate.of(2026, 8, 20))
                .horaInicio(LocalTime.of(8, 0))
                .horaFim(LocalTime.of(10, 0))
                .build();
        when(entityManager.find(Aula.class, 10L)).thenReturn(aula);

        // When
        var dto = chamadaService.getDadosChamadaPublica(10L);

        // Then
        assertEquals("Curso Seguro", dto.getCursoTitulo());
        assertEquals("Aula 1", dto.getAulaDescricao());
        assertEquals(LocalDate.of(2026, 8, 20), dto.getData());
        assertTrue(dto.isAberta());
    }
}
