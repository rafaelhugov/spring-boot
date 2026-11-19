package mppb.restapi.mobile.feature.mensagem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import com.fasterxml.jackson.databind.ObjectMapper;

import mppb.restapi.common.infrastructure.security.authorization.TokenAuthentication;
import mppb.restapi.mobile.feature.mensagem.dto.CriarMensagemRequest;
import mppb.restapi.mobile.feature.mensagem.dto.CriteriosDestinatariosDto;
import mppb.restapi.mobile.feature.mensagem.dto.MensagemEnviadaResponse;
import mppb.restapi.mobile.feature.mensagem.dto.MensagemRecebidaResponse;
import mppb.restapi.mobile.feature.mensagem.entity.MensagemAviso;
import mppb.restapi.mobile.feature.mensagem.entity.MensagemDestinatario;
import mppb.restapi.mobile.feature.mensagem.enums.ModalidadeEnvio;
import mppb.restapi.mobile.feature.mensagem.enums.PrioridadeMensagem;
import mppb.restapi.mobile.feature.mensagem.enums.StatusDestinatario;
import mppb.restapi.mobile.feature.mensagem.repository.GrupoMensagemMembroRepository;
import mppb.restapi.mobile.feature.mensagem.repository.MensagemAnexoRepository;
import mppb.restapi.mobile.feature.mensagem.repository.MensagemAvisoRepository;
import mppb.restapi.mobile.feature.mensagem.repository.MensagemDestinatarioRepository;
import mppb.restapi.mobile.feature.mensagem.repository.RhDestinatarioRepository;
import mppb.restapi.mobile.feature.mensagem.service.MensagemDestinatarioService;
import mppb.restapi.mobile.feature.mensagem.service.MensagemEnvioService;
import mppb.restapi.mobile.feature.mensagem.service.MensagemPushService;
import mppb.restapi.mobile.feature.perfil.repository.PerfilUsuarioRepository;
import mppb.restapi.mobile.feature.synthetic.audit.AuditoriaUsuarioSintetico;
import mppb.restapi.mobile.feature.synthetic.auth.AutenticadorUsuarioSintetico;
import mppb.restapi.mobile.feature.synthetic.auth.SyntheticTokenAuthenticationProvider;
import mppb.restapi.mobile.feature.synthetic.config.SyntheticUsersProperties;

class MensagemUatAdminTest {

    @Test
    @DisplayName("Usuários sintéticos MEMBRO (uat.membro e membro.google) recebem authority androidmppb.mensagens.admin; SERVIDOR permanece destinatário comum")
    void deveConcederAdminApenasParaUatMembro() {
        SyntheticUsersProperties props = new SyntheticUsersProperties();
        props.setEnabled(true);
        props.setJwtSecret("segredo-uat-teste-muito-longo-1234567890");
        props.setPasswords(Map.of(
                "uat-membro", "SenhaMembro123!",
                "uat-servidor", "SenhaServidor123!",
                "google-review-membro", "SenhaGoogleMembro123!",
                "google-review-servidor", "SenhaGoogleServidor123!"));

        AuditoriaUsuarioSintetico auditoria = new AuditoriaUsuarioSintetico();
        AutenticadorUsuarioSintetico autenticador = new AutenticadorUsuarioSintetico(props, auditoria);
        SyntheticTokenAuthenticationProvider provider = new SyntheticTokenAuthenticationProvider(autenticador, auditoria);

        String tokenMembro = autenticador.autenticar("uat.membro", "SenhaMembro123!").orElseThrow();
        Authentication authMembro = provider.authenticate(new TokenAuthentication(tokenMembro));
        assertTrue(authMembro.getAuthorities().stream()
                .anyMatch(a -> "androidmppb.mensagens.admin".equals(a.getAuthority())));

        String tokenGoogleMembro = autenticador.autenticar("membro.google", "SenhaGoogleMembro123!").orElseThrow();
        Authentication authGoogleMembro = provider.authenticate(new TokenAuthentication(tokenGoogleMembro));
        assertTrue(authGoogleMembro.getAuthorities().stream()
                .anyMatch(a -> "androidmppb.mensagens.admin".equals(a.getAuthority())));

        String tokenServidor = autenticador.autenticar("uat.servidor", "SenhaServidor123!").orElseThrow();
        Authentication authServidor = provider.authenticate(new TokenAuthentication(tokenServidor));
        assertFalse(authServidor.getAuthorities().stream()
                .anyMatch(a -> "androidmppb.mensagens.admin".equals(a.getAuthority())));

        String tokenGoogleServidor = autenticador.autenticar("servidor.google", "SenhaGoogleServidor123!").orElseThrow();
        Authentication authGoogleServidor = provider.authenticate(new TokenAuthentication(tokenGoogleServidor));
        assertFalse(authGoogleServidor.getAuthorities().stream()
                .anyMatch(a -> "androidmppb.mensagens.admin".equals(a.getAuthority())));
    }

