package mppb.restapi.mobile.feature.processoconsulta.service;

import mppb.restapi.mobile.feature.processoconsulta.dto.ConsultaProcessoFilterDTO;
import mppb.restapi.mobile.feature.processoconsulta.dto.ConsultaProcessoResponseDTO;
import mppb.restapi.mobile.feature.processoconsulta.repository.ProtocoloConsultaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProcessoConsultaService {

    @Autowired
    private ProtocoloConsultaRepository repository;

    public List<ConsultaProcessoResponseDTO> consultarProcessos(ConsultaProcessoFilterDTO filtro) {
        
        // 1. Validação do CAPTCHA removida daqui. O Captcha real agora é validado no ProcessoDetalhesService!

        // 2. Validação de Filtros Mínimos
        if (!possuiFiltroMinimo(filtro)) {
            throw new IllegalArgumentException("É necessário informar ao menos um parâmetro de pesquisa válido (Número, CPF, CNPJ ou Nome).");
        }

        // 3. Consulta ao banco
        return repository.consultarProcessos(filtro);
    }

    private boolean possuiFiltroMinimo(ConsultaProcessoFilterDTO filtro) {
        if (filtro.getNumeroProcesso() != null && !filtro.getNumeroProcesso().trim().isEmpty()) return true;
        if (filtro.getNumeroOrigem() != null && !filtro.getNumeroOrigem().trim().isEmpty()) return true;
        if (filtro.getNumeroCnmp() != null && !filtro.getNumeroCnmp().trim().isEmpty()) return true;
        if (filtro.getCpf() != null && !filtro.getCpf().trim().isEmpty()) return true;
        if (filtro.getCnpj() != null && !filtro.getCnpj().trim().isEmpty()) return true;
        if (filtro.getInteressado() != null && !filtro.getInteressado().trim().isEmpty()) return true;
        
        return false;
    }
}
