package mppb.restapi.mobile.feature.diarias.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import mppb.restapi.mobile.feature.diarias.dto.DiariaPagamentoDto;
import mppb.restapi.mobile.feature.diarias.dto.DiariasResponseDto;
import mppb.restapi.mobile.feature.diarias.repository.DiariasRepository;

@Slf4j
@Service
public class DiariasService {

    private final DiariasRepository diariasRepository;

    @Autowired
    public DiariasService(DiariasRepository diariasRepository) {
        this.diariasRepository = diariasRepository;
    }

    public String getCpfByLogin(String login) {
        return diariasRepository.getCpfByLogin(login);
    }

    public DiariasResponseDto listarDiarias(String exercicio, String cpf) {
        List<DiariaPagamentoDto> pagamentos = diariasRepository.findDiariasByExercicioAndCpf(exercicio, cpf);

        DiariasResponseDto response = new DiariasResponseDto();
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
