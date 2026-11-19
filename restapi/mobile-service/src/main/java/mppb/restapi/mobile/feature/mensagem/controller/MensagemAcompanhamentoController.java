package mppb.restapi.mobile.feature.mensagem.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import mppb.restapi.mobile.feature.mensagem.dto.AcompanhamentoMensagemResponse;
import mppb.restapi.mobile.feature.mensagem.dto.MensagensEnviadasPageResponse;
import mppb.restapi.mobile.feature.mensagem.enums.StatusDestinatario;
import mppb.restapi.mobile.feature.mensagem.service.MensagemAcompanhamentoService;

@RestController
@RequestMapping("/api/mensagens/admin")
@PreAuthorize("hasAuthority(@mensagemStorageProperties.authorityAdmin)")
@RequiredArgsConstructor
public class MensagemAcompanhamentoController {

    private final MensagemAcompanhamentoService mensagemAcompanhamentoService;

    @GetMapping("/enviadas")
    public ResponseEntity<MensagensEnviadasPageResponse> listarEnviadas(
            @RequestParam(defaultValue = "false") boolean arquivada,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return ResponseEntity.ok(mensagemAcompanhamentoService.listarEnviadas(arquivada, pagina, tamanho));
    }

    @GetMapping("/{id}/acompanhamento")
    public ResponseEntity<AcompanhamentoMensagemResponse> detalhar(
            @PathVariable Long id,
            @RequestParam(required = false) StatusDestinatario status,
            @RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return ResponseEntity.ok(mensagemAcompanhamentoService.detalhar(id, status, busca, pagina, tamanho));
    }
}
