package mppb.restapi.mobile.feature.processoconsulta.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import mppb.restapi.mobile.feature.processoconsulta.dto.ConsultaProcessoFilterDTO;
import mppb.restapi.mobile.feature.processoconsulta.dto.ConsultaProcessoResponseDTO;
import mppb.restapi.mobile.feature.processoconsulta.dto.ProcessoFiltroOptionDTO;
import mppb.restapi.mobile.feature.processoconsulta.dto.ProcessoFiltrosDTO;
import mppb.restapi.mobile.feature.processoconsulta.repository.ProtocoloConsultaRepository;

class ProcessoConsultaServiceTest {

    private ProcessoConsultaService service;
    private ProtocoloConsultaRepository repository;

    @BeforeEach
    void setUp() {
        service = new ProcessoConsultaService();
        repository = new FakeProtocoloConsultaRepository();
        ReflectionTestUtils.setField(service, "repository", repository);
    }

    @Test
    void listaFiltrosComValoresDoLegado() {
        ProcessoFiltrosDTO filtros = service.listarFiltros();

        assertEquals("10", filtros.getComarcas().get(0).getId());
        assertEquals("200910004", filtros.getClasses().get(0).getId());
        assertEquals("Inquérito Civil", filtros.getClasses().get(0).getLabel());
        assertEquals("T", filtros.getTiposAtividade().get(0).getId());
        assertEquals("Termos de Ajustamento de Conduta firmados", filtros.getTiposAtividade().get(0).getLabel());
        assertEquals("T", filtros.getStatus().get(0).getId());
        assertEquals("Todos", filtros.getStatus().get(0).getLabel());
    }

    @Test
    void aceitaCadaCampoVisivelComoFiltroMinimo() {
        assertFiltroValido(f -> f.setNumeroProcesso("001"));
        assertFiltroValido(f -> f.setNumeroOrigem("0800000"));
        assertFiltroValido(f -> f.setNumeroCnmp("1.24"));
        assertFiltroValido(f -> f.setCpf("12345678901"));
        assertFiltroValido(f -> f.setCnpj("12345678000199"));
        assertFiltroValido(f -> f.setInteressado("Maria"));
        assertFiltroValido(f -> f.setIdComarca(10L));
        assertFiltroValido(f -> f.setIdClasse(200910004L));
        assertFiltroValido(f -> f.setNomePromotor("Maria"));
        assertFiltroValido(f -> f.setIdTipoAtividade("T"));
        assertFiltroValido(f -> f.setStatus("A"));
    }

    @Test
    void rejeitaFormularioVazio() {
        ConsultaProcessoFilterDTO filtro = new ConsultaProcessoFilterDTO();

        assertThrows(IllegalArgumentException.class, () -> service.consultarProcessos(filtro));
    }

    private void assertFiltroValido(java.util.function.Consumer<ConsultaProcessoFilterDTO> setup) {
        ConsultaProcessoFilterDTO filtro = new ConsultaProcessoFilterDTO();
        setup.accept(filtro);
        assertDoesNotThrow(() -> service.consultarProcessos(filtro));
    }

    private static class FakeProtocoloConsultaRepository extends ProtocoloConsultaRepository {

        @Override
        public List<ProcessoFiltroOptionDTO> listarComarcas() {
            return List.of(new ProcessoFiltroOptionDTO("10", "Promotoria A"));
        }

        @Override
        public List<ConsultaProcessoResponseDTO> consultarProcessos(ConsultaProcessoFilterDTO filtro) {
            return List.of();
        }
    }
}
