package mppb.restapi.mobile.feature.pagamentos.controller;

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
import mppb.restapi.mobile.feature.pagamentos.dto.PagamentoItemDto;
import mppb.restapi.mobile.feature.pagamentos.dto.PagamentosResponseDto;
import mppb.restapi.mobile.feature.pagamentos.service.PagamentosService;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping({ "/pagamentos", "/" + Constants.API_PREFIX + "/pagamentos" })
@RequiredArgsConstructor
public class PagamentosController {

    private final PagamentosService pagamentosService;
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

    @GetMapping("/exercicios")
    public ResponseEntity<java.util.List<Integer>> getExerciciosDisponiveis(
            @RequestParam("token") @NotNull String token) {

        String login = extractLoginFromToken(token);

        if (login == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            String cpf = pagamentosService.getCpfByLogin(login);

            if (cpf == null || cpf.isBlank()) {
                return ResponseEntity.status(400).build();
            }

            cpf = cpf.replaceAll("\\D", "");

            java.util.List<Integer> response = pagamentosService.listarAnosDisponiveis(cpf);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erro ao buscar exercicios disponiveis", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * GET /pagamentos/{exercicio}?token=XXX
     * Returns all pagamentos for the authenticated user in the given exercício year.
     */
    @GetMapping("/{exercicio}")
    public ResponseEntity<PagamentosResponseDto> getPagamentos(
            @PathVariable("exercicio") String exercicio,
            @RequestParam("token") @NotNull String token) {

        String login = extractLoginFromToken(token);

        if (login == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            String cpf = pagamentosService.getCpfByLogin(login);

            if (cpf == null || cpf.isBlank()) {
                return ResponseEntity.status(400).build();
            }

            // Remove formatação para realizar a busca no banco apenas com números
            cpf = cpf.replaceAll("\\D", "");

            PagamentosResponseDto response = pagamentosService.listarPagamentos(exercicio, cpf);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erro ao buscar pagamentos para exercicio {}", exercicio, e);
            return ResponseEntity.status(500).build();
        }
    }
}
