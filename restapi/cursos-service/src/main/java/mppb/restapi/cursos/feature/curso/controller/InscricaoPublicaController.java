package mppb.restapi.cursos.feature.curso.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.controller.AbstractController;
import mppb.restapi.cursos.feature.curso.converter.CursoConverter;
import mppb.restapi.cursos.feature.curso.dto.CursoDto;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.curso.service.CursoService;
import mppb.restapi.cursos.infrastructure.util.Constants;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Validated
@RestController
@RequestMapping(Constants.API_PREFIX + "/public/inscricao")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Inscrição", description = "Gerencia as operações relacionadas à inscrição")
//TODO remover classe
public class InscricaoPublicaController extends AbstractController {

    private final CursoService cursoService;

    private final CursoConverter cursoConverter;

//    @GetMapping("/{hash}")
//    public ResponseEntity<CursoDto> getCursoParaInscricao(@PathVariable UUID hash) {
//        return cursoService.findByHashInscricao(hash)
//                .map(cursoConverter::toDto)
//                .map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
//    }

//    @PostMapping("/{hash}")
//    public ResponseEntity<String> realizarInscricao(@PathVariable UUID hash, @RequestBody Map<String, String> participante) {
//        return cursoService.findByHashInscricao(hash).map(curso -> {
//            if (!cursoService.isPeriodoInscricaoValido(curso)) {
//                return ResponseEntity.badRequest().body("Período de inscrições encerrado.");
//            }
//            if (!cursoService.isPublicoAlvoValido(participante.get("email"), curso)) {
//                return ResponseEntity.badRequest().body("Você não pertence ao público-alvo deste curso.");
//            }
//            System.out.println("Inscrição recebida para o curso " + curso.getTitulo() + ": " + participante);
//            return ResponseEntity.ok("Inscrição realizada com sucesso!");
//        }).orElse(ResponseEntity.notFound().build());
//    }

}
