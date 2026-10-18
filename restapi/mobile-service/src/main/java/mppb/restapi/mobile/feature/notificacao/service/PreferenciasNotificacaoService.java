package mppb.restapi.mobile.feature.notificacao.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import mppb.restapi.mobile.feature.notificacao.model.PreferenciasNotificacao;
import mppb.restapi.mobile.feature.notificacao.repository.PreferenciasNotificacaoRepository;

@Service
@RequiredArgsConstructor
public class PreferenciasNotificacaoService {

    private static final String NOVO_CONTRACHEQUE = "NOVO_CONTRACHEQUE";
    private static final String NOVO_PAGAMENTO = "NOVO_PAGAMENTO";

    private final PreferenciasNotificacaoRepository repository;

    public PreferenciasNotificacao buscar(String matricula) {
        return repository.buscar(matricula).orElseGet(PreferenciasNotificacao::todasHabilitadas);
    }

    public PreferenciasNotificacao salvar(String matricula, PreferenciasNotificacao preferencias) {
        return repository.salvar(matricula, preferencias);
    }

    public boolean permiteDesignacoes(String matricula) {
        return buscar(matricula).designacoes();
    }

    public boolean permiteEventoFuncional(String matricula, String tipoEvento) {
        PreferenciasNotificacao preferencias = buscar(matricula);
        if (NOVO_CONTRACHEQUE.equals(tipoEvento)) {
            return preferencias.contracheques();
        }
        if (NOVO_PAGAMENTO.equals(tipoEvento)) {
            return preferencias.pagamentos();
        }
        if (tipoEvento != null && tipoEvento.startsWith("AFASTAMENTO_")) {
            return preferencias.afastamentos();
        }
        return true;
    }
}
