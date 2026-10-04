package mppb.restapi.mobile.feature.pagamentos.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import mppb.restapi.mobile.feature.pagamentos.dto.PagamentoItemDto;
import mppb.restapi.mobile.feature.pagamentos.dto.PagamentosResponseDto;
import mppb.restapi.mobile.feature.pagamentos.repository.PagamentosRepository;

import mppb.restapi.mobile.feature.synthetic.dataset.SyntheticDataService;

@Slf4j
@Service
public class PagamentosService {

    private final PagamentosRepository pagamentosRepository;

    private SyntheticDataService syntheticDataService;

    public PagamentosService(PagamentosRepository pagamentosRepository) {
        this.pagamentosRepository = pagamentosRepository;
    }

    @Autowired
    public PagamentosService(PagamentosRepository pagamentosRepository, @Autowired(required = false) SyntheticDataService syntheticDataService) {
        this.pagamentosRepository = pagamentosRepository;
        this.syntheticDataService = syntheticDataService;
    }

    public String getCpfByLogin(String login) {
        if (syntheticDataService != null && syntheticDataService.isSyntheticLogin(login)) {
            return syntheticDataService.getCpfByLogin(login);
        }
        return pagamentosRepository.getCpfByLogin(login);
    }

    public List<Integer> listarAnosDisponiveis(String cpf) {
        if (syntheticDataService != null && syntheticDataService.isSyntheticCpf(cpf)) {
            return syntheticDataService.getAnosPagamento(cpf);
        }
        return pagamentosRepository.findAnosComPagamentos(cpf);
    }

    public PagamentosResponseDto listarPagamentos(String exercicio, String cpf) {
        if (syntheticDataService != null && syntheticDataService.isSyntheticCpf(cpf)) {
            return syntheticDataService.getPagamentos(exercicio, cpf);
        }
        List<PagamentoItemDto> pagamentos = pagamentosRepository.findPagamentosByExercicioAndCpf(exercicio, cpf);

        PagamentosResponseDto response = new PagamentosResponseDto();
        response.setNuExercicio(exercicio);
        response.setPagamentos(pagamentos);
        response.setTotalPagamentos(pagamentos.size());

        BigDecimal totalBruto = pagamentos.stream()
            .map(p -> p.getVlBruto() != null ? p.getVlBruto() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalLiquido = pagamentos.stream()
            .map(p -> p.getVlLiquido() != null ? p.getVlLiquido() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        response.setTotalBruto(totalBruto);
        response.setTotalLiquido(totalLiquido);

        return response;
    }
}
