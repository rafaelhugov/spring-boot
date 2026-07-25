package mppb.restapi.cursos.feature.ministrante.service;

import jakarta.persistence.EntityManager;
import mppb.restapi.common.core.error.BusinessRuleException;
import mppb.restapi.common.core.error.NotFoundException;
import mppb.restapi.cursos.feature.common.projection.ServidorProjection;
import mppb.restapi.cursos.feature.common.service.ServidorService;
import mppb.restapi.cursos.feature.curso.converter.CursoMinistranteConverter;
import mppb.restapi.cursos.feature.curso.dto.AssociarMinistranteDto;
import mppb.restapi.cursos.feature.ministrante.converter.MinistranteConverter;
import mppb.restapi.cursos.feature.ministrante.entity.Ministrante;
import mppb.restapi.cursos.feature.ministrante.enums.TipoMinistranteEnum;
import mppb.restapi.cursos.feature.ministrante.repository.MinistranteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinistranteServiceTest {

    @Mock
    private EntityManager entityManager;
    @Mock
    private MinistranteRepository ministranteRepository;
    @Mock
    private MinistranteConverter ministranteConverter;
    @Mock
    private ServidorService servidorService;
    @Mock
    private CursoMinistranteConverter cursoMinistranteConverter;

    @InjectMocks
    private MinistranteService ministranteService;

    @Test
    void findOrCreateMinistranteDeveCriarInternoQuandoServidorNaoExiste() {
        // Given
        AssociarMinistranteDto dto = AssociarMinistranteDto.builder()
                .tipo("INTERNO")
                .idServidor(123)
                .idFoto(10L)
                .idCurriculo(11L)
                .email("servidor@mppb.mp.br")
                .build();
        when(ministranteRepository.findByIdServidor(123)).thenReturn(Optional.empty());
        when(ministranteRepository.existsByIdServidor(123)).thenReturn(false);
        when(ministranteRepository.save(any(Ministrante.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Ministrante resultado = ministranteService.findOrCreateMinistrante(dto);

        // Then
        assertEquals(TipoMinistranteEnum.INTERNO, resultado.getTipo());
        assertEquals(123, resultado.getIdServidor());
        assertEquals(10L, resultado.getIdFoto());
        assertEquals(11L, resultado.getIdCurriculo());
        assertEquals("servidor@mppb.mp.br", resultado.getEmail());
    }

    @Test
    void findOrCreateMinistranteInternoDeveExigirIdServidor() {
        // Given
        AssociarMinistranteDto dto = AssociarMinistranteDto.builder().tipo("INTERNO").build();

        // When / Then
        assertThrows(BusinessRuleException.class, () -> ministranteService.findOrCreateMinistrante(dto));
    }

    @Test
    void findOrCreateMinistranteDeveNormalizarCpfENomeDeExternoNovo() {
        // Given
        AssociarMinistranteDto dto = AssociarMinistranteDto.builder()
                .tipo("EXTERNO")
                .nome("Maria Externa")
                .cpf("529.982.247-25")
                .nomeOrgaoExterno("Parceiro")
                .telefone("83999999999")
                .build();
        when(ministranteRepository.findByCpf("52998224725")).thenReturn(Optional.empty());
        when(ministranteRepository.save(any(Ministrante.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Ministrante resultado = ministranteService.findOrCreateMinistrante(dto);

        // Then
        assertEquals(TipoMinistranteEnum.EXTERNO, resultado.getTipo());
        assertEquals("MARIA EXTERNA", resultado.getNome());
        assertEquals("52998224725", resultado.getCpf());
        assertEquals("Parceiro", resultado.getNomeOrgaoExterno());
        assertEquals("83999999999", resultado.getTelefone());
    }

    @Test
    void findOrCreateMinistranteExternoPorIdDeveFalharQuandoNaoEncontrado() {
        // Given
        AssociarMinistranteDto dto = AssociarMinistranteDto.builder()
                .tipo("EXTERNO")
                .ministranteId(999L)
                .build();
        when(ministranteRepository.findById(999L)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class, () -> ministranteService.findOrCreateMinistrante(dto));
    }

    @Test
    void searchMinistrantesDeveMesclarServidoresEExternos() {
        // Given
        ServidorProjection servidor = mock(ServidorProjection.class);
        when(servidor.getId()).thenReturn(1);
        when(servidor.getNome()).thenReturn("Servidor Interno");
        when(servidorService.buscarServidoresPorNome("mar")).thenReturn(List.of(servidor));

        Ministrante externo = Ministrante.builder()
                .nome("Maria Externa")
                .cpf("52998224725")
                .tipo(TipoMinistranteEnum.EXTERNO)
                .build();
        externo.setId(2L);
        when(ministranteRepository.findAllByTipoAndNomeContainingIgnoreCase(TipoMinistranteEnum.EXTERNO, "mar"))
                .thenReturn(List.of(externo));

        // When
        var resultados = ministranteService.searchMinistrantes("mar");

        // Then
        assertEquals(2, resultados.size());
        assertTrue(resultados.stream().anyMatch(item -> "INTERNO".equals(item.getTipo())));
        assertTrue(resultados.stream().anyMatch(item -> "EXTERNO".equals(item.getTipo())));
        verify(servidorService).buscarServidoresPorNome("mar");
    }
}
