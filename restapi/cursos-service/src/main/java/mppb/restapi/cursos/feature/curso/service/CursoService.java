package mppb.restapi.cursos.feature.curso.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import mppb.restapi.common.core.service.AbstractService;
import mppb.restapi.common.feature.autorizacao.entities.Usuario;
import mppb.restapi.cursos.feature.common.service.ServidorService;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.curso.entity.Ministrante;
import mppb.restapi.cursos.feature.curso.repository.CursoRepository;
import mppb.restapi.cursos.feature.curso.repository.MinistranteRepository;
import mppb.restapi.cursos.feature.lookup.repository.ModalidadeRepository;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Log4j2
@Transactional
public class CursoService extends AbstractService<Curso, Long>{

    private final MinistranteRepository ministranteRepository;
    private final ServidorService servidorService;
    private final CursoRepository cursoRepository;

    public CursoService(EntityManager entityManager, CursoRepository cursoRepository, MinistranteRepository ministranteRepository, ServidorService servidorService){
        super(entityManager, cursoRepository);
        this.ministranteRepository = ministranteRepository;
        this.servidorService  = servidorService;
        this.cursoRepository = cursoRepository;
    }


    @Override
    @Transactional(readOnly = true)
    public Optional<Curso> findById(Long id) {
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

    @Override
    public Curso save(Curso curso) {
        if (curso.getHashInscricao() == null) {
            curso.setHashInscricao(UUID.randomUUID());
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

    @Transactional(readOnly = true)
    public Optional<Curso> findByHashInscricao(UUID hash) {
        return cursoRepository.findByHashInscricao(hash);
    }

    @Transactional(readOnly = true)
    public boolean isPeriodoInscricaoValido(Curso curso) {
        if (curso.getInscricaoDataInicial() == null || curso.getInscricaoDataFinal() == null) {
            // Se não há datas definidas, consideramos o período como inválido para inscrição.
            return false;
        }
        LocalDateTime agora = LocalDateTime.now();
        return !agora.isBefore(curso.getInscricaoDataInicial()) && !agora.isAfter(curso.getInscricaoDataFinal());
    }

    // TODO Método placeholder para implementação futura
    @Transactional(readOnly = true)
    public boolean isPublicoAlvoValido(String email, Curso curso) {
        // LÓGICA FUTURA:
        // 1. Verificar se o curso tem um público-alvo restrito (Membros/Servidores).
        // 2. Se sim, consultar a base do RH com o e-mail/CPF para validar o vínculo.
        // 3. Retornar true ou false com base na validação.
        System.out.println("Validando público-alvo para: " + email); // Log de placeholder
        return true; // Por enquanto, sempre retorna válido.
    }
}
