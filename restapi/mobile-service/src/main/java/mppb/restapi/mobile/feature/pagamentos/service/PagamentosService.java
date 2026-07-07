package mppb.restapi.mobile.feature.pagamentos.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import mppb.restapi.mobile.feature.pagamentos.dto.PagamentoItemDto;
import mppb.restapi.mobile.feature.pagamentos.dto.PagamentosResponseDto;
import mppb.restapi.mobile.feature.pagamentos.repository.PagamentosRepository;

@Slf4j
@Service
public class PagamentosService {

    private final PagamentosRepository pagamentosRepository;

    @Autowired
    public PagamentosService(PagamentosRepository pagamentosRepository) {
        this.pagamentosRepository = pagamentosRepository;
    }

    public String getCpfByLogin(String login) {
        return pagamentosRepository.getCpfByLogin(login);
    }

    public PagamentosResponseDto listarPagamentos(String exercicio, String cpf) {
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
