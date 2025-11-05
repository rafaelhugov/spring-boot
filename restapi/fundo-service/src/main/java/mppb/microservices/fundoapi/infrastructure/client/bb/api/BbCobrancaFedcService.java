package mppb.microservices.fundoapi.infrastructure.client.bb.api;

import lombok.RequiredArgsConstructor;
import mppb.microservices.fundoapi.infrastructure.client.bb.token.FedcTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BbCobrancaFedcService extends AbstractBbCobrancaService {

    private final FedcTokenProvider tokenProvider;

    @Value("${microsservicos.bb.fundos.FEDC.client-id}")
    private String clientId;

    @Value("${microsservicos.bb.fundos.FEDC.gw-dev-app-key}")
    private String devAppKey;

    @Override
    protected String getToken() {
        return tokenProvider.getToken();
    }

    @Override
    protected String getClientId() {
        return clientId;
    }

    @Override
    protected String getDevAppKey() {
        return devAppKey;
    }
}
