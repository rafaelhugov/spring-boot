package mppb.microservices.fundoapi.feature.cobranca.service;

import mppb.microservices.fundoapi.infrastructure.client.bc.api.BcSelicService;
import mppb.microservices.fundoapi.infrastructure.client.bc.dto.BcValorSelicDto;
import mppb.microservices.fundoapi.feature.cobranca.dto.ValorCorrigidoDto;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RefisService {

    private final BcSelicService bcSelicService;

    public RefisService(BcSelicService bcSelicService) {
        this.bcSelicService = bcSelicService;
    }

    public ValorCorrigidoDto corrigirValor(double valorInicial, LocalDate dataInicial, LocalDate dataFinal) {

        ValorCorrigidoDto valorCorrigido = corrigirValorSelic(valorInicial, dataInicial, dataFinal);
        double percentualTaxa = 1.0;
        double valorFinal = aplicarAcrescimoPercentual(valorCorrigido.getValorCorrigidoSelic(), percentualTaxa);

        valorCorrigido.setPercentualTaxa(percentualTaxa);
        valorCorrigido.setValorFinalCorrigido(valorFinal);

        return valorCorrigido;
    }

    private ValorCorrigidoDto corrigirValorSelic(double valorInicial, LocalDate dataInicial, LocalDate dataFinal) {
        List<BcValorSelicDto> dados = bcSelicService.buscarSelicDiaria(dataInicial, dataFinal);
        List<BcValorSelicDto> dadosFiltrados = filtrarAteUltimoDiaDisponivel(dados, dataFinal);

        double indiceCorrecao = 1.0;

        for (BcValorSelicDto dado : dadosFiltrados) {
            double taxa = Double.parseDouble(dado.valor()) / 100.0;
            indiceCorrecao *= (1 + taxa);
        }

        double valorCorrigidoSelic = valorInicial * indiceCorrecao;
        double variacaoPercentual = (indiceCorrecao - 1) * 100.0;

        ValorCorrigidoDto valor = new ValorCorrigidoDto();

        valor.setIndiceSelic(round(indiceCorrecao, 8));
        valor.setPercentualSelic(round(variacaoPercentual, 6));
        valor.setValorCorrigidoSelic(round(valorCorrigidoSelic, 2));

    return valor;
    }

    public double aplicarAcrescimoPercentual(double valor, double percentual) {
        double fator = 1 + (percentual / 100.0);
        return round(valor * fator, 2);
    }

    private double round(double valor, int casasDecimais) {
        double fator = Math.pow(10, casasDecimais);
        return Math.round(valor * fator) / fator;
    }

    private List<BcValorSelicDto> filtrarAteUltimoDiaDisponivel(List<BcValorSelicDto> dados, LocalDate dataFinal) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        return dados.stream()
                .map(dado -> new DadoComData(dado, LocalDate.parse(dado.data(), formatter)))
                .filter(dc -> dc.data.isBefore(dataFinal)) // exclui a dataFinal, se presente
                .sorted(Comparator.comparing(dc -> dc.data))
                .map(dc -> dc.dado)
                .collect(Collectors.toList());
    }

    private class DadoComData {
        BcValorSelicDto dado;
        LocalDate data;

        DadoComData(BcValorSelicDto dado, LocalDate data) {
            this.dado = dado;
            this.data = data;
        }
    }
}
