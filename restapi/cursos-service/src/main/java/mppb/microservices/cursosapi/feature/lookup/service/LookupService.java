package mppb.microservices.cursosapi.feature.lookup.service;

import lombok.RequiredArgsConstructor;
import mppb.microservices.common.core.service.AbstractService;
import mppb.microservices.cursosapi.feature.lookup.entities.Modalidade;
import mppb.microservices.cursosapi.feature.lookup.entities.PapelMinistrante;
import mppb.microservices.cursosapi.feature.lookup.entities.PublicoAlvo;
import mppb.microservices.cursosapi.feature.lookup.entities.TipoEvento;
import mppb.microservices.cursosapi.feature.lookup.repository.ModalidadeRepository;
import mppb.microservices.cursosapi.feature.lookup.repository.PapelMinistranteRepository;
import mppb.microservices.cursosapi.feature.lookup.repository.PublicoAlvoRepository;
import mppb.microservices.cursosapi.feature.lookup.repository.TipoEventoRepository;
import org.springframework.stereotype.Service;

import java.awt.geom.Area;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LookupService {

    private final ModalidadeRepository modalidadeRepository;
    private final TipoEventoRepository tipoEventoRepository;
    private final PublicoAlvoRepository publicoAlvoRepository;
    private final PapelMinistranteRepository papelMinistranteRepository;

    public List<Modalidade> findAllModalidades() {
        return modalidadeRepository.findAll();
    }

    public List<TipoEvento> findAllTiposEvento()  {
        return tipoEventoRepository.findAll();
    }

    public List<PublicoAlvo> findAllPublicoAlvo() {
        return publicoAlvoRepository.findAll();
    }

    public List<PapelMinistrante> findAllPapelMinistrante() {
        return papelMinistranteRepository.findAll();
    }
}
