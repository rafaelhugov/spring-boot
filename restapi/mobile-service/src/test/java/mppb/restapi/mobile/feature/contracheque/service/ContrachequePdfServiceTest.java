package mppb.restapi.mobile.feature.contracheque.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import mppb.restapi.mobile.feature.contracheque.dto.ContrachequeResponseDto;
import mppb.restapi.mobile.feature.contracheque.dto.LancamentoDto;

class ContrachequePdfServiceTest {

    private final ContrachequePdfService pdfService = new ContrachequePdfService();

    @Test
    void deveOrdenarVantagensPrimeiroEDepoisDescontos() {
        List<LancamentoDto> lancamentos = new ArrayList<>();

        LancamentoDto desc1 = new LancamentoDto();
        desc1.setCodigo("501");
        desc1.setDescricao("PREVIDÊNCIA");
        desc1.setTipo("D");
        desc1.setValor(new BigDecimal("1500.00"));
        lancamentos.add(desc1);

        LancamentoDto vant1 = new LancamentoDto();
        vant1.setCodigo("101");
        vant1.setDescricao("VENCIMENTO BÁSICO");
        vant1.setTipo("V");
        vant1.setValor(new BigDecimal("10000.00"));
        lancamentos.add(vant1);

        LancamentoDto desc2 = new LancamentoDto();
        desc2.setCodigo("502");
        desc2.setDescricao("IMPOSTO DE RENDA");
        desc2.setTipo("D");
        desc2.setValor(new BigDecimal("2000.00"));
        lancamentos.add(desc2);

        LancamentoDto vant2 = new LancamentoDto();
        vant2.setCodigo("102");
        vant2.setDescricao("GRATIFICAÇÃO");
        vant2.setTipo("V");
        vant2.setValor(new BigDecimal("3000.00"));
        lancamentos.add(vant2);

        List<LancamentoDto> ordenados = pdfService.ordenarLancamentos(lancamentos);

        assertEquals(4, ordenados.size());
        assertEquals("V", ordenados.get(0).getTipo());
        assertEquals("101", ordenados.get(0).getCodigo());

        assertEquals("V", ordenados.get(1).getTipo());
        assertEquals("102", ordenados.get(1).getCodigo());

        assertEquals("D", ordenados.get(2).getTipo());
        assertEquals("501", ordenados.get(2).getCodigo());

        assertEquals("D", ordenados.get(3).getTipo());
        assertEquals("502", ordenados.get(3).getCodigo());
    }

    @Test
    void deveGerarPdfComSucessoComLancamentosOrdenados() throws Exception {
        ContrachequeResponseDto dto = new ContrachequeResponseDto();
        dto.setNome("Servidor Teste");
        dto.setCargo("Analista");
        dto.setMatricula("12345");
        dto.setMesAno("10/2026");
        dto.setCodigoValidacao("VALID123");
        dto.setTotalVantagens(new BigDecimal("13000.00"));
        dto.setTotalDescontos(new BigDecimal("3500.00"));
        dto.setSalarioLiquido(new BigDecimal("9500.00"));

        List<LancamentoDto> lancamentos = new ArrayList<>();
        LancamentoDto d = new LancamentoDto();
        d.setCodigo("501");
        d.setDescricao("IRRF");
        d.setTipo("D");
        d.setValor(new BigDecimal("3500.00"));
        lancamentos.add(d);

        LancamentoDto v = new LancamentoDto();
        v.setCodigo("101");
        v.setDescricao("SUBSÍDIO");
        v.setTipo("V");
        v.setValor(new BigDecimal("13000.00"));
        lancamentos.add(v);

        dto.setLancamentos(lancamentos);

        byte[] pdf = pdfService.generatePdf(dto);

        assertNotNull(pdf);
        assertTrue(pdf.length > 100);
        assertTrue(new String(pdf, StandardCharsets.ISO_8859_1).contains("%PDF"));
    }
}
