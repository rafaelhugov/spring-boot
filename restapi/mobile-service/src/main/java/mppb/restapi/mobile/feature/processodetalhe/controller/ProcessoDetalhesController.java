package mppb.restapi.mobile.feature.processodetalhe.controller;

import mppb.restapi.mobile.feature.processodetalhe.dto.ProcessoDetalheDTO;
import mppb.restapi.mobile.feature.processodetalhe.dto.CaptchaProcessoRequest;
import mppb.restapi.mobile.feature.processodetalhe.service.ProcessoDetalhesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/v1/processos")
public class ProcessoDetalhesController {

    private static final Logger log = LoggerFactory.getLogger(ProcessoDetalhesController.class);

    @Autowired
    private ProcessoDetalhesService service;

    @GetMapping("/{idProcesso}/detalhes")
    @Deprecated(forRemoval = true)
    public ResponseEntity<?> obterDetalhesProcesso(
            @PathVariable Long idProcesso,
            @RequestParam("captchaToken") String captchaToken,
            @RequestParam("captchaValue") String captchaValue) {
        
        return carregarDetalhes(idProcesso, captchaToken, captchaValue);
    }

    @PostMapping("/{idProcesso}/detalhes")
    public ResponseEntity<?> obterDetalhesProcessoSeguro(
            @PathVariable Long idProcesso,
            @RequestBody CaptchaProcessoRequest captcha) {
        if (captcha == null) {
            return ResponseEntity.badRequest().body("CAPTCHA não informado.");
        }
        return carregarDetalhes(idProcesso, captcha.captchaToken(), captcha.captchaValue());
    }

    private ResponseEntity<?> carregarDetalhes(Long idProcesso, String captchaToken, String captchaValue) {
        try {
            ProcessoDetalheDTO detalhes = service.obterDetalhesProcesso(idProcesso, captchaToken, captchaValue);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(detalhes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao buscar os detalhes do processo {}", idProcesso, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao buscar os detalhes do processo.");
        }
    }
}
