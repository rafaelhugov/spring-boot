package br.gov.mppb.ceaf_backend.controller;

import br.gov.mppb.ceaf_backend.domain.Curso;
import br.gov.mppb.ceaf_backend.repository.CursoRepository;
import br.gov.mppb.ceaf_backend.service.InscricaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/public/inscricao")
public class InscricaoPublicaController {

    private final CursoRepository cursoRepository;
    private final InscricaoService inscricaoService;

    public InscricaoPublicaController(CursoRepository cursoRepository, InscricaoService inscricaoService) {
        this.cursoRepository = cursoRepository;
        this.inscricaoService = inscricaoService;
    }

    @GetMapping("/{hash}")
    public ResponseEntity<Curso> getCursoParaInscricao(@PathVariable UUID hash) { // Tipo do parâmetro alterado
        return cursoRepository.findByHashInscricao(hash)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{hash}")
    public ResponseEntity<String> realizarInscricao(@PathVariable UUID hash, @RequestBody Map<String, String> participante) { // Tipo do parâmetro alterado
        return cursoRepository.findByHashInscricao(hash).map(curso -> {
            if (!inscricaoService.isPeriodoInscricaoValido(curso)) {
                return ResponseEntity.badRequest().body("Período de inscrições encerrado.");
            }
            if (!inscricaoService.isPublicoAlvoValido(participante.get("email"), curso)) {
                return ResponseEntity.badRequest().body("Você não pertence ao público-alvo deste curso.");
            }
            System.out.println("Inscrição recebida para o curso " + curso.getTitulo() + ": " + participante);
            return ResponseEntity.ok("Inscrição realizada com sucesso!");
        }).orElse(ResponseEntity.notFound().build());
    }
}