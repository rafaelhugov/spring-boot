package br.gov.mppb.ncap.feature.servidor;

import jakarta.persistence.EntityManager;
import mppb.restapi.common.core.service.AbstractService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ServidorService extends AbstractService<Servidor, Long> {

    private final ServidorRepository servidorRepository;

    public ServidorService(EntityManager entityManager, ServidorRepository servidorRepository) {
        super(entityManager, servidorRepository);
        this.servidorRepository = servidorRepository;
    }

    public List<ServidorProjection> buscarPorNome(String nome) {
        return servidorRepository.buscarServidoresPorNome(nome);
    }
}
