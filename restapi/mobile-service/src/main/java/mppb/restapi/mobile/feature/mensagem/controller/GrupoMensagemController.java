package mppb.restapi.mobile.feature.mensagem.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mppb.restapi.mobile.core.security.AutenticacaoAtual;
import mppb.restapi.mobile.feature.mensagem.dto.CatalogoGruposRhResponse;
import mppb.restapi.mobile.feature.mensagem.dto.DestinatarioRhResponse;
import mppb.restapi.mobile.feature.mensagem.dto.GrupoPersonalizadoRequest;
import mppb.restapi.mobile.feature.mensagem.dto.GrupoPersonalizadoResponse;
import mppb.restapi.mobile.feature.mensagem.service.GrupoMensagemService;

@RestController
@RequestMapping("/api/mensagens/admin")
@PreAuthorize("hasAuthority(@mensagemStorageProperties.authorityAdmin)")
@RequiredArgsConstructor
public class GrupoMensagemController {

    private final GrupoMensagemService grupoMensagemService;

    @GetMapping("/grupos/predefinidos")
    public ResponseEntity<CatalogoGruposRhResponse> listarPredefinidos() {
        return ResponseEntity.ok(grupoMensagemService.catalogoRh());
    }

    @GetMapping("/grupos/personalizados")
    public ResponseEntity<List<GrupoPersonalizadoResponse>> listarPersonalizados() {
        return ResponseEntity.ok(grupoMensagemService.listarPersonalizados());
    }

    @PostMapping("/grupos/personalizados")
    public ResponseEntity<GrupoPersonalizadoResponse> criarPersonalizado(
            @Valid @RequestBody GrupoPersonalizadoRequest request) {
        String login = AutenticacaoAtual.login()
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não identificado."));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(grupoMensagemService.criarPersonalizado(login, request));
    }

    @PutMapping("/grupos/personalizados/{id}")
    public ResponseEntity<GrupoPersonalizadoResponse> atualizarPersonalizado(
            @PathVariable Long id,
            @Valid @RequestBody GrupoPersonalizadoRequest request) {
        return ResponseEntity.ok(grupoMensagemService.atualizarPersonalizado(id, request));
    }

    @DeleteMapping("/grupos/personalizados/{id}")
    public ResponseEntity<Void> removerPersonalizado(@PathVariable Long id) {
        grupoMensagemService.removerPersonalizado(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/destinatarios/buscar")
    public ResponseEntity<List<DestinatarioRhResponse>> buscarDestinatarios(
            @RequestParam(name = "q", defaultValue = "") String q,
            @RequestParam(defaultValue = "20") int limite) {
        return ResponseEntity.ok(grupoMensagemService.buscarDestinatarios(q, limite));
    }
}
