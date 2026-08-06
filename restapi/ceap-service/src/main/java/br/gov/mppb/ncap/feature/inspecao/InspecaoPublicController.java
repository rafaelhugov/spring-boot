package br.gov.mppb.ncap.feature.inspecao;

import br.gov.mppb.ncap.feature.inspecao.converter.InspecaoConverter;
import br.gov.mppb.ncap.feature.inspecao.converter.RespostaInspecaoConverter;
import br.gov.mppb.ncap.feature.inspecao.dto.InspecaoDto;
import br.gov.mppb.ncap.feature.inspecao.dto.RespostaInspecaoDto;
import br.gov.mppb.ncap.feature.formtemplate.FormTemplateService;
import br.gov.mppb.ncap.feature.formtemplate.converter.FormTemplateConverter;
import br.gov.mppb.ncap.feature.formtemplate.dto.FormTemplateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/public/inspecoes")
@RequiredArgsConstructor
public class InspecaoPublicController {

    private final InspecaoService service;
    private final InspecaoConverter converter;
    private final RespostaInspecaoConverter respostaConverter;
    private final FormTemplateService formTemplateService;
    private final FormTemplateConverter formTemplateConverter;


    @GetMapping(value = "/token/{token}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InspecaoDto> obterPorToken(@PathVariable String token) {
        log.info("Buscando inspeção pública por token: {}", token);
        Inspecao inspecao = service.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inspeção não encontrada para o token informado."));
        return ResponseEntity.ok(converter.toDto(inspecao));
    }

    @GetMapping(value = "/token/{token}/template", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FormTemplateDto> obterTemplatePorToken(@PathVariable String token) {
        log.info("Buscando template associado ao token público: {}", token);
        Inspecao inspecao = service.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inspeção não encontrada para o token informado."));
        
        if (inspecao.getIdFormtemplate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inspeção não possui template configurado.");
        }
        
        FormTemplateDto templateDto = formTemplateService.findById(inspecao.getIdFormtemplate())
                .map(formTemplateConverter::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template de formulário não encontrado."));
        return ResponseEntity.ok(templateDto);
    }

    @GetMapping(value = "/token/{token}/respostas", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<RespostaInspecaoDto>> obterRespostasPorToken(@PathVariable String token) {
        log.info("Buscando respostas da inspeção pública por token: {}", token);
        Inspecao inspecao = service.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inspeção não encontrada para o token informado."));
        
        List<RespostaInspecaoDto> respostas = service.obterRespostasInspecao(inspecao.getId()).stream()
                .map(respostaConverter::toDto)
                .toList();
        return ResponseEntity.ok(respostas);
    }

    @PutMapping(value = "/token/{token}/respostas", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<RespostaInspecaoDto>> salvarRespostasPorToken(
            @PathVariable String token,
            @RequestBody List<RespostaInspecaoDto> dtos) {
        log.info("Salvando respostas para token público: {}", token);
        Inspecao inspecao = service.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inspeção não encontrada para o token informado."));
        
        List<RespostaInspecao> respostas = dtos.stream()
                .map(respostaConverter::toEntity)
                .toList();
        for (RespostaInspecao r : respostas) {
            r.setInspecao(inspecao);
        }

        List<RespostaInspecaoDto> salvas = service.salvarRespostas(inspecao.getId(), respostas).stream()
                .map(respostaConverter::toDto)
                .toList();
        return ResponseEntity.ok(salvas);
    }

    @PutMapping(value = "/token/{token}/enviar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InspecaoDto> enviarParaCorregedoriaPorToken(@PathVariable String token) {
        log.info("Tramitando/Finalizando preenchimento de inspeção pública por token: {}", token);
        Inspecao inspecao = service.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inspeção não encontrada para o token informado."));

        if (inspecao.getStatus() != InspecaoStatus.AGENDADA && 
            inspecao.getStatus() != InspecaoStatus.EM_PREENCHIMENTO &&
            inspecao.getStatus() != InspecaoStatus.CORRIGIDO_DELEGACIA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A inspeção não está em estado que permita finalização.");
        }

        inspecao.setStatus(InspecaoStatus.VALIDACAO_CORREGEDORIA);
        Inspecao salva = service.save(inspecao);
        return ResponseEntity.ok(converter.toDto(salva));
    }
}
