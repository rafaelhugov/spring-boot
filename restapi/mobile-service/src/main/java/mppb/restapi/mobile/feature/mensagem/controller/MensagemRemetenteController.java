package mppb.restapi.mobile.feature.mensagem.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mppb.restapi.mobile.core.security.AutenticacaoAtual;
import mppb.restapi.mobile.feature.mensagem.dto.AcompanhamentoMensagemResponse;
import mppb.restapi.mobile.feature.mensagem.dto.CriarMensagemRequest;
import mppb.restapi.mobile.feature.mensagem.dto.MensagemAnexoResponse;
import mppb.restapi.mobile.feature.mensagem.dto.MensagemEnviadaResponse;
import mppb.restapi.mobile.feature.mensagem.dto.MensagensEnviadasPageResponse;
import mppb.restapi.mobile.feature.mensagem.dto.PreviaAlcanceRequest;
import mppb.restapi.mobile.feature.mensagem.dto.PreviaAlcanceResponse;
import mppb.restapi.mobile.feature.mensagem.entity.MensagemAnexo;
import mppb.restapi.mobile.feature.mensagem.enums.StatusDestinatario;
import mppb.restapi.mobile.feature.mensagem.repository.MensagemAnexoRepository;
import mppb.restapi.mobile.feature.mensagem.service.MensagemEnvioService;
import mppb.restapi.mobile.feature.mensagem.service.MensagemStorageService;
import mppb.restapi.mobile.feature.mensagem.service.MensagemStorageService.ArquivoArmazenado;

@RestController
@RequestMapping("/api/mensagens/admin")
@PreAuthorize("hasAuthority(@mensagemStorageProperties.authorityAdmin)")
@RequiredArgsConstructor
public class MensagemRemetenteController {

    private final MensagemEnvioService mensagemEnvioService;
    private final MensagemStorageService mensagemStorageService;
    private final MensagemAnexoRepository mensagemAnexoRepository;

    @PostMapping("/previa-alcance")
    public ResponseEntity<PreviaAlcanceResponse> previaAlcance(@RequestBody PreviaAlcanceRequest request) {
        return ResponseEntity.ok(mensagemEnvioService.previaAlcance(request));
    }

    @PostMapping(value = "/anexos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MensagemAnexoResponse> uploadAnexo(
            @RequestPart("arquivo") MultipartFile arquivo,
            @RequestParam(value = "duracaoSegundos", required = false) Integer duracaoSegundos) {

        ArquivoArmazenado armazenado = mensagemStorageService.armazenar(arquivo);
        MensagemAnexo anexo = new MensagemAnexo();
        anexo.setCategoria(armazenado.categoria());
        anexo.setNomeOriginal(armazenado.nomeOriginal());
        anexo.setMimeType(armazenado.mimeType());
        anexo.setTamanhoBytes(armazenado.tamanhoBytes());
        anexo.setDuracaoSegundos(duracaoSegundos);
        anexo.setCaminhoStorage(armazenado.caminhoStorage());
        anexo = mensagemAnexoRepository.save(anexo);
        return ResponseEntity.status(HttpStatus.CREATED).body(MensagemAnexoResponse.from(anexo));
    }

    @PostMapping
    public ResponseEntity<MensagemEnviadaResponse> enviar(@Valid @RequestBody CriarMensagemRequest request) {
        String login = AutenticacaoAtual.login()
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não identificado."));
        return ResponseEntity.status(HttpStatus.CREATED).body(mensagemEnvioService.enviarOuAgendar(login, request));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        String login = AutenticacaoAtual.login()
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não identificado."));
        mensagemEnvioService.cancelarAgendada(id, login);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/arquivar")
    public ResponseEntity<Void> arquivar(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean arquivada) {
        String login = AutenticacaoAtual.login()
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não identificado."));
        mensagemEnvioService.arquivarRemetente(id, arquivada, login);
        return ResponseEntity.noContent().build();
    }
}

