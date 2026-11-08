package mppb.restapi.mobile.feature.appversion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppVersaoResponseDTO {

    /**
     * Indica se a atualização é obrigatória/bloqueante.
     */
    private boolean forceUpdate;

    /**
     * Build mínima requerida pelo servidor para a plataforma informada.
     */
    private int minBuild;

    /**
     * Build informada pelo cliente.
     */
    private int currentBuild;

    /**
     * Plataforma avaliada ("android" ou "ios").
     */
    private String platform;

    /**
     * URL direta da respectiva loja (Google Play Store ou Apple App Store).
     */
    private String storeUrl;

    /**
     * Mensagem a ser exibida no diálogo institucional de atualização.
     */
    private String message;
}

