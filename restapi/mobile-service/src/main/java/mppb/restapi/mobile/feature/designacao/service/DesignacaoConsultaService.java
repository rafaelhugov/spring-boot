package mppb.restapi.mobile.feature.designacao.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import mppb.restapi.mobile.feature.designacao.dto.DesignacoesAtivasResponse;
import mppb.restapi.mobile.feature.designacao.mapper.DesignacaoMapper;
import mppb.restapi.mobile.feature.designacao.model.DesignacaoAtivaRow;
import mppb.restapi.mobile.feature.designacao.repository.DesignacaoViewRepository;

@Service
@RequiredArgsConstructor
public class DesignacaoConsultaService {

    private final DesignacaoInicializacaoService inicializacaoService;
    private final DesignacaoViewRepository viewRepository;
    private final DesignacaoMapper mapper;

    public DesignacoesAtivasResponse listarAtivas(String matricula) {
        inicializacaoService.inicializarSeNecessario(matricula);
        List<DesignacaoAtivaRow> rows = viewRepository.findAtivasByMatricula(matricula);
        return mapper.toResponse(rows);
    }
}
