package mppb.restapi.cursos.feature.curso.service;

import lombok.RequiredArgsConstructor;
import mppb.restapi.common.core.error.BusinessRuleException;
import mppb.restapi.common.core.error.NotFoundException;
import mppb.restapi.cursos.feature.common.entities.Arquivo;
import mppb.restapi.cursos.feature.common.service.ArquivoService;
import mppb.restapi.cursos.feature.curso.dto.CursoArquivoComprobatorioDto;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.curso.entity.CursoArquivoComprobatorio;
import mppb.restapi.cursos.feature.curso.enums.StatusCursoEnum;
import mppb.restapi.cursos.feature.curso.repository.CursoArquivoComprobatorioRepository;
import mppb.restapi.cursos.feature.curso.repository.CursoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CursoArquivoComprobatorioService {

    private final CursoRepository cursoRepository;
    private final CursoArquivoComprobatorioRepository repository;
    private final ArquivoService arquivoService;

    @Transactional(readOnly = true)
    public List<CursoArquivoComprobatorioDto> listar(Long cursoId) {
        if (!cursoRepository.existsById(cursoId)) {
            throw new NotFoundException("Curso não encontrado: " + cursoId);
        }
        return repository.findAllByCursoIdWithArquivo(cursoId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CursoArquivoComprobatorioDto upload(Long cursoId, MultipartFile file) throws IOException {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new NotFoundException("Curso não encontrado: " + cursoId));
        validarCursoFinalizado(curso);
        validarArquivo(file);

        Long arquivoId = arquivoService.upload(file);
        Arquivo arquivo = arquivoService.buscarPorId(arquivoId)
                .orElseThrow(() -> new NotFoundException("Arquivo não encontrado após upload: " + arquivoId));

        CursoArquivoComprobatorio vinculo = new CursoArquivoComprobatorio();
        vinculo.setCurso(curso);
        vinculo.setArquivo(arquivo);
        return toDto(repository.save(vinculo));
    }

    @Transactional
    public void remover(Long cursoId, Long comprovanteId) {
        CursoArquivoComprobatorio vinculo = repository.findByIdAndCursoId(comprovanteId, cursoId)
                .orElseThrow(() -> new NotFoundException("Arquivo comprobatório não encontrado para este curso."));
        validarCursoFinalizado(vinculo.getCurso());
        Long arquivoId = vinculo.getArquivo().getId();
        repository.delete(vinculo);
        arquivoService.remover(arquivoId);
    }

    private void validarCursoFinalizado(Curso curso) {
        boolean finalizadoPorStatus = curso.getStatus() == StatusCursoEnum.FINALIZADO;
        boolean finalizadoPorData = curso.getRealizacaoDataFinal() != null
                && curso.getRealizacaoDataFinal().isBefore(LocalDateTime.now());
        if (!finalizadoPorStatus && !finalizadoPorData) {
            throw new BusinessRuleException("Arquivos comprobatórios só podem ser gerenciados em cursos finalizados.");
        }
    }

    private void validarArquivo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Selecione um arquivo para envio.");
        }
        String mimeType = file.getContentType() != null ? file.getContentType().toLowerCase(Locale.ROOT) : "";
        if (!"application/pdf".equals(mimeType) && !mimeType.startsWith("image/")) {
            throw new BusinessRuleException("Envie apenas arquivos PDF ou imagens.");
        }
    }

    private CursoArquivoComprobatorioDto toDto(CursoArquivoComprobatorio entity) {
        Arquivo arquivo = entity.getArquivo();
        return CursoArquivoComprobatorioDto.builder()
                .id(entity.getId())
                .arquivoId(arquivo.getId())
                .nomeArquivo(arquivo.getNomeArquivo())
                .mimeType(arquivo.getMimeType())
                .tamanho(arquivo.getTamanho())
                .dataRegistro(entity.getDataRegistro())
                .build();
    }
}
