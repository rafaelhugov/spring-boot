package mppb.restapi.mobile.feature.processoconsulta.service;

import mppb.restapi.mobile.feature.processoconsulta.dto.ConsultaProcessoFilterDTO;
import mppb.restapi.mobile.feature.processoconsulta.dto.ConsultaProcessoResponseDTO;
import mppb.restapi.mobile.feature.processoconsulta.dto.ProcessoFiltroOptionDTO;
import mppb.restapi.mobile.feature.processoconsulta.dto.ProcessoFiltrosDTO;
import mppb.restapi.mobile.feature.processoconsulta.repository.ProtocoloConsultaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProcessoConsultaService {

    @Autowired
    private ProtocoloConsultaRepository repository;

    public ProcessoFiltrosDTO listarFiltros() {
        return new ProcessoFiltrosDTO(
                repository.listarComarcas(),
                List.of(
                        new ProcessoFiltroOptionDTO("200910004", "Inquérito Civil"),
                        new ProcessoFiltroOptionDTO("200910002", "Notícia de Fato"),
                        new ProcessoFiltroOptionDTO("200910005", "Procedimento Administrativo"),
                        new ProcessoFiltroOptionDTO("200910003", "Procedimento Preparatório"),
                        new ProcessoFiltroOptionDTO("200000279", "Inquérito Policial"),
                        new ProcessoFiltroOptionDTO("200001733", "Procedimento Investigatório do MP"),
                        new ProcessoFiltroOptionDTO("-1", "Processo Judicial"),
                        new ProcessoFiltroOptionDTO("-2", "Administrativo")),
                List.of(
                        new ProcessoFiltroOptionDTO("T", "Termos de Ajustamento de Conduta firmados"),
                        new ProcessoFiltroOptionDTO("R", "Recomendações expedidas"),
                        new ProcessoFiltroOptionDTO("A", "Audiências Públicas realizadas")),
                List.of(
                        new ProcessoFiltroOptionDTO("T", "Todos"),
                        new ProcessoFiltroOptionDTO("A", "Ativo"),
                        new ProcessoFiltroOptionDTO("I", "Inativo")));
    }

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
        if (filtro.getIdComarca() != null && filtro.getIdComarca() > 0) return true;
        if (filtro.getIdClasse() != null) return true;
        if (filtro.getNomePromotor() != null && !filtro.getNomePromotor().trim().isEmpty()) return true;
        if (filtro.getIdTipoAtividade() != null && !filtro.getIdTipoAtividade().trim().isEmpty()) return true;
        if (filtro.getStatus() != null && !filtro.getStatus().trim().isEmpty()) return true;
        
        return false;
    }
}
