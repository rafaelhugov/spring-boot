package mppb.microservices.fundoapi.feature.client.bb.token;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FedcTokenProvider extends AbstractTokenProvider {

    public FedcTokenProvider(@Value("${microsservicos.bb.oauth.token-url}") String tokenUrl,
                             @Value("${microsservicos.bb.fundos.FEDC.client-id}") String clientId,
                             @Value("${microsservicos.bb.fundos.FEDC.client-secret}") String clientSecret){
        this.setTokenUrl(tokenUrl);
        this.setClientId(clientId);
        this.setClientSecret(clientSecret);
    }
}
