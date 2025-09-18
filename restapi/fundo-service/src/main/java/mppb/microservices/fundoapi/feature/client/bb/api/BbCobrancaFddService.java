package mppb.microservices.fundoapi.feature.client.bb.api;

import lombok.RequiredArgsConstructor;
import mppb.microservices.fundoapi.feature.client.bb.token.FddTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BbCobrancaFddService extends AbstractBbCobrancaService {

    private final FddTokenProvider tokenProvider;

    @Value("${microsservicos.bb.fundos.FDD.client-id}")
    private String clientId;

    @Value("${microsservicos.bb.fundos.FDD.gw-dev-app-key}")
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
