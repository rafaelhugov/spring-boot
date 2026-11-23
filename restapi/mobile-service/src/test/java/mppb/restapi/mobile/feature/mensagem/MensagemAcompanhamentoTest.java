package mppb.restapi.mobile.feature.mensagem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;

import com.fasterxml.jackson.databind.ObjectMapper;

import mppb.restapi.mobile.feature.mensagem.controller.MensagemAcompanhamentoController;
import mppb.restapi.mobile.feature.mensagem.dto.AcompanhamentoMensagemResponse;
import mppb.restapi.mobile.feature.mensagem.dto.MensagensEnviadasPageResponse;
import mppb.restapi.mobile.feature.mensagem.entity.MensagemAnexo;
import mppb.restapi.mobile.feature.mensagem.entity.MensagemAviso;
import mppb.restapi.mobile.feature.mensagem.entity.MensagemDestinatario;
import mppb.restapi.mobile.feature.mensagem.enums.CategoriaAnexo;
import mppb.restapi.mobile.feature.mensagem.enums.ModalidadeEnvio;
import mppb.restapi.mobile.feature.mensagem.enums.PrioridadeMensagem;
import mppb.restapi.mobile.feature.mensagem.enums.StatusDestinatario;
import mppb.restapi.mobile.feature.mensagem.enums.StatusPublicacao;
import mppb.restapi.mobile.feature.mensagem.repository.GrupoMensagemMembroRepository;
import mppb.restapi.mobile.feature.mensagem.repository.MensagemAnexoRepository;
import mppb.restapi.mobile.feature.mensagem.repository.MensagemAvisoRepository;
import mppb.restapi.mobile.feature.mensagem.repository.MensagemDestinatarioRepository;
import mppb.restapi.mobile.feature.mensagem.repository.RhDestinatarioRepository;
import mppb.restapi.mobile.feature.mensagem.service.MensagemAcompanhamentoService;
import mppb.restapi.mobile.feature.mensagem.service.MensagemEnvioService;
import mppb.restapi.mobile.feature.mensagem.service.MensagemPushService;
import mppb.restapi.mobile.feature.perfil.repository.PerfilUsuarioRepository;

class MensagemAcompanhamentoTest {

    private MensagemAvisoRepository avisoRepo;
    private MensagemDestinatarioRepository destRepo;
    private MensagemAnexoRepository anexoRepo;
    private MensagemAcompanhamentoService service;

    @BeforeEach
    void setUp() {
        avisoRepo = mock(MensagemAvisoRepository.class);
        destRepo = mock(MensagemDestinatarioRepository.class);
        anexoRepo = mock(MensagemAnexoRepository.class);

        MensagemEnvioService envioService = new MensagemEnvioService(
                avisoRepo,
                destRepo,
                anexoRepo,
                mock(GrupoMensagemMembroRepository.class),
                mock(RhDestinatarioRepository.class),
                mock(PerfilUsuarioRepository.class),
                mock(MensagemPushService.class),
                new ObjectMapper());

        service = new MensagemAcompanhamentoService(avisoRepo, destRepo, anexoRepo, envioService);
    }

