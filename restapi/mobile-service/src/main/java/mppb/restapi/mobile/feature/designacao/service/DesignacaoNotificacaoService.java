package mppb.restapi.mobile.feature.designacao.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.mobile.feature.designacao.mapper.DesignacaoMapper;
import mppb.restapi.mobile.feature.designacao.model.DesignacaoAtivaRow;

@Slf4j
@Service
@RequiredArgsConstructor
public class DesignacaoNotificacaoService {

    private final DesignacaoMapper mapper;

    @Value("${designacoes.notificacao.fcm.enabled:false}")
    private boolean fcmEnabled;

    public void notificarNovaDesignacao(DesignacaoAtivaRow designacao) {
        if (!fcmEnabled) {
            log.debug("Notificacao FCM de designacoes desabilitada por configuracao");
            return;
        }

        String titulo = titulo(designacao);
        String corpo = corpo(designacao);
        log.info("Notificacao de designacao pronta para envio: titulo='{}', route='/designacoes'", titulo);
        log.debug("Corpo sanitizado da notificacao de designacao: {}", corpo);
    }

    private String titulo(DesignacaoAtivaRow designacao) {
        String tipo = mapper.tipoDescricao(designacao);
        return switch (tipo) {
            case "Titularidade" -> "Nova titularidade";
            case "Substituição Cumulativa" -> "Nova substituição cumulativa";
            case "Função" -> "Nova função";
            default -> "Nova designação";
        };
    }

    private String corpo(DesignacaoAtivaRow designacao) {
        String tipo = mapper.tipoDescricao(designacao);
        String destino = firstNonBlank(designacao.orgaoExecucaoNome(), designacao.orgaoNome(), designacao.funcaoNome(), "Designação");
        if ("Titularidade".equals(tipo)) {
            return "Você foi designado como %s.".formatted(destino);
        }
        if ("Função".equals(tipo)) {
            return joinNonBlank(" — ", designacao.funcaoNome(), firstNonBlank(designacao.orgaoExecucaoNome(), designacao.orgaoNome()));
        }
        return destino;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String joinNonBlank(String separator, String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(separator);
            }
            builder.append(value.trim());
        }
        return builder.isEmpty() ? "Nova designação" : builder.toString();
    }
}