    @Test
    @DisplayName("uat.membro envia aviso Unicast para uat.servidor (900002) e uat.servidor lê sem consultar autorizacao.usuario")
    void devePermitirCicloCompletoEntreUatMembroEUatServidor() {
        MensagemAvisoRepository avisoRepo = mock(MensagemAvisoRepository.class);
        MensagemDestinatarioRepository destRepo = mock(MensagemDestinatarioRepository.class);
        MensagemAnexoRepository anexoRepo = mock(MensagemAnexoRepository.class);
        GrupoMensagemMembroRepository grupoMembroRepo = mock(GrupoMensagemMembroRepository.class);
        RhDestinatarioRepository rhRepo = mock(RhDestinatarioRepository.class);
        PerfilUsuarioRepository perfilRepo = mock(PerfilUsuarioRepository.class);
        MensagemPushService pushService = mock(MensagemPushService.class);

        when(rhRepo.listarPorCriterios(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(avisoRepo.save(any(MensagemAviso.class))).thenAnswer(inv -> {
            MensagemAviso a = inv.getArgument(0);
            a.setId(501L);
            return a;
        });
        when(destRepo.countByMensagemId(501L)).thenReturn(1L);

        MensagemEnvioService envioService = new MensagemEnvioService(
                avisoRepo, destRepo, anexoRepo, grupoMembroRepo, rhRepo, perfilRepo, pushService, new ObjectMapper());

        CriarMensagemRequest req = new CriarMensagemRequest(
                "Teste UAT Simulador",
                "Mensagem enviada pelo uat.membro para uat.servidor.",
                PrioridadeMensagem.IMPORTANTE,
                ModalidadeEnvio.UNICAST,
                true,
                new CriteriosDestinatariosDto(null, null, null, null, null, List.of("900002")),
                List.of(),
                null,
                null);

        MensagemEnviadaResponse enviada = envioService.enviarOuAgendar("uat.membro", req);
        assertEquals(501L, enviada.id());
        assertEquals(1L, enviada.totalDestinatarios());
        verify(destRepo).saveAll(anyList());
        verifyNoInteractions(perfilRepo);

        // Agora uat.servidor (900002) abre e confirma ciência
        MensagemDestinatario registroServidor = new MensagemDestinatario();
        registroServidor.setMensagemId(501L);
        registroServidor.setDestinatarioMatricula("900002");
        registroServidor.setDestinatarioNome("Mariana Costa Ribeiro");
        registroServidor.setStatus(StatusDestinatario.PENDENTE);

        MensagemAviso avisoSalvo = new MensagemAviso();
        avisoSalvo.setId(501L);
        avisoSalvo.setTitulo("Teste UAT Simulador");
        avisoSalvo.setPrioridade(PrioridadeMensagem.IMPORTANTE);
        avisoSalvo.setModalidade(ModalidadeEnvio.UNICAST);
        avisoSalvo.setExigeCiencia(true);
        when(destRepo.findByMensagemIdAndDestinatarioMatricula(501L, "900002"))
                .thenReturn(Optional.of(registroServidor));
        when(avisoRepo.findById(501L)).thenReturn(Optional.of(avisoSalvo));
        when(anexoRepo.findByMensagemIdOrderByIdAsc(501L)).thenReturn(List.of());

        MensagemDestinatarioService destService = new MensagemDestinatarioService(
                destRepo, avisoRepo, anexoRepo, perfilRepo);

        MensagemRecebidaResponse lida = destService.abrirMensagem("uat.servidor", 501L);
        assertEquals(StatusDestinatario.LIDO, lida.status());
        verifyNoInteractions(perfilRepo);
    }
}
