package mppb.microservices.cursosapi.feature.lookup.service;

import lombok.RequiredArgsConstructor;
import mppb.microservices.common.core.service.AbstractService;
import mppb.microservices.cursosapi.feature.lookup.entities.Modalidade;
import mppb.microservices.cursosapi.feature.lookup.repository.ModalidadeRepository;
import org.springframework.stereotype.Service;

import java.awt.geom.Area;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LookupService {

    private final ModalidadeRepository modalidadeRepository;

    public List<Modalidade> findAllModalidades() {
        return modalidadeRepository.findAll();
    }
}
