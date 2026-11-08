package mppb.restapi.mobile.feature.appversion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "app.update")
public class AppUpdateProperties {

    /**
     * Build mínima aceita para Android (ex: 129).
     * Se 0, nenhuma versão é bloqueada.
     */
    private int minBuildAndroid = 0;

    /**
     * Build mínima aceita para iOS.
     * Se 0, nenhuma versão é bloqueada.
     */
    private int minBuildIos = 0;

    /**
     * URL direta da Google Play Store.
     */
    private String storeUrlAndroid = "https://play.google.com/store/apps/details?id=br.mp.mppbapp.app";

    /**
     * URL direta da Apple App Store.
     */
    private String storeUrlIos = "https://apps.apple.com/app/mppb/id6825377891";

    /**
     * Mensagem institucional exibida no diálogo de atualização obrigatória.
     */
    private String mensagem = "Uma nova versão do MPPB+ está disponível com importantes melhorias e correções. Atualize para continuar utilizando o aplicativo.";

    /**
     * Retorna a build mínima para a plataforma indicada ("android" ou "ios").
     */
    public int getMinBuildParaPlataforma(String platform) {
        if (platform == null) {
            return 0;
        }
        if ("ios".equalsIgnoreCase(platform.trim())) {
            return minBuildIos;
        }
        return minBuildAndroid;
    }

    /**
     * Retorna a URL da loja para a plataforma indicada.
     */
    public String getStoreUrlParaPlataforma(String platform) {
        if (platform != null && "ios".equalsIgnoreCase(platform.trim())) {
            return storeUrlIos;
        }
        return storeUrlAndroid;
    }
}

