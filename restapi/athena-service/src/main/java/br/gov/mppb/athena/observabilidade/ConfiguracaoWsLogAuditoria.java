package br.gov.mppb.athena.observabilidade;

import java.net.URI;

public record ConfiguracaoWsLogAuditoria(URI endpoint) {

    public ConfiguracaoWsLogAuditoria {
        if (endpoint != null) {
            validarEndpoint(endpoint);
        }
    }

    public static ConfiguracaoWsLogAuditoria desabilitada() {
        return new ConfiguracaoWsLogAuditoria(null);
    }

    public static ConfiguracaoWsLogAuditoria configurada(URI endpoint) {
        return new ConfiguracaoWsLogAuditoria(endpoint);
    }

    public boolean habilitada() {
        return endpoint != null;
    }

    private static void validarEndpoint(URI endpoint) {
        String esquema = endpoint.getScheme();
        if (!"http".equalsIgnoreCase(esquema) && !"https".equalsIgnoreCase(esquema)) {
            throw new IllegalArgumentException("endpoint WSLog deve usar HTTP ou HTTPS.");
        }
        if (endpoint.getHost() == null || endpoint.getHost().isBlank()) {
            throw new IllegalArgumentException("endpoint WSLog deve informar host.");
        }
    }
}
