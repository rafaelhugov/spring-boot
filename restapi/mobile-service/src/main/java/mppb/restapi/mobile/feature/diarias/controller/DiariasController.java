package mppb.restapi.mobile.feature.diarias.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.infrastructure.client.authapi.AuthServiceClient;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoRequestDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoResponseDto;
import mppb.restapi.common.infrastructure.util.Constants;
import mppb.restapi.mobile.feature.diarias.dto.DiariasResponseDto;
import mppb.restapi.mobile.feature.diarias.service.DiariasService;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping({ "/diarias", "/" + Constants.API_PREFIX + "/diarias" })
@RequiredArgsConstructor
public class DiariasController {

    private final DiariasService diariasService;
    private final AuthServiceClient authServiceClient;

    private String extractLoginFromToken(String token) {
        ConsultaAutorizacaoRequestDto req = new ConsultaAutorizacaoRequestDto(token, "ANDROID");
        ConsultaAutorizacaoResponseDto autorizacao = null;
        try {
            autorizacao = authServiceClient.consultaAutorizacao(req);
        } catch (Exception e) {
            log.error("Erro ao consultar autorizacao no auth-service", e);
        }

        if (autorizacao == null || autorizacao.getUsuario() == null || autorizacao.getUsuario().getLogin() == null) {
            return null;
        }

        return autorizacao.getUsuario().getLogin();
    }

    /**
     * GET /diarias/{exercicio}?token=XXX
     * Returns all diárias payments for the authenticated user in the given exercício year.
     */
    @GetMapping("/{exercicio}")
    public ResponseEntity<DiariasResponseDto> getDiarias(
            @PathVariable("exercicio") String exercicio,
            @RequestParam("token") @NotNull String token) {

        String login = extractLoginFromToken(token);

        if (login == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            String cpf = diariasService.getCpfByLogin(login);

            if (cpf == null || cpf.isBlank()) {
                return ResponseEntity.status(400).build();
            }

            DiariasResponseDto response = diariasService.listarDiarias(exercicio, cpf.trim());

            if (response.getPagamentos() == null || response.getPagamentos().isEmpty()) {
                // Mock data para testes quando o usuário não tiver diárias
                DiariasResponseDto mockResponse = new DiariasResponseDto();
                mockResponse.setNuExercicio(exercicio);
                mockResponse.setTotalBruto(new java.math.BigDecimal("1700.00"));
                mockResponse.setTotalLiquido(new java.math.BigDecimal("1500.00"));
                mockResponse.setTotalPagamentos(2);
                
                java.util.List<mppb.restapi.mobile.feature.diarias.dto.DiariaPagamentoDto> mocks = new java.util.ArrayList<>();
                
                mppb.restapi.mobile.feature.diarias.dto.DiariaPagamentoDto p1 = new mppb.restapi.mobile.feature.diarias.dto.DiariaPagamentoDto();
                p1.setCdOrgao("01");
                p1.setSigla("MPPB");
                p1.setNuExercicio(exercicio);
                p1.setDocumento("2026OB00001");
                p1.setDtPagamento(exercicio + "-01-15 14:00:00.000");
                p1.setVlBruto(new java.math.BigDecimal("850.00"));
                p1.setVlLiquido(new java.math.BigDecimal("750.00"));
                p1.setDsModalidadePag("CREDITO EM CONTA");
                p1.setNuProcesso("001.2026.000001");
                p1.setCdElemento("14");
                p1.setDescricaoElemento("DIARIAS - CIVIL");
                mocks.add(p1);
                
                mppb.restapi.mobile.feature.diarias.dto.DiariaPagamentoDto p2 = new mppb.restapi.mobile.feature.diarias.dto.DiariaPagamentoDto();
                p2.setCdOrgao("01");
                p2.setSigla("MPPB");
                p2.setNuExercicio(exercicio);
                p2.setDocumento("2026OB00002");
                p2.setDtPagamento(exercicio + "-02-20 10:30:00.000");
                p2.setVlBruto(new java.math.BigDecimal("850.00"));
                p2.setVlLiquido(new java.math.BigDecimal("750.00"));
                p2.setDsModalidadePag("CREDITO EM CONTA");
                p2.setNuProcesso("001.2026.000002");
                p2.setCdElemento("14");
                p2.setDescricaoElemento("DIARIAS - CIVIL");
                mocks.add(p2);
                
                mockResponse.setPagamentos(mocks);
                
                return ResponseEntity.ok(mockResponse);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erro ao buscar diarias para exercicio {}", exercicio, e);
            return ResponseEntity.status(500).build();
        }
    }
}
