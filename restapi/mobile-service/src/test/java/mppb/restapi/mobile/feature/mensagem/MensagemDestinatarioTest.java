package mppb.restapi.mobile.feature.mensagem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import mppb.restapi.mobile.feature.mensagem.dto.MensagemRecebidaResponse;
import mppb.restapi.mobile.feature.mensagem.entity.MensagemAviso;
import mppb.restapi.mobile.feature.mensagem.entity.MensagemDestinatario;
import mppb.restapi.mobile.feature.mensagem.enums.ModalidadeEnvio;
import mppb.restapi.mobile.feature.mensagem.enums.PrioridadeMensagem;
import mppb.restapi.mobile.feature.mensagem.enums.StatusDestinatario;
import mppb.restapi.mobile.feature.mensagem.repository.MensagemAnexoRepository;
import mppb.restapi.mobile.feature.mensagem.repository.MensagemAvisoRepository;
import mppb.restapi.mobile.feature.mensagem.repository.MensagemDestinatarioRepository;
import mppb.restapi.mobile.feature.mensagem.service.MensagemDestinatarioService;
import mppb.restapi.mobile.feature.perfil.dto.PerfilUsuarioDto;
import mppb.restapi.mobile.feature.perfil.repository.PerfilUsuarioRepository;

class MensagemDestinatarioTest {

    private MensagemDestinatarioRepository destRepo;
    private MensagemAvisoRepository avisoRepo;
    private MensagemAnexoRepository anexoRepo;
    private PerfilUsuarioRepository perfilRepo;
    private MensagemDestinatarioService service;

    @BeforeEach
    void setUp() {
        destRepo = mock(MensagemDestinatarioRepository.class);
        avisoRepo = mock(MensagemAvisoRepository.class);
        anexoRepo = mock(MensagemAnexoRepository.class);
        perfilRepo = mock(PerfilUsuarioRepository.class);
        service = new MensagemDestinatarioService(destRepo, avisoRepo, anexoRepo, perfilRepo);

        PerfilUsuarioDto perfil = new PerfilUsuarioDto();
        perfil.setMatricula("2001");
        when(perfilRepo.buscarPorLogin("servidor.a")).thenReturn(perfil);
    }

    @Test
    @DisplayName("Deve bloquear acesso quando usuário não é destinatário da mensagem (IDOR)")
    void deveBloquearAcessoDeOutroUsuario() {
        when(destRepo.findByMensagemIdAndDestinatarioMatricula(10L, "2001")).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class, () -> service.abrirMensagem("servidor.a", 10L));

        when(destRepo.existsByMensagemIdAndDestinatarioMatricula(10L, "2001")).thenReturn(false);
        assertThrows(AccessDeniedException.class, () -> service.buscarAnexoAutorizado("servidor.a", false, 10L, 1L));
    }

    @Test
    @DisplayName("Deve preservar primeiro timestamp de entrega, leitura e ciência de forma idempotente")
    void devePreservarTimestampsDeLeituraECiencia() {
        LocalDateTime entregaOriginal = LocalDateTime.of(2026, 11, 18, 10, 0);
        LocalDateTime leituraOriginal = LocalDateTime.of(2026, 11, 18, 10, 5);

        MensagemDestinatario dest = new MensagemDestinatario();
        dest.setMensagemId(10L);
        dest.setDestinatarioMatricula("2001");
        dest.setStatus(StatusDestinatario.LIDO);
        dest.setDataEntrega(entregaOriginal);
        dest.setDataLeitura(leituraOriginal);

        MensagemAviso aviso = new MensagemAviso();
        aviso.setId(10L);
        aviso.setPrioridade(PrioridadeMensagem.IMPORTANTE);
        aviso.setModalidade(ModalidadeEnvio.MULTICAST);
        aviso.setExigeCiencia(true);

        when(destRepo.findByMensagemIdAndDestinatarioMatricula(10L, "2001")).thenReturn(Optional.of(dest));
        when(avisoRepo.findById(10L)).thenReturn(Optional.of(aviso));
        when(anexoRepo.findByMensagemIdOrderByIdAsc(10L)).thenReturn(List.of());

        MensagemRecebidaResponse resp = service.confirmarCiencia("servidor.a", 10L);

        assertEquals(entregaOriginal, resp.dataEntrega());
        assertEquals(leituraOriginal, resp.dataLeitura());
        assertNotNull(resp.dataCiencia());
        verify(destRepo).save(dest);

        // Segunda chamada não deve alterar ciência já registrada
        LocalDateTime cienciaOriginal = dest.getDataCiencia();
        MensagemRecebidaResponse resp2 = service.confirmarCiencia("servidor.a", 10L);
        assertEquals(cienciaOriginal, resp2.dataCiencia());
        verify(destRepo, never()).saveAll(anyList());
    }
}

