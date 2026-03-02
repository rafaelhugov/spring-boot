package mppb.restapi.cursos.feature.frequencia.service;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.error.NotFoundException;
import mppb.restapi.common.core.service.AbstractService;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.curso.repository.CursoRepository;
import mppb.restapi.cursos.feature.frequencia.converter.AulaConverter;
import mppb.restapi.cursos.feature.frequencia.dto.AulaDto;
import mppb.restapi.cursos.feature.frequencia.entity.Aula;
import mppb.restapi.cursos.feature.frequencia.entity.Chamada;
import mppb.restapi.cursos.feature.frequencia.repository.AulaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@Transactional
public class AulaService extends AbstractService<Aula, Long> {

    private final AulaRepository aulaRepository;
    private final CursoRepository cursoRepository;
    private final AulaConverter aulaConverter; // Injeção

    public AulaService(EntityManager entityManager, AulaRepository aulaRepository, CursoRepository cursoRepository, AulaConverter aulaConverter) {
        super(entityManager, aulaRepository);
        this.aulaRepository = aulaRepository;
        this.cursoRepository = cursoRepository;
        this.aulaConverter = aulaConverter; // Atribuição
    }


    /**
     * Busca todas as aulas do curso e as converte para DTO, garantindo que
     * o carregamento da Chamada (JOIN FETCH) e a conversão ocorram na transação.
     * @param cursoId ID do curso.
     * @return Lista de AulaDto.
     */
    @Transactional(readOnly = true)
    public List<AulaDto> findAllByCursoId(Long cursoId) {
        List<Aula> aulas = aulaRepository.findAllByCursoId(cursoId);
        // A conversão ocorre aqui, garantindo o acesso às chamadas antes de fechar a sessão.
        return aulaConverter.toDto(aulas);
    }
    public Aula createAula(Long cursoId, Aula aula) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new NotFoundException("Curso não encontrado: " + cursoId));

        aula.setCurso(curso);
        // O @PrePersist na entidade Chamada garante que tokens sejam gerados se adicionarmos chamadas aqui
        return aulaRepository.save(aula);
    }

    public Chamada adicionarChamada(Long aulaId, String tipo) {
        Aula aula = findById(aulaId)
                .orElseThrow(() -> new NotFoundException("Aula não encontrada: " + aulaId));

        Chamada novaChamada = new Chamada();
        novaChamada.setAula(aula);
        novaChamada.setDataGeracao(LocalDateTime.now());
        // O token será gerado automaticamente pelo @PrePersist da entidade Chamada

        aula.getChamadas().add(novaChamada);
        aulaRepository.save(aula);

        return novaChamada;
    }
}