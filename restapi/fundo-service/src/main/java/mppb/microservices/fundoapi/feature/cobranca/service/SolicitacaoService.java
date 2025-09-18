package mppb.microservices.fundoapi.feature.cobranca.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.log4j.Log4j2;
import mppb.microservices.common.error.AppException;
import mppb.microservices.common.error.ValidationException;
import mppb.microservices.common.error.feign.ClientApiException;
import mppb.microservices.common.infrastructure.report.JasperService;
import mppb.microservices.common.service.AbstractService;
import mppb.microservices.common.util.NumeroUtil;
import mppb.microservices.fundoapi.feature.client.bb.api.BbCobrancaFddService;
import mppb.microservices.fundoapi.feature.client.bb.api.BbCobrancaFedcService;
import mppb.microservices.fundoapi.feature.client.bb.dto.request.BbBoletoRequestDto;
import mppb.microservices.fundoapi.feature.client.bb.dto.request.BbPagadorDto;
import mppb.microservices.fundoapi.feature.client.bb.dto.response.BbBoletoResponseDto;
import mppb.microservices.fundoapi.feature.cobranca.entity.Boleto;
import mppb.microservices.fundoapi.feature.cobranca.entity.Fundo;
import mppb.microservices.fundoapi.feature.cobranca.entity.Solicitacao;
import mppb.microservices.fundoapi.feature.cobranca.enums.TipoSolicitacaoEnum;
import mppb.microservices.fundoapi.feature.cobranca.repository.BoletoRepository;
import mppb.microservices.fundoapi.feature.cobranca.repository.SolicitacaoRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
