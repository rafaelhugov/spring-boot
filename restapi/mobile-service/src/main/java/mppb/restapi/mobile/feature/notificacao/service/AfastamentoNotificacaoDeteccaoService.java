package mppb.restapi.mobile.feature.notificacao.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.extern.slf4j.Slf4j;
import mppb.restapi.mobile.feature.concessaofolga.dto.ConcessaoFolgaDto;
import mppb.restapi.mobile.feature.concessaofolga.dto.SolicitacaoConcessaoDto;
import mppb.restapi.mobile.feature.concessaofolga.repository.ConcessaoFolgaRemoteRepository;
import mppb.restapi.mobile.feature.notificacao.model.EventoFuncionalNotificacao;
import mppb.restapi.mobile.feature.notificacao.repository.AfastamentoNotificacaoControleRepository;

@Slf4j
@Service
public class AfastamentoNotificacaoDeteccaoService extends NotificacaoDominioDeteccaoSupport {

    private static final String ROTA_AFASTAMENTOS = "/concessao-afastamento";
    private static final String EVENTO_AFASTAMENTO_STATUS = "AFASTAMENTO_STATUS_ALTERADO";
    private static final String EVENTO_AFASTAMENTO_MOVIMENTACAO = "AFASTAMENTO_MOVIMENTACAO";

    private final ConcessaoFolgaRemoteRepository concessaoFolgaRepository;

    public AfastamentoNotificacaoDeteccaoService(
            AfastamentoNotificacaoControleRepository controleRepository,
            TransactionTemplate transactionTemplate,
            ConcessaoFolgaRemoteRepository concessaoFolgaRepository) {
        super(controleRepository, transactionTemplate);
        this.concessaoFolgaRepository = concessaoFolgaRepository;
    }

    public int detectarNovosAfastamentos(String matricula) {
        return detectarDominio(matricula, eventosAfastamentos(matricula));
    }

    private List<EventoFuncionalNotificacao> eventosAfastamentos(String matricula) {
        try {
            List<EventoFuncionalNotificacao> eventos = new ArrayList<>();
            for (ConcessaoFolgaDto concessao : concessaoFolgaRepository.findConcessoesVigentes(matricula)) {
                for (SolicitacaoConcessaoDto solicitacao : concessao.getSolicitacoes()) {
                    EventoFuncionalNotificacao evento = eventoAfastamento(matricula, concessao, solicitacao);
                    if (evento != null) {
                        eventos.add(evento);
                    }
                }
            }
            return eventos;
        } catch (RuntimeException e) {
            log.warn("Falha sanitizada ao detectar afastamentos: {}", e.getClass().getSimpleName());
            return List.of();
        }
    }

    private EventoFuncionalNotificacao eventoAfastamento(
            String matricula,
            ConcessaoFolgaDto concessao,
            SolicitacaoConcessaoDto solicitacao) {
        if (solicitacao.getId() == null || solicitacao.getTipo() == null || solicitacao.getStatus() == null) {
            return null;
        }
        String tipo = solicitacao.getTipo();
        String status = solicitacao.getStatus();
        String tipoEvento = isMovimentacao(tipo) ? EVENTO_AFASTAMENTO_MOVIMENTACAO : EVENTO_AFASTAMENTO_STATUS;
        String chave = "AFASTAMENTO_%s_%d_%s_%s".formatted(matricula, solicitacao.getId(), slug(tipo), slug(status));
        String titulo = isMovimentacao(tipo)
                ? "%s %s".formatted(capitalizar(tipo), status.toLowerCase())
                : "Solicitação de afastamento %s".formatted(status.toLowerCase());
        String corpo = "%s: %s.".formatted(textoOuPadrao(concessao.getTipo(), "Afastamento"),
                textoOuPadrao(concessao.getDescricaoOriginal(), "solicitação atualizada"));
        Integer dias = solicitacao.getDiasInformados();
        if (isMovimentacao(tipo) && dias != null && dias > 0) {
            corpo = "%d dia(s) registrados para retorno/gozo oportuno.".formatted(dias);
        }
        return new EventoFuncionalNotificacao(
                chave,
                matricula,
                tipoEvento,
                ROTA_AFASTAMENTOS,
                titulo,
                corpo);
    }

    private static boolean isMovimentacao(String tipo) {
        String normalized = slug(tipo);
        return normalized.contains("INTERRUPCAO")
                || normalized.contains("SUSPENSAO")
                || normalized.contains("CANCELAMENTO")
                || normalized.contains("ADIAMENTO")
                || normalized.contains("ANTECIPACAO");
    }
}
