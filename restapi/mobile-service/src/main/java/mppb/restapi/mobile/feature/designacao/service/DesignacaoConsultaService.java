package mppb.restapi.mobile.feature.designacao.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import mppb.restapi.mobile.feature.designacao.dto.DesignacoesAtivasResponse;
import mppb.restapi.mobile.feature.designacao.mapper.DesignacaoMapper;
import mppb.restapi.mobile.feature.designacao.model.DesignacaoAtivaRow;
import mppb.restapi.mobile.feature.designacao.repository.DesignacaoViewRepository;

import org.springframework.beans.factory.annotation.Autowired;
import mppb.restapi.mobile.feature.synthetic.dataset.SyntheticDataService;

@Service
public class DesignacaoConsultaService {

    private final DesignacaoInicializacaoService inicializacaoService;
    private final DesignacaoViewRepository viewRepository;
    private final DesignacaoMapper mapper;

    private SyntheticDataService syntheticDataService;

    public DesignacaoConsultaService(
            DesignacaoInicializacaoService inicializacaoService,
            DesignacaoViewRepository viewRepository,
            DesignacaoMapper mapper) {
        this.inicializacaoService = inicializacaoService;
        this.viewRepository = viewRepository;
        this.mapper = mapper;
    }

    @Autowired
    public DesignacaoConsultaService(
            DesignacaoInicializacaoService inicializacaoService,
            DesignacaoViewRepository viewRepository,
            DesignacaoMapper mapper,
            @Autowired(required = false) SyntheticDataService syntheticDataService) {
        this.inicializacaoService = inicializacaoService;
        this.viewRepository = viewRepository;
        this.mapper = mapper;
        this.syntheticDataService = syntheticDataService;
    }

    public DesignacoesAtivasResponse listarAtivas(String matricula) {
        if (syntheticDataService != null && syntheticDataService.isSyntheticMatricula(matricula)) {
            return syntheticDataService.getDesignacoesAtivas(matricula);
        }
        inicializacaoService.inicializarSeNecessario(matricula);
        List<DesignacaoAtivaRow> rows = viewRepository.findAtivasByMatricula(matricula);
        return mapper.toResponse(rows);
    }
}