    @Test
    @DisplayName("Deve exigir anotação @PreAuthorize no controller de acompanhamento")
    void deveExigirPreAuthorizeNoController() {
        PreAuthorize annotation = MensagemAcompanhamentoController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation);
        assertTrue(annotation.value().contains("authorityAdmin"));
    }

    @Test
    @DisplayName("Deve listar mensagens enviadas paginadas com totais consolidados")
    void deveListarEnviadasComContadoresConsolidados() {
        MensagemAviso aviso = criarAviso(10L, "Aviso Geral");
        when(avisoRepo.findByRemetenteMatriculaAndArquivadaRemetenteOrderByDataRegistroDesc(
                "900001", false, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(aviso), PageRequest.of(0, 20), 1));

        MensagemAnexo anexo = new MensagemAnexo();
        anexo.setId(5L);
        anexo.setCategoria(CategoriaAnexo.DOCUMENTO);
        anexo.setNomeOriginal("comunicado.pdf");
        anexo.setMimeType("application/pdf");
        anexo.setTamanhoBytes(1024L);
        when(anexoRepo.findByMensagemIdOrderByIdAsc(10L)).thenReturn(List.of(anexo));

        when(destRepo.countByMensagemId(10L)).thenReturn(10L);
        when(destRepo.countByMensagemIdAndStatus(10L, StatusDestinatario.ENTREGUE)).thenReturn(3L);
        when(destRepo.countByMensagemIdAndStatus(10L, StatusDestinatario.LIDO)).thenReturn(5L);
        when(destRepo.countByMensagemIdAndDataCienciaIsNotNull(10L)).thenReturn(4L);

        MensagensEnviadasPageResponse resp = service.listarEnviadas("uat.membro", false, 0, 20);

        assertEquals(1, resp.itens().size());
        assertEquals(1L, resp.totalElementos());
        assertEquals(10L, resp.itens().get(0).totalDestinatarios());
        assertEquals(8L, resp.itens().get(0).totalEntregues());
        assertEquals(5L, resp.itens().get(0).totalLidos());
        assertEquals(4L, resp.itens().get(0).totalCientes());
        assertEquals(2L, resp.itens().get(0).totalPendentes());
        assertEquals(1, resp.itens().get(0).anexos().size());
    }

    @Test
    @DisplayName("Deve detalhar acompanhamento aplicando filtro de status e busca normalizada")
    void deveDetalharAcompanhamentoComFiltros() {
        MensagemAviso aviso = criarAviso(10L, "Convocação");
        when(avisoRepo.findById(10L)).thenReturn(Optional.of(aviso));
        when(anexoRepo.findByMensagemIdOrderByIdAsc(10L)).thenReturn(List.of());
        when(destRepo.countByMensagemId(10L)).thenReturn(2L);
        when(destRepo.countByMensagemIdAndStatus(10L, StatusDestinatario.ENTREGUE)).thenReturn(1L);
        when(destRepo.countByMensagemIdAndStatus(10L, StatusDestinatario.LIDO)).thenReturn(1L);
        when(destRepo.countByMensagemIdAndDataCienciaIsNotNull(10L)).thenReturn(1L);

        MensagemDestinatario dest = new MensagemDestinatario();
        dest.setMensagemId(10L);
        dest.setDestinatarioMatricula("2001");
        dest.setDestinatarioNome("Maria Silva");
        dest.setDestinatarioSetor("DTI");
        dest.setStatus(StatusDestinatario.LIDO);
        dest.setDataEntrega(LocalDateTime.of(2026, 11, 18, 9, 0));
        dest.setDataLeitura(LocalDateTime.of(2026, 11, 18, 9, 5));
        dest.setDataCiencia(LocalDateTime.of(2026, 11, 18, 9, 6));

        when(destRepo.filtrarAcompanhamento(
                eq(10L),
                eq(StatusDestinatario.LIDO),
                eq("%maria%"),
                eq(PageRequest.of(0, 20))))
                .thenReturn(new PageImpl<>(List.of(dest), PageRequest.of(0, 20), 1));

        AcompanhamentoMensagemResponse resp = service.detalhar("uat.membro", 10L, StatusDestinatario.LIDO, "  Maria ", 0, 20);

        assertEquals(10L, resp.resumo().id());
        assertEquals(1, resp.destinatarios().size());
        assertEquals("2001", resp.destinatarios().get(0).matricula());
        assertEquals(StatusDestinatario.LIDO, resp.destinatarios().get(0).status());
        assertNotNull(resp.destinatarios().get(0).dataCiencia());
    }

    @Test
    @DisplayName("Deve enviar busca nula quando termo estiver em branco e falhar se mensagem não existir")
    void deveNormalizarBuscaVaziaEFalharQuandoNaoEncontrada() {
        MensagemAviso aviso = criarAviso(20L, "Aviso");
        when(avisoRepo.findById(20L)).thenReturn(Optional.of(aviso));
        when(anexoRepo.findByMensagemIdOrderByIdAsc(20L)).thenReturn(List.of());
        when(destRepo.filtrarAcompanhamento(eq(20L), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        AcompanhamentoMensagemResponse resp = service.detalhar("uat.membro", 20L, null, "   ", -1, 500);
        assertEquals(0, resp.destinatarios().size());
        verify(destRepo).filtrarAcompanhamento(20L, null, null, PageRequest.of(0, 100));

        assertThrows(AccessDeniedException.class, () -> service.detalhar("uat.servidor", 20L, null, null, 0, 20));

        when(avisoRepo.findById(999L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.detalhar("uat.membro", 999L, null, null, 0, 20));
    }

    private MensagemAviso criarAviso(Long id, String titulo) {
        MensagemAviso aviso = new MensagemAviso();
        aviso.setId(id);
        aviso.setRemetenteMatricula("900001");
        aviso.setTitulo(titulo);
        aviso.setConteudo("Conteúdo do aviso");
        aviso.setPrioridade(PrioridadeMensagem.NORMAL);
        aviso.setModalidade(ModalidadeEnvio.BROADCAST);
        aviso.setExigeCiencia(true);
        aviso.setStatusPublicacao(StatusPublicacao.ENVIADA);
        aviso.setDataEnvio(LocalDateTime.of(2026, 11, 18, 8, 0));
        return aviso;
    }
}
