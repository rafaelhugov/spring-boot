package mppb.restapi.mobile.feature.concessaofolga.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpServerErrorException;

import mppb.restapi.common.infrastructure.client.authapi.dto.UsuarioAutorizadoResponseDto;
import mppb.restapi.common.infrastructure.security.authorization.TokenAuthentication;
import mppb.restapi.mobile.feature.concessaofolga.dto.AusenciaCalendarioDto;
import mppb.restapi.mobile.feature.concessaofolga.dto.ConcessaoFolgaDto;
import mppb.restapi.mobile.feature.concessaofolga.dto.OperacaoSolicitacaoRequestDto;
import mppb.restapi.mobile.feature.concessaofolga.dto.SolicitacaoMarcacaoFeriasRequestDto;
import mppb.restapi.mobile.feature.concessaofolga.service.ConcessaoFolgaService;

class ConcessaoFolgaControllerTest {

    private FakeConcessaoFolgaService concessaoFolgaService;
    private ConcessaoFolgaController controller;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        concessaoFolgaService = new FakeConcessaoFolgaService();
        controller = new ConcessaoFolgaController(concessaoFolgaService);
        autenticar("token-auth", "renata.luz");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void marcacaoDeFeriasUsaTokenNoModoPadrao() {
        SolicitacaoMarcacaoFeriasRequestDto request = new SolicitacaoMarcacaoFeriasRequestDto();
        request.setDataInicial("14/09/2026");
        request.setNumeroDias(15);

        ResponseEntity<String> response = controller.cadastrarSolicitacao(422364L, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("token-auth", concessaoFolgaService.mpVirtualCredentialRecebida);
        assertEquals("renata.luz", concessaoFolgaService.loginConsultado);
    }

    @Test
    void marcacaoDeFeriasUsaLoginNoModoHomologacao() {
        ReflectionTestUtils.setField(controller, "mpVirtualAuthMode", "login");
        SolicitacaoMarcacaoFeriasRequestDto request = new SolicitacaoMarcacaoFeriasRequestDto();
        request.setDataInicial("14/09/2026");
        request.setNumeroDias(15);

        ResponseEntity<String> response = controller.cadastrarSolicitacao(422364L, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("renata.luz", concessaoFolgaService.mpVirtualCredentialRecebida);
    }

    @Test
    void operacoesDaSolicitacaoTambemUsamLoginNoModoHomologacao() {
        ReflectionTestUtils.setField(controller, "mpVirtualAuthMode", "login");

        ResponseEntity<String> response = controller.consultarOperacoesSolicitacao(514825L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("renata.luz", concessaoFolgaService.mpVirtualCredentialRecebida);
    }

    @Test
    void cadastrarSolicitacaoTrataErroDeIntervaloMinimoComo422() {
        String mensagemErroPg = "ERROR: Não é possível inserir mais uma etapa em uma concessão em menos de 1 minuto desde a última.\n"
                + "Onde: PL/pgSQL function recursoshumanos.fragmentofla_prevent_fntg() line 21 at RAISE";
        concessaoFolgaService.erroSimulado = HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                HttpHeaders.EMPTY,
                mensagemErroPg.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        SolicitacaoMarcacaoFeriasRequestDto request = new SolicitacaoMarcacaoFeriasRequestDto();
        request.setDataInicial("14/09/2026");
        request.setNumeroDias(15);

        ResponseEntity<String> response = controller.cadastrarSolicitacao(422364L, request);

        assertEquals(422, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Não é possível inserir mais uma etapa em uma concessão em menos de 1 minuto"));
        assertTrue(response.getBody().contains("Por favor, aguarde 1 minuto"));
    }

    @Test
    void proxyMpVirtualTrataErroDeIntervaloMinimoComo422() {
        String mensagemErroPg = "ERROR: Não é possível inserir mais uma etapa em uma concessão em menos de 1 minuto desde a última.";
        concessaoFolgaService.erroSimulado = HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                HttpHeaders.EMPTY,
                mensagemErroPg.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        ResponseEntity<String> response = controller.cancelarSolicitacao(514825L);

        assertEquals(422, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Não é possível inserir mais uma etapa em uma concessão em menos de 1 minuto"));
    }

    @Test
    void proxyMpVirtualSanitizaHtmlDeErroRetornandoBadGateway() {
        String htmlTomcat = "<html><head><title>500 Internal Server Error</title></head><body><h1>HTTP Status 500</h1></body></html>";
        concessaoFolgaService.erroSimulado = HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                HttpHeaders.EMPTY,
                htmlTomcat.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        ResponseEntity<String> response = controller.cancelarSolicitacao(514825L);

        assertEquals(502, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Falha no serviço de Recursos Humanos ao cancelar solicitacao."));
    }

    private void autenticar(String token, String login) {
        SecurityContextHolder.getContext().setAuthentication(new TokenAuthentication(
                token,
                new UsuarioAutorizadoResponseDto(6L, "Renata Luz", login),
                null,
                null,
                AuthorityUtils.createAuthorityList("ROLE_USER")));
    }

    private static class FakeConcessaoFolgaService extends ConcessaoFolgaService {

        private String mpVirtualCredentialRecebida;
        private String loginConsultado;
        private RuntimeException erroSimulado;

        FakeConcessaoFolgaService() {
            super(null, null, null);
        }

        private void checarErroSimulado() {
            if (erroSimulado != null) {
                throw erroSimulado;
            }
        }

        @Override
        public String getMatriculaByLogin(String login) {
            loginConsultado = login;
            return "12345";
        }

        @Override
        public List<ConcessaoFolgaDto> listarConcessoes(String matricula) {
            checarErroSimulado();
            return List.of();
        }

        @Override
        public List<AusenciaCalendarioDto> listarAusencias(String matricula, Integer mes, Integer ano) {
            checarErroSimulado();
            return List.of();
        }

        @Override
        public ResponseEntity<String> sugestoesDatasFinais(Long concessaoId, String dataInicial, String token) {
            checarErroSimulado();
            mpVirtualCredentialRecebida = token;
            return ResponseEntity.ok("[]");
        }

        @Override
        public ResponseEntity<String> cadastrarSolicitacao(
                Long concessaoId,
                SolicitacaoMarcacaoFeriasRequestDto request,
                String token) {
            checarErroSimulado();
            mpVirtualCredentialRecebida = token;
            return ResponseEntity.ok("{}");
        }

        @Override
        public ResponseEntity<String> consultarSolicitacao(Long solicitacaoId, String token) {
            checarErroSimulado();
            mpVirtualCredentialRecebida = token;
            return ResponseEntity.ok("{}");
        }

        @Override
        public ResponseEntity<String> consultarOperacoesSolicitacao(Long solicitacaoId, String token) {
            checarErroSimulado();
            mpVirtualCredentialRecebida = token;
            return ResponseEntity.ok("[]");
        }

        @Override
        public ResponseEntity<String> cancelarSolicitacao(Long solicitacaoId, String token) {
            checarErroSimulado();
            mpVirtualCredentialRecebida = token;
            return ResponseEntity.ok("{}");
        }

        @Override
        public ResponseEntity<String> adiarSolicitacao(
                Long solicitacaoId,
                OperacaoSolicitacaoRequestDto request,
                String token) {
            mpVirtualCredentialRecebida = token;
            return ResponseEntity.ok("{}");
        }

        @Override
        public ResponseEntity<String> anteciparSolicitacao(
                Long solicitacaoId,
                OperacaoSolicitacaoRequestDto request,
                String token) {
            mpVirtualCredentialRecebida = token;
            return ResponseEntity.ok("{}");
        }

        @Override
        public ResponseEntity<String> interromperSolicitacao(
                Long solicitacaoId,
                OperacaoSolicitacaoRequestDto request,
                String token) {
            mpVirtualCredentialRecebida = token;
            return ResponseEntity.ok("{}");
        }

        @Override
        public ResponseEntity<String> suspenderIntegralmenteSolicitacao(Long solicitacaoId, String token) {
            mpVirtualCredentialRecebida = token;
            return ResponseEntity.ok("{}");
        }

        @Override
        public ResponseEntity<String> suspenderParcialmenteSolicitacao(
                Long solicitacaoId,
                OperacaoSolicitacaoRequestDto request,
                String token) {
            mpVirtualCredentialRecebida = token;
            return ResponseEntity.ok("{}");
        }
    }
}
