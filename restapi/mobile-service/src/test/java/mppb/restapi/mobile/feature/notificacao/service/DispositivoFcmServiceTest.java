package mppb.restapi.mobile.feature.notificacao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import mppb.restapi.mobile.feature.notificacao.dto.DispositivoNotificacaoResponse;
import mppb.restapi.mobile.feature.notificacao.dto.RegistrarDispositivoRequest;
import mppb.restapi.mobile.feature.notificacao.model.DispositivoFcm;
import mppb.restapi.mobile.feature.notificacao.repository.DispositivoFcmRepository;

@ExtendWith(OutputCaptureExtension.class)
class DispositivoFcmServiceTest {

    private static final String TOKEN = "fcm-token-sensivel-123";

    private FakeDispositivoFcmRepository repository;
    private DispositivoFcmService service;

    @BeforeEach
    void setUp() {
        repository = new FakeDispositivoFcmRepository();
        service = new DispositivoFcmService(repository);
    }

    @Test
    void registraPrimeiroDispositivoSemRetornarToken(CapturedOutput output) {
        DispositivoNotificacaoResponse response = service.registrar(
                "12345",
                new RegistrarDispositivoRequest(null, TOKEN, "ANDROID"));

        assertNotNull(response.identificador());
        assertEquals("ANDROID", response.plataforma());
        assertTrue(response.ativo());
        assertEquals(1, repository.rows.size());
        assertFalse(response.toString().contains(TOKEN));
        assertFalse(output.getOut().contains(TOKEN));
        assertFalse(output.getErr().contains(TOKEN));
    }

    @Test
    void registroDoMesmoTokenEIdempotente() {
        DispositivoNotificacaoResponse primeiro = service.registrar(
                "12345",
                new RegistrarDispositivoRequest(null, TOKEN, "android"));
        DispositivoNotificacaoResponse segundo = service.registrar(
                "12345",
                new RegistrarDispositivoRequest(null, TOKEN, "ANDROID"));

        assertEquals(primeiro.identificador(), segundo.identificador());
        assertEquals(1, repository.rows.size());
        assertEquals("12345", repository.rows.values().iterator().next().servidorMatricula());
    }

    @Test
    void permiteMultiplosDispositivosDoMesmoUsuario() {
        DispositivoNotificacaoResponse primeiro = service.registrar(
                "12345",
                new RegistrarDispositivoRequest(null, "token-1", "ANDROID"));
        DispositivoNotificacaoResponse segundo = service.registrar(
                "12345",
                new RegistrarDispositivoRequest(null, "token-2", "IOS"));

        assertNotEquals(primeiro.identificador(), segundo.identificador());
        assertEquals(2, repository.rows.size());
    }

    @Test
    void tokenJaExistenteAtualizaMatriculaEReativa() {
        DispositivoNotificacaoResponse primeiro = service.registrar(
                "11111",
                new RegistrarDispositivoRequest(null, TOKEN, "ANDROID"));
        repository.desativar("11111", primeiro.identificador());

        DispositivoNotificacaoResponse segundo = service.registrar(
                "22222",
                new RegistrarDispositivoRequest(null, TOKEN, "IOS"));

        assertEquals(primeiro.identificador(), segundo.identificador());
        assertEquals("IOS", segundo.plataforma());
        assertTrue(segundo.ativo());
        assertEquals("22222", repository.rows.values().iterator().next().servidorMatricula());
        assertEquals(1, repository.rows.size());
    }

    @Test
    void atualizaTokenQuandoIdentificadorPertenceAoUsuario() {
        DispositivoNotificacaoResponse primeiro = service.registrar(
                "12345",
                new RegistrarDispositivoRequest(null, "token-antigo", "ANDROID"));

        DispositivoNotificacaoResponse segundo = service.registrar(
                "12345",
                new RegistrarDispositivoRequest(primeiro.identificador(), "token-novo", "ANDROID"));

        assertEquals(primeiro.identificador(), segundo.identificador());
        assertEquals(1, repository.rows.size());
        assertEquals("token-novo", repository.rows.values().iterator().next().tokenFcm());
    }

    @Test
    void desativaDispositivoDoProprioUsuario() {
        DispositivoNotificacaoResponse response = service.registrar(
                "12345",
                new RegistrarDispositivoRequest(null, TOKEN, "ANDROID"));

        assertTrue(service.desativar("12345", response.identificador()));

        assertFalse(repository.rows.values().iterator().next().ativo());
    }

