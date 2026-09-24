package mppb.restapi.mobile.feature.contracheque.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.infrastructure.client.authapi.AuthServiceClient;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoRequestDto;
import mppb.restapi.common.infrastructure.client.authapi.dto.ConsultaAutorizacaoResponseDto;
import mppb.restapi.common.infrastructure.util.Constants;
import mppb.restapi.mobile.core.security.ResolvedorTokenAutenticacao;
import mppb.restapi.mobile.feature.contracheque.dto.CompetenciaDto;
import mppb.restapi.mobile.feature.contracheque.dto.ContrachequeResponseDto;
import mppb.restapi.mobile.feature.contracheque.service.ContrachequeService;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping({ "/contracheques", "/" + Constants.API_PREFIX + "/contracheques" })
@RequiredArgsConstructor
public class ContrachequeController {

    private final ContrachequeService contrachequeService;
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

    @GetMapping("/competencias")
    public ResponseEntity<List<CompetenciaDto>> getCompetencias(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "token", required = false) String tokenLegado) {
        String token = ResolvedorTokenAutenticacao.resolver(authorization, tokenLegado);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        String login = extractLoginFromToken(token);

        if (login == null) {
            return ResponseEntity.status(403).build(); // Return 403 or 401 if unauthorized
        }

        try {
            // Em produção, isso virá do RH. Por enquanto, parseamos a string ou usamos um
            // mock se for matrícula numérica
            Integer matricula = contrachequeService.getMatriculaByLogin(login);

            if (matricula == null) {
                return ResponseEntity.status(400).build();
            }

            List<CompetenciaDto> competencias = contrachequeService.listarCompetenciasDisponiveis(matricula);
            return ResponseEntity.ok(competencias);
        } catch (Exception e) {
            log.error("Erro ao buscar competencias", e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/{mes}/{ano}")
    public ResponseEntity<ContrachequeResponseDto> getContracheque(
            @PathVariable("mes") Integer mes,
            @PathVariable("ano") Integer ano,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "token", required = false) String tokenLegado) {

        String token = ResolvedorTokenAutenticacao.resolver(authorization, tokenLegado);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        String login = extractLoginFromToken(token);

        if (login == null) {
            return ResponseEntity.status(403).build();
        }

        try {
            Integer matricula = contrachequeService.getMatriculaByLogin(login);

            if (matricula == null) {
                return ResponseEntity.status(400).build();
            }

            ContrachequeResponseDto contracheque = contrachequeService.detalharContracheque(matricula, mes, ano);
            if (contracheque == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(contracheque);
        } catch (Exception e) {
            log.error("Erro ao buscar contracheque", e);
            return ResponseEntity.status(500).build();
        }
    }

    @Autowired
    private mppb.restapi.mobile.feature.contracheque.service.ContrachequePdfService pdfService;

    @GetMapping(value = "/{mes}/{ano}/pdf", produces = org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getContrachequePdf(
            @PathVariable("mes") Integer mes,
            @PathVariable("ano") Integer ano,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "token", required = false) String tokenLegado) {

        String token = ResolvedorTokenAutenticacao.resolver(authorization, tokenLegado);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        String login = extractLoginFromToken(token);

        if (login == null) {
            return ResponseEntity.status(403).build();
        }

        try {
            Integer matricula = contrachequeService.getMatriculaByLogin(login);
            if (matricula == null) {
                return ResponseEntity.status(400).build();
            }

            ContrachequeResponseDto contracheque = contrachequeService.detalharContracheque(matricula, mes, ano);
            if (contracheque == null) {
                return ResponseEntity.notFound().build();
            }

            byte[] pdfBytes = pdfService.generatePdf(contracheque);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    "contracheque_" + ano + "_" + String.format("%02d", mes) + ".pdf");

            return new ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);

        } catch (Exception e) {
            log.error("Erro ao gerar PDF do contracheque", e);
            return ResponseEntity.status(500).build();
        }
    }
}
