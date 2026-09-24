package mppb.restapi.mobile.feature.designacao.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.infrastructure.util.Constants;
import mppb.restapi.mobile.core.security.AutenticacaoAtual;
import mppb.restapi.mobile.feature.designacao.dto.DesignacoesAtivasResponse;
import mppb.restapi.mobile.feature.designacao.service.DesignacaoAuthService;
import mppb.restapi.mobile.feature.designacao.service.DesignacaoConsultaService;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(Constants.API_PREFIX + "/designacoes")
@RequiredArgsConstructor
public class DesignacaoController {

    private final DesignacaoAuthService authService;
    private final DesignacaoConsultaService consultaService;

    @GetMapping("/ativas")
    public ResponseEntity<DesignacoesAtivasResponse> ativas() {
        String matricula = AutenticacaoAtual.login()
                .map(authService::matriculaPorLogin)
                .orElse(null);
        if (matricula == null) {
            log.warn("Consulta de designacoes negada: usuario nao autenticado ou sem matricula");
            return ResponseEntity.status(401).build();
        }

        try {
            return ResponseEntity.ok(consultaService.listarAtivas(matricula));
        } catch (Exception e) {
            log.error("Erro ao consultar designacoes ativas", e);
            return ResponseEntity.status(500).build();
        }
    }
}
