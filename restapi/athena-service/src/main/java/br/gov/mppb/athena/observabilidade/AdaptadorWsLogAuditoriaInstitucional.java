package br.gov.mppb.athena.observabilidade;

import java.util.Map;
import java.util.Objects;

public final class AdaptadorWsLogAuditoriaInstitucional {

    private final ConfiguracaoWsLogAuditoria configuracao;
    private final ClienteWsLogAuditoria cliente;

    public AdaptadorWsLogAuditoriaInstitucional(
            ConfiguracaoWsLogAuditoria configuracao,
            ClienteWsLogAuditoria cliente
    ) {
        this.configuracao = Objects.requireNonNull(configuracao, "configuracao WSLog deve ser informada.");
        this.cliente = Objects.requireNonNull(cliente, "cliente WSLog deve ser informado.");
    }

    public ResultadoEnvioWsLog emitir(EventoAuditoria evento) {
        Objects.requireNonNull(evento, "evento de auditoria institucional deve ser informado.");
        if (!configuracao.habilitada()) {
            return ResultadoEnvioWsLog.desabilitado();
        }

        cliente.enviar(configuracao.endpoint(), payload(evento));
        return ResultadoEnvioWsLog.enviadoComSucesso();
    }

    private static Map<String, Object> payload(EventoAuditoria evento) {
        return Map.of(
                "ator", evento.ator().valor(),
                "ocorridoEm", evento.ocorridoEm().toString(),
                "projeto", evento.projeto().valor(),
                "recurso", Map.of(
                        "tipo", evento.recursoAfetado().tipo(),
                        "identificador", evento.recursoAfetado().identificador()
                ),
                "acao", evento.acao().name(),
                "resultado", evento.resultado().name(),
                "detalhes", evento.detalhes(),
                "politicaRetencao", evento.politicaRetencao().name()
        );
    }
}
