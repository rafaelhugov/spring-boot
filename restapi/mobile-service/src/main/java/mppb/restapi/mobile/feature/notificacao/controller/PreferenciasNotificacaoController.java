package mppb.restapi.mobile.feature.notificacao.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mppb.restapi.common.infrastructure.util.Constants;
import mppb.restapi.mobile.core.security.AutenticacaoAtual;
import mppb.restapi.mobile.feature.designacao.service.DesignacaoAuthService;
import mppb.restapi.mobile.feature.notificacao.dto.PreferenciasNotificacaoRequest;
import mppb.restapi.mobile.feature.notificacao.model.PreferenciasNotificacao;
import mppb.restapi.mobile.feature.notificacao.service.PreferenciasNotificacaoService;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(Constants.API_PREFIX + "/notificacoes/preferencias")
@RequiredArgsConstructor
public class PreferenciasNotificacaoController {

    private final DesignacaoAuthService authService;
    private final PreferenciasNotificacaoService service;

    @GetMapping
    public ResponseEntity<PreferenciasNotificacao> buscar() {
        String matricula = matriculaAutenticada();
        return matricula == null
                ? ResponseEntity.status(401).build()
                : ResponseEntity.ok(service.buscar(matricula));
    }

    @PutMapping
    public ResponseEntity<PreferenciasNotificacao> salvar(
            @Valid @RequestBody PreferenciasNotificacaoRequest request) {
        String matricula = matriculaAutenticada();
        if (matricula == null) {
            return ResponseEntity.status(401).build();
        }
        PreferenciasNotificacao preferencias = new PreferenciasNotificacao(
                obrigatorio(request.pushNoticias()), obrigatorio(request.pushDesignacoes()),
                obrigatorio(request.pushContracheques()), obrigatorio(request.pushPagamentos()),
                obrigatorio(request.pushAfastamentos()), obrigatorio(request.centralNoticias()),
                obrigatorio(request.centralDesignacoes()), obrigatorio(request.centralContracheques()),
                obrigatorio(request.centralPagamentos()), obrigatorio(request.centralAfastamentos()));
        return ResponseEntity.ok(service.salvar(matricula, preferencias));
    }

    private String matriculaAutenticada() {
        return AutenticacaoAtual.login()
                .map(authService::matriculaPorLogin)
                .filter(matricula -> !matricula.isBlank())
                .orElse(null);
    }

    private static boolean obrigatorio(Boolean valor) {
        if (valor == null) {
            throw new IllegalArgumentException("Todas as preferências de notificação são obrigatórias");
        }
        return valor;
    }
}
