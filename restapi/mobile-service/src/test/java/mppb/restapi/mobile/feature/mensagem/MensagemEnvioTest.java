package mppb.restapi.mobile.feature.mensagem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import mppb.restapi.mobile.feature.mensagem.controller.MensagemRemetenteController;
import mppb.restapi.mobile.feature.mensagem.dto.CriarMensagemRequest;
import mppb.restapi.mobile.feature.mensagem.dto.MensagemEnviadaResponse;
import mppb.restapi.mobile.feature.mensagem.entity.MensagemAnexo;
import mppb.restapi.mobile.feature.mensagem.entity.MensagemAviso;
import mppb.restapi.mobile.feature.mensagem.enums.CategoriaAnexo;
import mppb.restapi.mobile.feature.mensagem.enums.ModalidadeEnvio;
import mppb.restapi.mobile.feature.mensagem.enums.PrioridadeMensagem;
import mppb.restapi.mobile.feature.mensagem.enums.StatusPublicacao;
import mppb.restapi.mobile.feature.mensagem.repository.GrupoMensagemMembroRepository;
import mppb.restapi.mobile.feature.mensagem.repository.MensagemAnexoRepository;
import mppb.restapi.mobile.feature.mensagem.repository.MensagemAvisoRepository;
import mppb.restapi.mobile.feature.mensagem.repository.MensagemDestinatarioRepository;
import mppb.restapi.mobile.feature.mensagem.repository.RhDestinatarioRepository;
import mppb.restapi.mobile.feature.mensagem.repository.RhDestinatarioRepository.DestinatarioRh;
import mppb.restapi.mobile.feature.mensagem.service.MensagemEnvioService;
import mppb.restapi.mobile.feature.mensagem.service.MensagemPushService;
import mppb.restapi.mobile.feature.mensagem.service.MensagemStorageProperties;
import mppb.restapi.mobile.feature.mensagem.service.MensagemStorageService;
import mppb.restapi.mobile.feature.mensagem.service.MensagemStorageService.ArquivoArmazenado;
import mppb.restapi.mobile.feature.perfil.dto.PerfilUsuarioDto;
import mppb.restapi.mobile.feature.perfil.repository.PerfilUsuarioRepository;

class MensagemEnvioTest {

    @TempDir
    Path tempDir;

    private MensagemAvisoRepository avisoRepo;
    private MensagemDestinatarioRepository destRepo;
    private MensagemAnexoRepository anexoRepo;
    private GrupoMensagemMembroRepository grupoMembroRepo;
    private RhDestinatarioRepository rhRepo;
    private PerfilUsuarioRepository perfilRepo;
    private MensagemPushService pushService;
    private MensagemEnvioService envioService;
    private MensagemStorageService storageService;

    @BeforeEach
    void setUp() {
        avisoRepo = mock(MensagemAvisoRepository.class);
        destRepo = mock(MensagemDestinatarioRepository.class);
        anexoRepo = mock(MensagemAnexoRepository.class);
        grupoMembroRepo = mock(GrupoMensagemMembroRepository.class);
        rhRepo = mock(RhDestinatarioRepository.class);
        perfilRepo = mock(PerfilUsuarioRepository.class);
        pushService = mock(MensagemPushService.class);

        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        envioService = new MensagemEnvioService(
                avisoRepo, destRepo, anexoRepo, grupoMembroRepo, rhRepo, perfilRepo, pushService, mapper);

        MensagemStorageProperties props = new MensagemStorageProperties();
        props.setStoragePath(tempDir.toString());
        props.setMaxImagemBytes(1024);
        storageService = new MensagemStorageService(props);
    }

    @Test
    @DisplayName("Deve exigir anotação @PreAuthorize no controller do remetente")
    void deveExigirPreAuthorizeNoController() {
        PreAuthorize annotation = MensagemRemetenteController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation);
        assertTrue(annotation.value().contains("authorityAdmin"));
    }

    @Test
    @DisplayName("Deve enviar aviso Broadcast somente com áudio (sem título e sem texto)")
    void deveEnviarBroadcastSomenteComAudio() {
        PerfilUsuarioDto remetente = new PerfilUsuarioDto();
        remetente.setMatricula("1001");
        remetente.setNome("PGJ Teste");
        when(perfilRepo.buscarPorLogin("pgj.teste")).thenReturn(remetente);

        when(rhRepo.listarTodosAtivos()).thenReturn(List.of(
                new DestinatarioRh("2001", "Servidor A", "Setor 1", "Analista", "S"),
                new DestinatarioRh("2002", "Membro B", "Promotoria 1", "Promotor", "M")));

        when(avisoRepo.save(any(MensagemAviso.class))).thenAnswer(inv -> {
            MensagemAviso a = inv.getArgument(0);
            a.setId(99L);
            return a;
        });

        MensagemAnexo audio = new MensagemAnexo();
        audio.setId(55L);
        audio.setCategoria(CategoriaAnexo.AUDIO);
        audio.setNomeOriginal("aviso.m4a");
        audio.setMimeType("audio/mp4");
        audio.setTamanhoBytes(2048L);
        audio.setDuracaoSegundos(18);
        when(anexoRepo.findByIdIn(List.of(55L))).thenReturn(List.of(audio));
        when(anexoRepo.saveAll(anyList())).thenReturn(List.of(audio));
        when(destRepo.countByMensagemId(99L)).thenReturn(2L);

        CriarMensagemRequest request = new CriarMensagemRequest(
                null, null, PrioridadeMensagem.URGENTE, ModalidadeEnvio.BROADCAST, true,
                null, List.of(55L), null, LocalDateTime.now().plusDays(7));

        MensagemEnviadaResponse response = envioService.enviarOuAgendar("pgj.teste", request);

        assertEquals(99L, response.id());
        assertEquals(StatusPublicacao.ENVIADA, response.statusPublicacao());
        assertEquals(2L, response.totalDestinatarios());
        verify(destRepo).saveAll(anyList());
        verify(pushService).notificarDestinatarios(any(), anyList(), anyList());
    }

    @Test
    @DisplayName("Deve rejeitar envio sem texto e sem anexos")
    void deveRejeitarEnvioVazio() {
        CriarMensagemRequest request = new CriarMensagemRequest(
                "Título", "   ", PrioridadeMensagem.NORMAL, ModalidadeEnvio.BROADCAST, false,
                null, List.of(), null, null);

        assertThrows(IllegalArgumentException.class, () -> envioService.enviarOuAgendar("pgj.teste", request));
    }

    @Test
    @DisplayName("Deve validar extensões permitidas (md, m4a) e rejeitar executáveis ou arquivos acima do limite")
    void deveValidarStorageDeMidias() {
        MockMultipartFile md = new MockMultipartFile("arquivo", "nota.md", "text/markdown", "# Aviso".getBytes());
        ArquivoArmazenado salvo = storageService.armazenar(md);
        assertEquals(CategoriaAnexo.DOCUMENTO, salvo.categoria());

        MockMultipartFile exe = new MockMultipartFile("arquivo", "script.exe", "application/octet-stream", new byte[] {1, 2});
        assertThrows(IllegalArgumentException.class, () -> storageService.armazenar(exe));

        MockMultipartFile imgGrande = new MockMultipartFile("arquivo", "foto.png", "image/png", new byte[2048]);
        assertThrows(IllegalArgumentException.class, () -> storageService.armazenar(imgGrande));
    }
}

