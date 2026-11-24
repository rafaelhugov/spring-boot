package mppb.restapi.mobile.feature.mensagem.controller;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mppb.restapi.mobile.core.security.AutenticacaoAtual;
import mppb.restapi.mobile.feature.carteirafuncional.service.CarteiraService;
import mppb.restapi.mobile.feature.mensagem.dto.CaixaEntradaResponse;
import mppb.restapi.mobile.feature.mensagem.dto.ConfirmarEntregaRequest;
import mppb.restapi.mobile.feature.mensagem.dto.MensagemRecebidaResponse;
import mppb.restapi.mobile.feature.mensagem.entity.MensagemAnexo;
import mppb.restapi.mobile.feature.mensagem.service.MensagemDestinatarioService;
import mppb.restapi.mobile.feature.mensagem.service.MensagemStorageProperties;
import mppb.restapi.mobile.feature.mensagem.service.MensagemStorageService;

@RestController
@RequestMapping("/api/mensagens")
@RequiredArgsConstructor
public class MensagemDestinatarioController {

    private static final long CHUNK_SIZE_PADRAO = 1024L * 1024L;

    private final MensagemDestinatarioService destinatarioService;
    private final MensagemStorageService storageService;
    private final MensagemStorageProperties storageProperties;
    private final CarteiraService carteiraService;

    @GetMapping("/remetentes/{matricula}/foto")
    public ResponseEntity<byte[]> fotoRemetente(@PathVariable String matricula) {
        loginObrigatorio();
        byte[] bytes = carteiraService.getBytesFotoPorMatricula(matricula);
        if (bytes == null || bytes.length == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(bytes);
    }

    @GetMapping("/permissao-admin")
    public ResponseEntity<java.util.Map<String, Boolean>> permissaoAdmin() {
        loginObrigatorio();
        return ResponseEntity.ok(java.util.Map.of("podeEnviar", usuarioEhAdmin()));
    }

    @GetMapping("/recebidas")
    public ResponseEntity<CaixaEntradaResponse> listarRecebidas(
            @RequestParam(defaultValue = "TODAS") String filtro,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return ResponseEntity.ok(destinatarioService.listarCaixaEntrada(loginObrigatorio(), filtro, pagina, tamanho));
    }

    @PostMapping("/recebidas/confirmar-entrega")
    public ResponseEntity<Void> confirmarEntrega(@Valid @RequestBody ConfirmarEntregaRequest request) {
        destinatarioService.confirmarEntregaEmLote(loginObrigatorio(), request.mensagemIds());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/recebidas/{id}")
    public ResponseEntity<MensagemRecebidaResponse> abrir(@PathVariable Long id) {
        return ResponseEntity.ok(destinatarioService.abrirMensagem(loginObrigatorio(), id));
    }

    @PostMapping("/recebidas/{id}/ciencia")
    public ResponseEntity<MensagemRecebidaResponse> confirmarCiencia(@PathVariable Long id) {
        return ResponseEntity.ok(destinatarioService.confirmarCiencia(loginObrigatorio(), id));
    }

    @PatchMapping("/recebidas/{id}/arquivar")
    public ResponseEntity<Void> arquivar(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean arquivada) {
        destinatarioService.arquivar(loginObrigatorio(), id, arquivada);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{mensagemId}/anexos/{anexoId}/conteudo")
    public ResponseEntity<ResourceRegion> conteudoAnexo(
            @PathVariable Long mensagemId,
            @PathVariable Long anexoId,
            @RequestHeader HttpHeaders headers) throws IOException {

        MensagemAnexo anexo = destinatarioService.buscarAnexoAutorizado(
                loginObrigatorio(), usuarioEhAdmin(), mensagemId, anexoId);
        Resource resource = storageService.carregarRecurso(anexo);
        long tamanhoTotal = resource.contentLength();
        ResourceRegion region = resolverRegiao(resource, tamanhoTotal, headers.getRange());

        HttpStatus status = headers.getRange().isEmpty() ? HttpStatus.OK : HttpStatus.PARTIAL_CONTENT;
        return ResponseEntity.status(status)
                .contentType(MediaType.parseMediaType(anexo.getMimeType()))
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(anexo.getNomeOriginal())
                        .build()
                        .toString())
                .body(region);
    }

    @GetMapping("/{mensagemId}/anexos/{anexoId}/thumbnail")
    public ResponseEntity<Resource> thumbnailAnexo(
            @PathVariable Long mensagemId,
            @PathVariable Long anexoId) {

        MensagemAnexo anexo = destinatarioService.buscarAnexoAutorizado(
                loginObrigatorio(), usuarioEhAdmin(), mensagemId, anexoId);
        boolean semThumbnailBytes = anexo.getThumbnailBytes() == null || anexo.getThumbnailBytes().length == 0;
        boolean semThumbnailDisco = anexo.getCaminhoThumbnail() == null || anexo.getCaminhoThumbnail().isBlank();
        if (semThumbnailBytes && semThumbnailDisco) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = storageService.carregarThumbnail(anexo);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }

    private ResourceRegion resolverRegiao(Resource resource, long tamanhoTotal, List<HttpRange> ranges) {
        if (ranges == null || ranges.isEmpty()) {
            return new ResourceRegion(resource, 0, tamanhoTotal);
        }
        HttpRange range = ranges.get(0);
        long start = range.getRangeStart(tamanhoTotal);
        long end = range.getRangeEnd(tamanhoTotal);
        long rangeLength = Math.min(CHUNK_SIZE_PADRAO, end - start + 1);
        return new ResourceRegion(resource, start, rangeLength);
    }

    private String loginObrigatorio() {
        return AutenticacaoAtual.login()
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não identificado."));
    }

    private boolean usuarioEhAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        String required = storageProperties.getAuthorityAdmin();
        return auth.getAuthorities().stream()
                .anyMatch(a -> required.equals(a.getAuthority()));
    }
}

