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

            if (response.getPagamentos().isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erro ao buscar diarias para exercicio {}", exercicio, e);
            return ResponseEntity.status(500).build();
        }
    }
}
