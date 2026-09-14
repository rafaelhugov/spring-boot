package mppb.restapi.cursos.feature.common.service;

import lombok.RequiredArgsConstructor;
import mppb.restapi.cursos.feature.common.entities.Arquivo;
import mppb.restapi.cursos.feature.common.repository.ArquivoRepository;
import mppb.restapi.common.infrastructure.security.util.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Optional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ArquivoService {

    private final ArquivoRepository arquivoRepository;

    @Transactional
    public Long upload(MultipartFile file) throws IOException {
        ArquivoSecurityUtils.ValidatedUpload upload = ArquivoSecurityUtils.validateUpload(file);

        Arquivo arquivo = new Arquivo();
        arquivo.setNomeArquivo(upload.safeName());
        arquivo.setMimeType(upload.mimeType());
        arquivo.setTamanho(upload.size());
        arquivo.setBytes(upload.bytes());

        Arquivo salvo = arquivoRepository.save(arquivo);
        return salvo.getId();
    }

    @Transactional(readOnly = true)
    public Optional<Arquivo> buscarPorId(Long id) {
        return arquivoRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Arquivo buscarArquivoPrivadoAutorizado(Long id) {
        Arquivo arquivo = arquivoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo não encontrado."));
        int referenciasConhecidas = countKnownResourceReferences(id);
        if (referenciasConhecidas == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Arquivo sem vínculo autorizado.");
        }
        if (referenciasConhecidas > 1) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Arquivo com vínculo ambíguo.");
        }
        validarAcessoPrivado(id);
        return arquivo;
    }

    @Transactional(readOnly = true)
    public Arquivo buscarArquivoPublicoAutorizado(Long id) {
        Arquivo arquivo = arquivoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo não encontrado."));
        if (!arquivoRepository.existsPublicHeaderImageReference(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Arquivo privado.");
        }
        return arquivo;
    }

    @Transactional
    public void remover(Long id) {
        arquivoRepository.deleteById(id);
    }

    private int countKnownResourceReferences(Long id) {
        int references = 0;
        boolean solicitacaoMinistrante = arquivoRepository.existsSolicitacaoMinistranteReference(id);
        references += arquivoRepository.existsCursoHeaderImageReference(id) ? 1 : 0;
        references += arquivoRepository.existsCursoCertificateTemplateReference(id) ? 1 : 0;
        references += arquivoRepository.existsArquivoComprobatorioReference(id) ? 1 : 0;
        references += !solicitacaoMinistrante && arquivoRepository.existsMinistranteReference(id) ? 1 : 0;
        references += solicitacaoMinistrante ? 1 : 0;
        references += arquivoRepository.existsSolicitacaoProgramacaoReference(id) ? 1 : 0;
        return references;
    }

    private void validarAcessoPrivado(Long id) {
        if (usuarioAtualEhAdministradorCursos()) {
            return;
        }

        Long servidorId = SecurityUtils.getAuthenticationToken()
                .map(auth -> auth.getUsuario() != null ? auth.getUsuario().getId() : null)
                .orElse(null);

        if (servidorId != null
                && (arquivoRepository.existsSolicitacaoProgramacaoReferenceForServidor(id, servidorId)
                || arquivoRepository.existsSolicitacaoMinistranteReferenceForServidor(id, servidorId))) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Arquivo sem autorização para o usuário autenticado.");
    }

    private boolean usuarioAtualEhAdministradorCursos() {
        return SecurityUtils.getAuthenticationToken()
                .map(auth -> auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(authority -> authority.equals("cursos.analisar.solicitacoes")
                                || authority.equals("cursos.manter.cursos")))
                .orElse(false);
    }
}
