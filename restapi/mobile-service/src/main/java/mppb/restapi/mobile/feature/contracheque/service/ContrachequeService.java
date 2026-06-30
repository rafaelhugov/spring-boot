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
        if (competencias == null || competencias.isEmpty()) {
            // MOCK PARA TESTE E DESENVOLVIMENTO
            return java.util.Arrays.asList(
                new CompetenciaDto(4, 2026),
                new CompetenciaDto(3, 2026),
                new CompetenciaDto(2, 2026)
            );
        }
        return competencias;
    }

    public ContrachequeResponseDto detalharContracheque(Integer matricula, Integer mes, Integer ano) {
        ContrachequeResponseDto contracheque = contrachequeRepository.findContracheque(matricula, mes, ano);
        
        if (contracheque == null || contracheque.getLancamentos() == null || contracheque.getLancamentos().isEmpty()) {
            // MOCK PARA TESTE E DESENVOLVIMENTO
            contracheque = new ContrachequeResponseDto();
            contracheque.setNome("SERVIDOR DE TESTE (MOCK)");
            contracheque.setMatricula(String.valueOf(matricula));
            contracheque.setCargo("TÉCNICO MINISTERIAL");
            contracheque.setMesAno(String.format("%02d/%d", mes, ano));
            
            mppb.restapi.mobile.feature.contracheque.dto.LancamentoDto l1 = new mppb.restapi.mobile.feature.contracheque.dto.LancamentoDto();
            l1.setCodigo("001");
            l1.setDescricao("VENCIMENTO BÁSICO");
            l1.setValor(new BigDecimal("6500.00"));
            l1.setTipo("V");
            
            mppb.restapi.mobile.feature.contracheque.dto.LancamentoDto l2 = new mppb.restapi.mobile.feature.contracheque.dto.LancamentoDto();
            l2.setCodigo("002");
            l2.setDescricao("AUXÍLIO ALIMENTAÇÃO");
            l2.setValor(new BigDecimal("1800.00"));
            l2.setTipo("V");

            mppb.restapi.mobile.feature.contracheque.dto.LancamentoDto l3 = new mppb.restapi.mobile.feature.contracheque.dto.LancamentoDto();
            l3.setCodigo("101");
            l3.setDescricao("PREVIDÊNCIA ESTADUAL");
            l3.setValor(new BigDecimal("715.00"));
            l3.setTipo("D");
            
            mppb.restapi.mobile.feature.contracheque.dto.LancamentoDto l4 = new mppb.restapi.mobile.feature.contracheque.dto.LancamentoDto();
            l4.setCodigo("102");
            l4.setDescricao("IMPOSTO DE RENDA");
            l4.setValor(new BigDecimal("450.00"));
            l4.setTipo("D");
            
            contracheque.setLancamentos(java.util.Arrays.asList(l1, l2, l3, l4));
        }
        
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
        }
        
        return contracheque;
    }
}
