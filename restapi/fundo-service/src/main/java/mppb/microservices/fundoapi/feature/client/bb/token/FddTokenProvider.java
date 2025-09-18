package mppb.microservices.fundoapi.feature.client.bb.token;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FddTokenProvider extends AbstractTokenProvider {

    public FddTokenProvider(@Value("${microsservicos.bb.oauth.token-url}") String tokenUrl,
                            @Value("${microsservicos.bb.fundos.FDD.client-id}") String clientId,
                            @Value("${microsservicos.bb.fundos.FDD.client-secret}") String clientSecret){
        this.setTokenUrl(tokenUrl);
        this.setClientId(clientId);
        this.setClientSecret(clientSecret);
    }

}
