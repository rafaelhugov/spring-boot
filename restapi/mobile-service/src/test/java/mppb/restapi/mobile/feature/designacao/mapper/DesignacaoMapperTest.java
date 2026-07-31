package mppb.restapi.mobile.feature.designacao.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import mppb.restapi.mobile.feature.designacao.dto.DesignacoesAtivasResponse;
import mppb.restapi.mobile.feature.designacao.model.DesignacaoAtivaRow;

class DesignacaoMapperTest {

    private final DesignacaoMapper mapper = new DesignacaoMapper();

    @Test
    void mapeiaSubstituicaoDeMembroComoSubstituicaoCumulativa() {
        DesignacaoAtivaRow row = new DesignacaoAtivaRow(
                1002L,
                "12345",
                "S",
                "Substituição",
                null,
                "15º Procurador de Justiça",
                null,
                LocalDate.of(2026, 6, 16),
                LocalDate.of(2026, 7, 30),
                LocalDate.of(2026, 6, 15),
                "456/2026",
                false,
                "M",
                "Membro");

        DesignacoesAtivasResponse response = mapper.toResponse(List.of(row));

        assertEquals("MEMBRO", response.getTipoUsuario());
        assertEquals("Substituição Cumulativa", response.getDesignacoes().get(0).getTipoDescricao());
        assertEquals("15º Procurador de Justiça", response.getDesignacoes().get(0).getTitulo());
    }

    @Test
    void mapeiaLotacaoDeServidorPeloCodigoRealDaView() {
        DesignacaoAtivaRow row = new DesignacaoAtivaRow(
                2001L,
                "12345",
                "L",
                "Lotação",
                null,
                "Departamento de Desenvolvimento de Sistemas",
                null,
                LocalDate.of(2015, 10, 27),
                null,
                null,
                null,
                false,
                "S",
                "Servidor");

        DesignacoesAtivasResponse response = mapper.toResponse(List.of(row));

        assertEquals("SERVIDOR", response.getTipoUsuario());
        assertEquals("Lotação", response.getDesignacoes().get(0).getTipoDescricao());
        assertEquals("Departamento de Desenvolvimento de Sistemas", response.getDesignacoes().get(0).getTitulo());
    }

    @Test
    void mapeiaFuncaoComOrgaoComoSubtituloPrioritario() {
        DesignacaoAtivaRow row = new DesignacaoAtivaRow(
                2002L,
                "12345",
                "F",
                "Função",
                "Chefe do Departamento de Desenvolvimento de Sistemas",
                "Departamento de Desenvolvimento de Sistemas",
                "Promotoria de Justiça de Cabedelo",
                LocalDate.of(2025, 9, 2),
                null,
                LocalDate.of(2025, 9, 1),
                "1278/2025 DIADM",
                false,
                "S",
                "Servidor");

        DesignacoesAtivasResponse response = mapper.toResponse(List.of(row));

        assertEquals("Função", response.getDesignacoes().get(0).getTipoDescricao());
        assertEquals("Chefe do Departamento de Desenvolvimento de Sistemas", response.getDesignacoes().get(0).getTitulo());
        assertEquals("Departamento de Desenvolvimento de Sistemas", response.getDesignacoes().get(0).getSubtitulo());
    }

    @Test
    void omiteAtoEDataPublicacaoDeDocumentoInicialSemOcultarDesignacao() {
        DesignacaoAtivaRow row = new DesignacaoAtivaRow(
                1001L,
                "12345",
                "T",
                "Titular",
                null,
                "16º Procurador de Justiça",
                null,
                LocalDate.of(2019, 1, 7),
                null,
                LocalDate.of(1900, 1, 1),
                "INI_000001",
                true,
                "M",
                "Membro");

        DesignacoesAtivasResponse response = mapper.toResponse(List.of(row));

        assertEquals(1, response.getDesignacoes().size());
        assertEquals("Titularidade", response.getDesignacoes().get(0).getTipoDescricao());
        assertEquals("16º Procurador de Justiça", response.getDesignacoes().get(0).getTitulo());
        assertEquals(LocalDate.of(2019, 1, 7), response.getDesignacoes().get(0).getDataInicial());
        assertEquals(null, response.getDesignacoes().get(0).getAto());
        assertEquals(null, response.getDesignacoes().get(0).getDataPublicacao());
    }

    @Test
    void documentoComIniNoMeioNaoETratadoComoInicial() {
        DesignacaoAtivaRow row = new DesignacaoAtivaRow(
                1003L,
                "12345",
                "S",
                "Substituição",
                null,
                "15º Procurador de Justiça",
                null,
                LocalDate.of(2026, 6, 16),
                LocalDate.of(2026, 7, 30),
                LocalDate.of(2026, 6, 15),
                "456/2026",
                false,
                "M",
                "Membro");

        DesignacoesAtivasResponse response = mapper.toResponse(List.of(row));

        assertEquals("456/2026", response.getDesignacoes().get(0).getAto());
        assertEquals(LocalDate.of(2026, 6, 15), response.getDesignacoes().get(0).getDataPublicacao());
    }

    @Test
    void omiteAtoQuandoNumeroDoAtoComecaComIniMesmoSemFlagDoDocumento() {
        DesignacaoAtivaRow row = new DesignacaoAtivaRow(
                2003L,
                "12345",
                "F",
                "Função",
                "Exercer atribuições",
                "Promotoria de Justiça de João Pessoa - Violência Doméstica",
                null,
                LocalDate.of(2025, 10, 23),
                null,
                LocalDate.of(2025, 10, 22),
                " INI_EDUARDO_promMulher",
                false,
                "S",
                "Servidor");

        DesignacoesAtivasResponse response = mapper.toResponse(List.of(row));

        assertEquals("Exercer atribuições", response.getDesignacoes().get(0).getTitulo());
        assertEquals(LocalDate.of(2025, 10, 23), response.getDesignacoes().get(0).getDataInicial());
        assertEquals(null, response.getDesignacoes().get(0).getAto());
        assertEquals(null, response.getDesignacoes().get(0).getDataPublicacao());
    }

    @Test
    void mantemAtoQuandoIniApareceNoMeioDoNumero() {
        DesignacaoAtivaRow row = new DesignacaoAtivaRow(
                2004L,
                "12345",
                "F",
                "Função",
                "Exercer atribuições",
                "Promotoria de Justiça de João Pessoa",
                null,
                LocalDate.of(2025, 10, 23),
                null,
                LocalDate.of(2025, 10, 22),
                "ATO_INI_ENQUADRAMENTO_EFETIVOS",
                false,
                "S",
                "Servidor");

        DesignacoesAtivasResponse response = mapper.toResponse(List.of(row));

        assertEquals("ATO_INI_ENQUADRAMENTO_EFETIVOS", response.getDesignacoes().get(0).getAto());
        assertEquals(LocalDate.of(2025, 10, 22), response.getDesignacoes().get(0).getDataPublicacao());
    }
}
