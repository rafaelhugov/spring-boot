package mppb.microservices.cursosapi.feature.common.service;

import org.springframework.transaction.annotation.Transactional;
import mppb.microservices.cursosapi.feature.common.projection.ServidorProjection;
import mppb.microservices.cursosapi.feature.common.repository.ServidorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ServidorService {

    private final ServidorRepository servidorRepository;

    public ServidorService(ServidorRepository servidorRepository) {
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
