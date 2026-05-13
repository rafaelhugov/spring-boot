package mppb.restapi.eleicao.feature.foto.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.eleicao.feature.candidato.entity.Candidato;
import mppb.restapi.eleicao.feature.candidato.repository.CandidatoRepository;
import mppb.restapi.eleicao.feature.foto.service.FotoService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/fotos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Fotos", description = "Recuperação de fotos de candidatos")
public class FotoController {

    private final FotoService fotoService;
    private final CandidatoRepository candidatoRepository;
    private final JdbcTemplate jdbcTemplate;

    @Operation(summary = "Busca foto do candidato", description = "Retorna a imagem do candidato pelo seu ID.")
    @Transactional(readOnly = true)
    @GetMapping(value = "/candidato/{id}", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_GIF_VALUE})
    public ResponseEntity<byte[]> getFotoCandidato(@PathVariable Long id) throws IOException {
        log.debug("Buscando foto para o candidato ID: {}", id);
        return candidatoRepository.findById(id)
                .map(candidato -> {
                    byte[] foto = null;
                    String mimetype = "image/png";

                    if (candidato.getIdFoto() != null) {
                        log.debug("Tentando buscar foto específica (ID Arquivo: {})", candidato.getIdFoto());
                        foto = fotoService.getFotoPorArquivoId(candidato.getIdFoto());
                        if (foto != null) mimetype = fotoService.getMimetype(candidato.getIdFoto());
                    }

                    if (foto == null && candidato.getIdServidor() != null) {
                        log.debug("Tentando buscar foto do servidor (ID Servidor: {})", candidato.getIdServidor());
                        Long pessoaId = getPessoaIdPorServidorId(candidato.getIdServidor());
                        if (pessoaId != null) {
                            foto = fotoService.getFotoPorPessoaId(pessoaId);
                        }
                    }

                    if (foto != null) {
                        log.debug("Foto encontrada. Retornando {} bytes", foto.length);
                        return ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType(mimetype))
                                .body(foto);
                    }

                    log.warn("Nenhuma foto encontrada para o candidato {}. Retornando imagem padrão.", id);
                    return returnDefaultImage();
                })
                .orElseGet(() -> {
                    log.error("Candidato ID {} não encontrado no banco.", id);
                    return returnDefaultImage();
                });
    }

    @Operation(summary = "Busca foto por ID de arquivo", description = "Retorna a imagem diretamente pelo ID do arquivo no GED.")
    @Transactional(readOnly = true)
    @GetMapping(value = "/arquivo/{id}", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_GIF_VALUE})
    public ResponseEntity<byte[]> getFotoPorArquivo(@PathVariable Long id) {
        byte[] foto = fotoService.getFotoPorArquivoId(id);
        if (foto != null) {
            String mimetype = fotoService.getMimetype(id);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mimetype))
                    .body(foto);
        }
        return returnDefaultImage();
    }

    private Long getPessoaIdPorServidorId(Long servidorId) {
        String sql = "select id_pessoa from publico.servidor where id = ?";
        try {
            log.debug("Executando query em publico.servidor para id: {}", servidorId);
            return jdbcTemplate.queryForObject(sql, Long.class, servidorId);
        } catch (Exception e) {
            log.error("Erro ao buscar id_pessoa para servidor {}: {}", servidorId, e.getMessage());
            return null;
        }
    }

    private ResponseEntity<byte[]> returnDefaultImage() {
        try {
            byte[] defaultImage = new ClassPathResource("semfoto.gif").getInputStream().readAllBytes();
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_GIF)
                    .body(defaultImage);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
