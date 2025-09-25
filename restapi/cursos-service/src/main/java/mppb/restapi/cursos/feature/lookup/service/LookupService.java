package mppb.restapi.cursos.feature.lookup.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import mppb.restapi.cursos.feature.lookup.entities.Modalidade;
import mppb.restapi.cursos.feature.lookup.entities.PapelMinistrante;
import mppb.restapi.cursos.feature.lookup.entities.PublicoAlvo;
import mppb.restapi.cursos.feature.lookup.entities.TipoEvento;
import mppb.restapi.cursos.feature.lookup.repository.ModalidadeRepository;
import mppb.restapi.cursos.feature.lookup.repository.PapelMinistranteRepository;
import mppb.restapi.cursos.feature.lookup.repository.PublicoAlvoRepository;
import mppb.restapi.cursos.feature.lookup.repository.TipoEventoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
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
