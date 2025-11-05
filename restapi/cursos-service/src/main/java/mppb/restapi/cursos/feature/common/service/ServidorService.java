package mppb.restapi.cursos.feature.common.service;

import jakarta.persistence.EntityManager;
import mppb.restapi.common.core.service.AbstractService;
import mppb.restapi.cursos.feature.common.entities.Servidor;
import mppb.restapi.cursos.feature.curso.repository.CursoRepository;
import org.springframework.transaction.annotation.Transactional;
import mppb.restapi.cursos.feature.common.projection.ServidorProjection;
import mppb.restapi.cursos.feature.common.repository.ServidorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ServidorService extends AbstractService<Servidor, Long> {

    private final ServidorRepository servidorRepository;

    public ServidorService(EntityManager entityManager, CursoRepository cursoRepository, ServidorRepository servidorRepository){
        super(entityManager, servidorRepository);
        this.servidorRepository = servidorRepository;
    }

    @Transactional(readOnly = true)
    public List<ServidorProjection> buscarServidoresPorNome(String nome) {
        return servidorRepository.buscarServidoresPorNome(nome);
    }

    @Transactional(readOnly = true)
    public Optional<ServidorProjection> buscarServidorPorId(Integer id) {
        return servidorRepository.buscarServidorPorId(id);
    }
}
