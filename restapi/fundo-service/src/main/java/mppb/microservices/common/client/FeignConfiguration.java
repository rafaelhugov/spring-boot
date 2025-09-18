package mppb.microservices.common.client;

import feign.Client;
import feign.Feign;
import feign.codec.ErrorDecoder;
import mppb.microservices.common.error.feign.FeignErrorDecoder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContexts;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

@Configuration
@EnableFeignClients
public class FeignConfiguration {

    @Bean
    public Client feignClient() {
        return new Client.Default(getSSLSocketFactory(), new NoopHostnameVerifier());
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new FeignErrorDecoder();
    }

    private SSLSocketFactory getSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build();

            return sslContext.getSocketFactory();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
