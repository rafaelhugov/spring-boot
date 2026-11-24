package mppb.restapi.cursos.feature.participante.service;

import jakarta.persistence.EntityManager;
import mppb.restapi.cursos.feature.common.projection.ServidorProjection;
import mppb.restapi.cursos.feature.common.service.ServidorService;
import mppb.restapi.cursos.feature.curso.repository.CursoRepository;
import mppb.restapi.cursos.feature.inscricao.dto.InscricaoDto;
import mppb.restapi.cursos.feature.inscricao.util.InscricaoExcelExporter;
import mppb.restapi.cursos.feature.participante.converter.ParticipanteConverter;
import mppb.restapi.cursos.feature.participante.dto.ParticipanteDto;
import mppb.restapi.cursos.feature.participante.entity.Participante;
import mppb.restapi.cursos.feature.participante.enums.TipoParticipanteEnum;
import mppb.restapi.cursos.feature.participante.repository.ParticipanteRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParticipanteServiceDesignacaoLotacaoTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private ParticipanteRepository participanteRepository;

    @Mock
    private ServidorService servidorService;

    @Mock
    private CursoRepository cursoRepository;

    private ParticipanteService createService() {
        ParticipanteConverter converter = new ParticipanteConverter(new ModelMapper());
        return new ParticipanteService(entityManager, participanteRepository, servidorService, converter, cursoRepository);
    }

    @Test
    void toEnrichedDtoDevePreencherLotacaoEDesignacaoParaParticipanteInterno() {
        ParticipanteService service = createService();

        Participante interno = Participante.builder()
                .idServidor(123)
                .tipo(TipoParticipanteEnum.INTERNO)
                .nome("Nome Antigo")
                .email("antigo@mppb.mp.br")
                .build();
        interno.setId(1L);

        ServidorProjection servidor = mock(ServidorProjection.class);
        when(servidor.getNome()).thenReturn("Servidor Atualizado");
        when(servidor.getCargoNome()).thenReturn("Analista Ministerial");
        when(servidor.getEmail()).thenReturn("servidor@mppb.mp.br");
        when(servidor.getLotacaoNome()).thenReturn("Diretoria de Tecnologia da Informação");
        when(servidor.getDesignacaoNome()).thenReturn("Coordenador de Sistemas");

        when(servidorService.buscarServidorPorId(123)).thenReturn(Optional.of(servidor));

        ParticipanteDto dto = service.toEnrichedDto(interno);

        assertEquals("Servidor Atualizado", dto.getNome());
        assertEquals("Analista Ministerial", dto.getCargo());
        assertEquals("servidor@mppb.mp.br", dto.getEmail());
        assertEquals("Diretoria de Tecnologia da Informação", dto.getLotacao());
        assertEquals("Coordenador de Sistemas", dto.getDesignacao());
    }

    @Test
    void toEnrichedDtoDeveManterLotacaoEDesignacaoNulosParaParticipanteExterno() {
        ParticipanteService service = createService();

        Participante externo = Participante.builder()
                .tipo(TipoParticipanteEnum.EXTERNO)
                .nome("Convidado Externo")
                .email("externo@exemplo.com")
                .build();
        externo.setId(2L);

        ParticipanteDto dto = service.toEnrichedDto(externo);

        assertEquals("Convidado Externo", dto.getNome());
        assertNull(dto.getLotacao());
        assertNull(dto.getDesignacao());
    }

    @Test
    void exportToExcelDeveIncluirLotacaoEDesignacaoApenasParaInternos() throws Exception {
        ParticipanteDto interno = new ParticipanteDto();
        interno.setNome("Servidor MPPB");
        interno.setEmail("interno@mppb.mp.br");
        interno.setCargo("Promotor de Justiça");
        interno.setTipo(TipoParticipanteEnum.INTERNO);
        interno.setLotacao("1ª Promotoria de Justiça");
        interno.setDesignacao("2ª Promotoria de Justiça");

        InscricaoDto inscricaoInterna = new InscricaoDto();
        inscricaoInterna.setParticipante(interno);
        inscricaoInterna.setPcd(false);
        inscricaoInterna.setSituacao("INSCRITO");
        inscricaoInterna.setDataInscricao(LocalDateTime.of(2026, 11, 24, 15, 0));

        ParticipanteDto externo = new ParticipanteDto();
        externo.setNome("Participante Externo");
        externo.setEmail("externo@exemplo.com");
        externo.setTipo(TipoParticipanteEnum.EXTERNO);
        externo.setLotacao("Valor Indevido");
        externo.setDesignacao("Valor Indevido");

        InscricaoDto inscricaoExterna = new InscricaoDto();
        inscricaoExterna.setParticipante(externo);
        inscricaoExterna.setPcd(false);
        inscricaoExterna.setSituacao("INSCRITO");

        try (ByteArrayInputStream in = InscricaoExcelExporter.exportToExcel(List.of(inscricaoInterna, inscricaoExterna));
             Workbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheet("Participantes");
            Row header = sheet.getRow(0);
            assertEquals("Lotação", header.getCell(3).getStringCellValue());
            assertEquals("Designação", header.getCell(4).getStringCellValue());

            Row rowInterno = sheet.getRow(1);
            assertEquals("1ª Promotoria de Justiça", rowInterno.getCell(3).getStringCellValue());
            assertEquals("2ª Promotoria de Justiça", rowInterno.getCell(4).getStringCellValue());

            Row rowExterno = sheet.getRow(2);
            assertEquals("", rowExterno.getCell(3).getStringCellValue());
            assertEquals("", rowExterno.getCell(4).getStringCellValue());
        }
    }
}