    @Test
    void naoDesativaDispositivoDeOutroUsuario() {
        DispositivoNotificacaoResponse response = service.registrar(
                "12345",
                new RegistrarDispositivoRequest(null, TOKEN, "ANDROID"));

        assertFalse(service.desativar("99999", response.identificador()));

        assertTrue(repository.rows.values().iterator().next().ativo());
    }

    @Test
    void rejeitaPlataformaInvalida() {
        RegistrarDispositivoRequest request = new RegistrarDispositivoRequest(null, TOKEN, "WEB");

        assertThrows(IllegalArgumentException.class, () -> service.registrar("12345", request));
    }

    @Test
    void rejeitaTokenAusenteOuVazio() {
        assertThrows(IllegalArgumentException.class,
                () -> service.registrar("12345", new RegistrarDispositivoRequest(null, null, "ANDROID")));
        assertThrows(IllegalArgumentException.class,
                () -> service.registrar("12345", new RegistrarDispositivoRequest(null, "   ", "ANDROID")));
    }

    @Test
    void identificadorDeOutroUsuarioNaoPermiteSobrescreverDispositivo() {
        DispositivoNotificacaoResponse outro = service.registrar(
                "11111",
                new RegistrarDispositivoRequest(null, "token-outro", "ANDROID"));

        DispositivoNotificacaoResponse novo = service.registrar(
                "22222",
                new RegistrarDispositivoRequest(outro.identificador(), "token-novo", "IOS"));

        assertNotEquals(outro.identificador(), novo.identificador());
        assertEquals(2, repository.rows.size());
        assertEquals("11111", repository.rows.get(outro.identificador()).servidorMatricula());
    }

    private static class FakeDispositivoFcmRepository extends DispositivoFcmRepository {

        private final Map<String, DispositivoFcm> rows = new LinkedHashMap<>();
        private long nextId = 1L;

        FakeDispositivoFcmRepository() {
            super(null);
        }

        @Override
        public Optional<DispositivoFcm> findByIdentificador(String identificador) {
            return Optional.ofNullable(rows.get(identificador));
        }

        @Override
        public Optional<DispositivoFcm> findByTokenHash(String tokenFcmHash) {
            return rows.values().stream()
                    .filter(row -> tokenFcmHash.equals(row.tokenFcmHash()))
                    .findFirst();
        }

        @Override
        public DispositivoFcm inserir(
                String identificador,
                String matricula,
                String tokenFcm,
                String tokenFcmHash,
                String plataforma) {
            DispositivoFcm dispositivo = new DispositivoFcm(
                    nextId++,
                    identificador,
                    matricula,
                    tokenFcm,
                    tokenFcmHash,
                    plataforma,
                    true,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    LocalDateTime.now());
            rows.put(identificador, dispositivo);
            return dispositivo;
        }

        @Override
        public DispositivoFcm atualizar(
                Long id,
                String matricula,
                String tokenFcm,
                String tokenFcmHash,
                String plataforma) {
            DispositivoFcm atual = rows.values().stream()
                    .filter(row -> id.equals(row.id()))
                    .findFirst()
                    .orElseThrow();
            DispositivoFcm atualizado = new DispositivoFcm(
                    atual.id(),
                    atual.identificador(),
                    matricula,
                    tokenFcm,
                    tokenFcmHash,
                    plataforma,
                    true,
                    atual.dataCriacao(),
                    LocalDateTime.now(),
                    LocalDateTime.now());
            rows.put(atualizado.identificador(), atualizado);
            return atualizado;
        }

        @Override
        public boolean desativar(String matricula, String identificador) {
            DispositivoFcm atual = rows.get(identificador);
            if (atual == null || !matricula.equals(atual.servidorMatricula()) || !atual.ativo()) {
                return false;
            }
            rows.put(identificador, new DispositivoFcm(
                    atual.id(),
                    atual.identificador(),
                    atual.servidorMatricula(),
                    atual.tokenFcm(),
                    atual.tokenFcmHash(),
                    atual.plataforma(),
                    false,
                    atual.dataCriacao(),
                    LocalDateTime.now(),
                    atual.dataUltimoUso()));
            return true;
        }
    }
}
