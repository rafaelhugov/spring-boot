package mppb.restapi.cursos.feature.curso.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mppb.restapi.common.core.controller.AbstractController;
import mppb.restapi.common.infrastructure.util.Constants;
import mppb.restapi.cursos.feature.common.service.ArquivoService;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.curso.repository.CursoRepository;
import mppb.restapi.cursos.feature.curso.service.CertificadoService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(Constants.API_PREFIX + "/cursos")
@RequiredArgsConstructor
@Tag(name = "Certificados", description = "Endpoints para geração e gestão de certificados")
public class CertificadoController extends AbstractController {

    private final CertificadoService certificadoService;
    private final ArquivoService arquivoService;
    private final CursoRepository cursoRepository;

    @Operation(summary = "Upload da arte de fundo do certificado")
    @PostMapping("/{id}/certificado-template")
    @PreAuthorize("hasAuthority('cursos.manter.cursos')")
    @Transactional
    public ResponseEntity<Void> uploadTemplate(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws Exception {
        Curso curso = cursoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Curso não encontrado"));

        Long arquivoId = arquivoService.upload(file);
        curso.setCertificateTemplateId(arquivoId);
        cursoRepository.save(curso);

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Gera o PDF do certificado para um participante")
    @GetMapping("/{cursoId}/participantes/{participanteId}/certificado/pdf")
    @PreAuthorize("hasAuthority('cursos.manter.cursos')")
    public ResponseEntity<byte[]> downloadCertificado(@PathVariable Long cursoId, @PathVariable Long participanteId) throws Exception {
        byte[] pdf = certificadoService.gerarCertificadoPdf(cursoId, participanteId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "certificado.pdf");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }

    @Operation(summary = "Gera o PDF do folheto de inscrição com QR Code")
    @GetMapping("/{cursoId}/qrcode-inscricao")
    @PreAuthorize("hasAuthority('cursos.manter.cursos')")
    public ResponseEntity<byte[]> downloadFlyerInscricao(@PathVariable Long cursoId, @RequestParam String baseUrl) throws Exception {
        byte[] pdf = certificadoService.gerarFlyerInscricaoPdf(cursoId, baseUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "ficha_inscricao.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }
}
