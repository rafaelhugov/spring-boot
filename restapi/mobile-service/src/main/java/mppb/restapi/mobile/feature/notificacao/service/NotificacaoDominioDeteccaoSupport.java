package mppb.restapi.mobile.feature.notificacao.service;

import java.util.List;

import org.springframework.transaction.support.TransactionTemplate;

import mppb.restapi.mobile.feature.notificacao.model.EventoFuncionalNotificacao;
import mppb.restapi.mobile.feature.notificacao.repository.NotificacaoDominioControleRepository;

abstract class NotificacaoDominioDeteccaoSupport {

    private final NotificacaoDominioControleRepository controleRepository;
    private final TransactionTemplate transactionTemplate;

    protected NotificacaoDominioDeteccaoSupport(
            NotificacaoDominioControleRepository controleRepository,
            TransactionTemplate transactionTemplate) {
        this.controleRepository = controleRepository;
        this.transactionTemplate = transactionTemplate;
    }

    protected int detectarDominio(String matricula, List<EventoFuncionalNotificacao> eventos) {
        if (matricula == null || matricula.isBlank()) {
            return 0;
        }
        Integer inseridas = transactionTemplate.execute(status -> {
            controleRepository.criarControleSeAusente(matricula);
            Long controleId = controleRepository.lockControle(matricula).orElse(null);
            if (!controleRepository.isInicializado(matricula)) {
                controleRepository.registrarConhecidasIniciais(eventos);
                controleRepository.marcarInicializado(matricula);
                return 0;
            }

            int total = 0;
            for (EventoFuncionalNotificacao evento : eventos) {
                if (controleRepository.registrarNova(evento)) {
                    total++;
                }
            }
            if (controleId != null) {
                controleRepository.atualizarProcessamento(controleId);
            }
            return total;
        });
        return inseridas == null ? 0 : inseridas;
    }

    protected static Integer parseInt(String value) {
        try {
            return value == null ? null : Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected static String textoOuPadrao(String value, String padrao) {
        return value == null || value.isBlank() ? padrao : value.trim();
    }

    protected static String capitalizar(String value) {
        String text = textoOuPadrao(value, "Atualização");
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    protected static String slug(String value) {
        if (value == null) {
            return "NA";
        }
        String normalized = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return normalized.isBlank() ? "NA" : normalized;
    }
}
