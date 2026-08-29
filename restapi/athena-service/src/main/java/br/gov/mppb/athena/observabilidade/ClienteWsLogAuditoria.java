package br.gov.mppb.athena.observabilidade;

import java.net.URI;
import java.util.Map;

public interface ClienteWsLogAuditoria {

    void enviar(URI endpoint, Map<String, Object> evento);
}
