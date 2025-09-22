package mppb.microservices.fundoapi.feature.cobranca.service;

import jakarta.persistence.EntityManager;
import lombok.extern.log4j.Log4j2;
import mppb.microservices.common.infrastructure.report.JasperService;
import mppb.microservices.common.core.service.AbstractService;
import mppb.microservices.fundoapi.infrastructure.client.bb.api.BbCobrancaFddService;
import mppb.microservices.fundoapi.infrastructure.client.bb.api.BbCobrancaFedcService;
import mppb.microservices.fundoapi.feature.cobranca.entity.Solicitacao;
import mppb.microservices.fundoapi.feature.cobranca.repository.SolicitacaoRepository;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class SolicitacaoService extends AbstractService<Solicitacao, Long> {

    private final SolicitacaoRepository solicitacaoRepository;

    private final EntityManager entityManager;

    private final BbCobrancaFedcService bbCobrancaFedcService;

    private final BbCobrancaFddService bbCobrancaFddService;

    private final JasperService jasperService;


    public SolicitacaoService(EntityManager entityManager, SolicitacaoRepository solicitacaoRepository, BbCobrancaFddService bbCobrancaFddService, BbCobrancaFedcService bbCobrancaFedcService, JasperService jasperService){
        super(entityManager, solicitacaoRepository);
        this.entityManager = entityManager;
        this.solicitacaoRepository = solicitacaoRepository;
        this.bbCobrancaFddService = bbCobrancaFddService;
        this.bbCobrancaFedcService = bbCobrancaFedcService;
        this.jasperService = jasperService;
    }
}
