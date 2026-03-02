package mppb.restapi.mobile.feature.lookup.service;

import lombok.extern.slf4j.Slf4j;
import mppb.restapi.mobile.feature.cadastro.entities.EntidadeTutora;
import mppb.restapi.mobile.feature.cadastro.repository.EntidadeTutoraRepository;
import mppb.restapi.mobile.feature.inscricao.enums.SituacaoInscricaoEnum;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import mppb.restapi.mobile.feature.lookup.entities.Modalidade;
import mppb.restapi.mobile.feature.lookup.entities.PapelMinistrante;
import mppb.restapi.mobile.feature.lookup.entities.PublicoAlvo;
import mppb.restapi.mobile.feature.lookup.entities.TipoEvento;
import mppb.restapi.mobile.feature.lookup.repository.ModalidadeRepository;
import mppb.restapi.mobile.feature.lookup.repository.PapelMinistranteRepository;
import mppb.restapi.mobile.feature.lookup.repository.PublicoAlvoRepository;
import mppb.restapi.mobile.feature.lookup.repository.TipoEventoRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LookupService {

    private final ModalidadeRepository modalidadeRepository;
    private final TipoEventoRepository tipoEventoRepository;
    private final PublicoAlvoRepository publicoAlvoRepository;
    private final PapelMinistranteRepository papelMinistranteRepository;
    private final EntidadeTutoraRepository entidadeTutoraRepository;

    @Transactional(readOnly = true)
    public List<Modalidade> findAllModalidades() {
        return modalidadeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<TipoEvento> findAllTiposEvento()  {
        return tipoEventoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<PublicoAlvo> findAllPublicoAlvo() {
        return publicoAlvoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<PapelMinistrante> findAllPapelMinistrante() {
        return papelMinistranteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<EntidadeTutora> findAllEntidadesTutoras() {
        return entidadeTutoraRepository.findAll();
    }

    public List<SituacaoInscricaoEnum> findAllSituacoesInscricao() {
        return Arrays.asList(SituacaoInscricaoEnum.values());
    }
}
