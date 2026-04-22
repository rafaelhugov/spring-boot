package mppb.restapi.cursos.feature.manual.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mppb.restapi.common.infrastructure.client.authapi.AuthServiceClient;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoRequestDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoResponseDto;
import mppb.restapi.common.infrastructure.util.Constants;
import mppb.restapi.cursos.feature.manual.service.ManualPdfService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Constants.API_PREFIX + "/manuais")
@CrossOrigin(origins = "*", exposedHeaders = {"Content-Disposition", "Content-Type"})
@RequiredArgsConstructor
@Tag(name = "Manuais", description = "Endpoints para download dos manuais do sistema em PDF")
public class ManualController {

    private final ManualPdfService service;
    private final AuthServiceClient authServiceClient;

    @Value("${sistema.sigla:MPVIRTUAL}")
    private String siglaSistema;

    @Operation(summary = "Download Manual Solicitante", description = "Gera e faz download do manual para proponentes de cursos (Público)")
    @GetMapping(value = {"/pdf/solicitante", "/pdf/manual_solicitante.pdf"}, produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getManualSolicitante() throws Exception {
        byte[] pdf = service.gerarPdfManual("solicitante");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"manual_solicitante.pdf\"")  
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .body(pdf);
    }

    @Operation(summary = "Download Manual Administrativo", description = "Gera e faz download do manual completo para administradores (Restrito)")
    @GetMapping(value = {"/pdf/administrativo", "/pdf/manual_administrativo.pdf"}, produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getManualAdministrativo(@RequestParam(value = "token", required = false) String queryToken) throws Exception {
        if (!isAuthorized(queryToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        byte[] pdf = service.gerarPdfManual("administrativo");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"manual_administrativo.pdf\"")  
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .body(pdf);
    }

    private boolean isAuthorized(String queryToken) {
        // 1. Tentar via contexto de segurança padrão (Filter já processou se houver Header)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("cursos.manter.cursos"));
        }

        // 2. Se não houver auth no contexto, mas houver token na query param, validar manualmente
        if (queryToken != null && !queryToken.trim().isEmpty()) {
            try {
                ConsultaAutorizacaoResponseDto resp = authServiceClient.consultaAutorizacao(new ConsultaAutorizacaoRequestDto(queryToken, siglaSistema));
                if (resp != null && resp.getAcessoData() != null && resp.getAcessoData().getFuncionalidadesPermitidas() != null) {
                    return resp.getAcessoData().getFuncionalidadesPermitidas().stream()
                            .anyMatch(f -> "cursos.manter.cursos".equals(f.getServico()));
                }
            } catch (Exception e) {
                return false;
            }
        }

        return false;
    }
}
