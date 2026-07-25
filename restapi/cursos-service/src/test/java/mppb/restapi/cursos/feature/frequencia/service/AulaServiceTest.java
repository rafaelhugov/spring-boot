package mppb.restapi.cursos.feature.frequencia.service;

import mppb.restapi.common.core.error.BusinessRuleException;
import mppb.restapi.common.core.error.NotFoundException;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.curso.enums.StatusCursoEnum;
import mppb.restapi.cursos.feature.curso.repository.CursoRepository;
import mppb.restapi.cursos.feature.frequencia.converter.AulaConverter;
import mppb.restapi.cursos.feature.frequencia.entity.Aula;
import mppb.restapi.cursos.feature.frequencia.repository.AulaRepository;
import mppb.restapi.cursos.feature.frequencia.repository.ChamadaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AulaServiceTest {

    @Mock
    private AulaRepository aulaRepository;

    @Mock
    private CursoRepository cursoRepository;

    @Mock
    private ChamadaRepository chamadaRepository;

    @Mock
    private AulaConverter aulaConverter;

    @Mock
    private EntityManager entityManager;

    @Mock
    private FrequenciaService frequenciaService;

    @InjectMocks
    private AulaService aulaService;

    private Curso curso;
    private Aula aulaValida;

    @BeforeEach
    void setUp() {
        curso = new Curso();
        curso.setId(1L);
        curso.setCargaHoraria("02:00:00"); // 120 minutes
        curso.setRealizacaoDataInicial(LocalDateTime.of(2023, 1, 1, 0, 0));
        curso.setRealizacaoDataFinal(LocalDateTime.of(2023, 1, 31, 23, 59));
        curso.setStatus(StatusCursoEnum.EM_CONSTRUCAO);

        aulaValida = new Aula();
        aulaValida.setId(10L);
        aulaValida.setData(LocalDate.of(2023, 1, 10));
        aulaValida.setHoraInicio(LocalTime.of(14, 0));
        aulaValida.setHoraFim(LocalTime.of(15, 0)); // 60 minutes
        aulaValida.setDescricao("Aula 1");
    }

    @Test
    void createAula_Success() {
        when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
        when(aulaRepository.findAllByCursoId(1L)).thenReturn(List.of(aulaValida));
        when(aulaRepository.save(any(Aula.class))).thenReturn(aulaValida);

        Aula result = aulaService.createAula(1L, aulaValida);

        assertNotNull(result);
        assertEquals(StatusCursoEnum.PUBLICADO, curso.getStatus());
        assertEquals("01:00:00", curso.getCargaHoraria());
        assertEquals(LocalDateTime.of(2023, 1, 10, 14, 0), curso.getRealizacaoDataInicial());
        assertEquals(LocalDateTime.of(2023, 1, 10, 15, 0), curso.getRealizacaoDataFinal());
        verify(aulaRepository, times(1)).save(aulaValida);
        verify(cursoRepository, times(1)).save(curso);
        verify(frequenciaService).consolidarFrequenciasPorCurso(1L);
    }

    @Test
    void createAula_SuccessAndChangesStatusToProntoPublicacao() {
        when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
        
        Aula aulaExistente = new Aula();
        aulaExistente.setId(11L);
        aulaExistente.setData(LocalDate.of(2023, 1, 9));
        aulaExistente.setHoraInicio(LocalTime.of(10, 0));
        aulaExistente.setHoraFim(LocalTime.of(11, 0)); // 60 minutes

        when(aulaRepository.findAllByCursoId(1L))
            .thenReturn(List.of(aulaExistente, aulaValida)); // Segunda chamada (recalcularStatusCurso)
        when(aulaRepository.save(any(Aula.class))).thenReturn(aulaValida);

        Aula result = aulaService.createAula(1L, aulaValida);

        assertNotNull(result);
        assertEquals(StatusCursoEnum.PUBLICADO, curso.getStatus());
        assertEquals("02:00:00", curso.getCargaHoraria());
    }

    @Test
    void createAula_FailsWhenCourseDoesNotExist() {
        when(cursoRepository.findById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> aulaService.createAula(1L, aulaValida));

        assertTrue(exception.getMessage().contains("Curso não encontrado"));
    }

    @Test
    void createAula_FailsWhenTimeIsInvalid() {
        when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
        
        aulaValida.setHoraInicio(LocalTime.of(15, 0));
        aulaValida.setHoraFim(LocalTime.of(14, 0)); // Fim antes de Início

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> {
            aulaService.createAula(1L, aulaValida);
        });

        assertTrue(exception.getMessage().contains("Hora fim deve ser maior que a hora início"));
    }
}
