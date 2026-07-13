package mppb.restapi.mobile.feature.contracheque.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mppb.restapi.mobile.feature.contracheque.dto.CompetenciaDto;
import mppb.restapi.mobile.feature.contracheque.dto.ContrachequeResponseDto;
import mppb.restapi.mobile.feature.contracheque.dto.LancamentoDto;
import mppb.restapi.mobile.feature.contracheque.repository.ContrachequeRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ContrachequeService {

    private final ContrachequeRepository contrachequeRepository;

    @Autowired
    public ContrachequeService(ContrachequeRepository contrachequeRepository) {
        this.contrachequeRepository = contrachequeRepository;
    }

    public Integer getMatriculaByLogin(String login) {
        return contrachequeRepository.getMatriculaByLogin(login);
    }

    public List<CompetenciaDto> listarCompetenciasDisponiveis(Integer matricula) {
        List<CompetenciaDto> competencias = contrachequeRepository.findCompetenciasByMatricula(matricula);

        return competencias;
    }

    public ContrachequeResponseDto detalharContracheque(Integer matricula, Integer mes, Integer ano) {
        ContrachequeResponseDto contracheque = contrachequeRepository.findContracheque(matricula, mes, ano);
        

        
        if (contracheque != null && contracheque.getLancamentos() != null) {
            BigDecimal totalVantagens = BigDecimal.ZERO;
            BigDecimal totalDescontos = BigDecimal.ZERO;

            for (LancamentoDto lancamento : contracheque.getLancamentos()) {
                if ("V".equals(lancamento.getTipo())) {
                    totalVantagens = totalVantagens.add(lancamento.getValor());
                } else if ("D".equals(lancamento.getTipo())) {
                    totalDescontos = totalDescontos.add(lancamento.getValor());
                }
            }

            contracheque.setTotalVantagens(totalVantagens);
            contracheque.setTotalDescontos(totalDescontos);
            contracheque.setSalarioLiquido(totalVantagens.subtract(totalDescontos));

            // Hash handling
            String hash = contrachequeRepository.getHashContracheque(matricula, mes, ano);
            if (hash == null || hash.isEmpty()) {
                hash = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
                contrachequeRepository.insertHashContracheque(hash, matricula, mes, ano);
            } else {
                contrachequeRepository.incrementaEmissaoHash(hash);
            }
            contracheque.setCodigoValidacao(hash.toUpperCase());
        }
        
        return contracheque;
    }
}
