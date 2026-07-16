package mppb.restapi.cursos.feature.common.service;

import lombok.RequiredArgsConstructor;
import mppb.restapi.cursos.feature.common.entities.Arquivo;
import mppb.restapi.cursos.feature.common.repository.ArquivoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ArquivoService {

    private final ArquivoRepository arquivoRepository;

    @Transactional
    public Long upload(MultipartFile file) throws IOException {
        Arquivo arquivo = new Arquivo();
        arquivo.setNomeArquivo(file.getOriginalFilename());
        arquivo.setMimeType(file.getContentType());
        arquivo.setTamanho(file.getSize());
        arquivo.setBytes(file.getBytes());

        Arquivo salvo = arquivoRepository.save(arquivo);
        return salvo.getId();
    }

    @Transactional(readOnly = true)
    public Optional<Arquivo> buscarPorId(Long id) {
        return arquivoRepository.findById(id);
    }

    @Transactional
    public void remover(Long id) {
        arquivoRepository.deleteById(id);
    }
}
