package mppb.restapi.cursos.feature.inscricao.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mppb.restapi.cursos.feature.inscricao.dto.InscricaoDto;
import mppb.restapi.cursos.feature.inscricao.service.InscricaoService;
import mppb.restapi.cursos.infrastructure.util.Constants;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Validated
@RestController
@RequestMapping(Constants.API_PREFIX + "/inscricoes")
@RequiredArgsConstructor
@Tag(name = "Inscrições", description = "Endpoints autenticados para a inscrição do próprio utilizador")
//TODO mover os metodos para o InscricaoController
public class InscricaoAutenticadaController {

    private final InscricaoService inscricaoService;

    /**
     * Endpoint seguro para o utilizador interno (servidor/membro) confirmar a sua
     * própria inscrição num curso.
     *
     * @param hash O hash do curso.
     * @return O DTO da inscrição criada.
     */
    @Operation(summary = "Realiza a inscrição do utilizador interno logado",
            description = "Este endpoint requer a permissão 'cursos.acesso' e inscreve o utilizador autenticado no curso especificado pelo hash.",
            security = @SecurityRequirement(name = "bearerAuth")) // Assume que a sua segurança OpenAPI chama-se 'bearerAuth'
    @PostMapping("/{hash}")
    @PreAuthorize("hasAuthority('cursos.acesso')") // Protegido pela permissão base
    public ResponseEntity<InscricaoDto> inscreverInterno(@PathVariable UUID hash) {
        InscricaoDto inscricaoSalva = inscricaoService.realizarInscricaoInterna(hash);
        return new ResponseEntity<>(inscricaoSalva, HttpStatus.CREATED);
    }
}
