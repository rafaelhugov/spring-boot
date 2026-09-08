package mppb.restapi.mobile.feature.concessaofolga.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import mppb.restapi.common.infrastructure.client.authapi.AuthServiceClient;
import mppb.restapi.common.infrastructure.client.authapi.dto.AutorizaRequestDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.AutorizaResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoRequestDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoResponseDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.UsuarioAutorizadoResponseDto;
import mppb.restapi.mobile.feature.concessaofolga.dto.AusenciaCalendarioDto;
import mppb.restapi.mobile.feature.concessaofolga.dto.ConcessaoFolgaDto;
import mppb.restapi.mobile.feature.concessaofolga.dto.OperacaoSolicitacaoRequestDto;
import mppb.restapi.mobile.feature.concessaofolga.dto.SolicitacaoMarcacaoFeriasRequestDto;
import mppb.restapi.mobile.feature.concessaofolga.service.ConcessaoFolgaService;

class ConcessaoFolgaControllerTest {

    private FakeConcessaoFolgaService concessaoFolgaService;
    private FakeAuthServiceClient authServiceClient;
    private ConcessaoFolgaController controller;

    @BeforeEach
    void setUp() {
        concessaoFolgaService = new FakeConcessaoFolgaService();
        authServiceClient = new FakeAuthServiceClient();
        controller = new ConcessaoFolgaController(concessaoFolgaService, authServiceClient);
    }

    @Test
    void marcacaoDeFeriasUsaTokenNoModoPadrao() {
        SolicitacaoMarcacaoFeriasRequestDto request = new SolicitacaoMarcacaoFeriasRequestDto();
        request.setDataInicial("14/09/2026");
        request.setNumeroDias(15);

        ResponseEntity<String> response = controller.cadastrarSolicitacao("token-auth", 422364L, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("token-auth", concessaoFolgaService.mpVirtualCredentialRecebida);
        assertEquals("renata.luz", authServiceClient.loginConsultado);
    }

    @Test
    void marcacaoDeFeriasUsaLoginNoModoHomologacao() {
        ReflectionTestUtils.setField(controller, "mpVirtualAuthMode", "login");
        SolicitacaoMarcacaoFeriasRequestDto request = new SolicitacaoMarcacaoFeriasRequestDto();
        request.setDataInicial("14/09/2026");
        request.setNumeroDias(15);

        ResponseEntity<String> response = controller.cadastrarSolicitacao("token-auth", 422364L, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("renata.luz", concessaoFolgaService.mpVirtualCredentialRecebida);
    }

    @Test
    void operacoesDaSolicitacaoTambemUsamLoginNoModoHomologacao() {
        ReflectionTestUtils.setField(controller, "mpVirtualAuthMode", "login");

        ResponseEntity<String> response = controller.consultarOperacoesSolicitacao("token-auth", 514825L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("renata.luz", concessaoFolgaService.mpVirtualCredentialRecebida);
    }

    private static class FakeAuthServiceClient implements AuthServiceClient {

        private String loginConsultado = "renata.luz";

        @Override
        public AutorizaResponseDto autoriza(AutorizaRequestDto autorizaRequestTO) {
            return null;
        }

        @Override
        public ConsultaAutorizacaoResponseDto consultaAutorizacao(
                ConsultaAutorizacaoRequestDto consultaAutorizacaoRequestTO) {
            return new ConsultaAutorizacaoResponseDto(
                    null,
                    new UsuarioAutorizadoResponseDto(6L, "Renata Luz", loginConsultado));
        }
    }

    private static class FakeConcessaoFolgaService extends ConcessaoFolgaService {

        private String mpVirtualCredentialRecebida;

        FakeConcessaoFolgaService() {
            super(null, null, null);
        }

        @Override
        public String getMatriculaByLogin(String login) {
            return "12345";
        }

        @Override
        public List<ConcessaoFolgaDto> listarConcessoes(String matricula) {
            return List.of();
        }

        @Override
        public List<AusenciaCalendarioDto> listarAusencias(String matricula, Integer mes, Integer ano) {
            return List.of();
        }

        @Override
        public ResponseEntity<String> sugestoesDatasFinais(Long concessaoId, String dataInicial, String token) {
            mpVirtualCredentialRecebida = token;
            return ResponseEntity.ok("[]");
        }

        @Override
        public ResponseEntity<String> cadastrarSolicitacao(
                Long concessaoId,
                SolicitacaoMarcacaoFeriasRequestDto request,
                String token) {
            mpVirtualCredentialRecebida = token;
            return ResponseEntity.ok("{}");
        }

        @Override
        public ResponseEntity<String> consultarSolicitacao(Long solicitacaoId, String token) {
            mpVirtualCredentialRecebida = token;
            return ResponseEntity.ok("{}");
        }

        @Override
        public ResponseEntity<String> consultarOperacoesSolicitacao(Long solicitacaoId, String token) {
            mpVirtualCredentialRecebida = token;
            return ResponseEntity.ok("[]");
        }

        @Override
        public ResponseEntity<String> cancelarSolicitacao(Long solicitacaoId, String token) {
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
