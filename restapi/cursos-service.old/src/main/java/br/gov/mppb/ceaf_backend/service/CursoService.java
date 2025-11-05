package br.gov.mppb.ceaf_backend.service;

import br.gov.mppb.ceaf_backend.domain.Curso;
import br.gov.mppb.ceaf_backend.domain.Ministrante;
import br.gov.mppb.ceaf_backend.repository.CursoRepository;
import br.gov.mppb.ceaf_backend.repository.MinistranteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class CursoService {

    private final CursoRepository cursoRepository;
    private final MinistranteRepository ministranteRepository;
    private final ServidorService servidorService; // Injetar o serviço de servidor

    public CursoService(CursoRepository cursoRepository, MinistranteRepository ministranteRepository, ServidorService servidorService) {
        this.cursoRepository = cursoRepository;
        this.ministranteRepository = ministranteRepository;
        this.servidorService = servidorService;
    }


    @Transactional(readOnly = true)
    public Optional<Curso> findCompletoById(Long id) {
        Optional<Curso> cursoOpt = cursoRepository.findById(id);

        cursoOpt.ifPresent(curso -> {
            curso.getMinistrantes().forEach(ministrante -> {
                if (ministrante.getIdServidor() != null) {
                    ministrante.setTipo("interno");
                    servidorService.buscarServidorPorId(ministrante.getIdServidor())
                            .ifPresent(servidorDTO -> {
                                ministrante.setNome(servidorDTO.getNome());
                                ministrante.setCargo(servidorDTO.getTipoCargoNome()); // Preenche o cargo
                            });
                } else {
                    ministrante.setTipo("externo");
                }
            });
        });

        return cursoOpt;
    }
    public Curso salvar(Curso curso) {
        if (curso.isGerarLink() && curso.getHashInscricao() == null) {
            curso.setHashInscricao(UUID.randomUUID());
        } else if (!curso.isGerarLink()) {
            curso.setHashInscricao(null);
        }

        Set<Ministrante> ministrantesGerenciados = new HashSet<>();
        if (curso.getMinistrantes() != null) {
            for (Ministrante min : curso.getMinistrantes()) {
                if ("interno".equals(min.getTipo())) {
                    min.setNome(null);
                    min.setNomeOrgaoExterno(null);
                } else { // externo
                    min.setIdServidor(null);
                }
                ministrantesGerenciados.add(ministranteRepository.save(min));
            }
        }
        curso.setMinistrantes(ministrantesGerenciados);

        return cursoRepository.save(curso);
    }
}