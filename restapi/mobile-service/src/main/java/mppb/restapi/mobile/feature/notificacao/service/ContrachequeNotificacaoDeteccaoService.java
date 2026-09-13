package mppb.restapi.mobile.feature.notificacao.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.extern.slf4j.Slf4j;
import mppb.restapi.mobile.feature.contracheque.dto.CompetenciaDto;
import mppb.restapi.mobile.feature.contracheque.repository.ContrachequeRepository;
import mppb.restapi.mobile.feature.notificacao.model.EventoFuncionalNotificacao;
import mppb.restapi.mobile.feature.notificacao.repository.ContrachequeNotificacaoControleRepository;

@Slf4j
@Service
public class ContrachequeNotificacaoDeteccaoService extends NotificacaoDominioDeteccaoSupport {

    private static final String ROTA_CONTRACHEQUE = "/contracheque";
    private static final String EVENTO_CONTRACHEQUE = "NOVO_CONTRACHEQUE";

    private final ContrachequeRepository contrachequeRepository;

    public ContrachequeNotificacaoDeteccaoService(
            ContrachequeNotificacaoControleRepository controleRepository,
            TransactionTemplate transactionTemplate,
            ContrachequeRepository contrachequeRepository) {
        super(controleRepository, transactionTemplate);
        this.contrachequeRepository = contrachequeRepository;
    }

    public int detectarNovosContracheques(String matricula) {
        return detectarDominio(matricula, eventosContracheque(matricula));
    }

    private List<EventoFuncionalNotificacao> eventosContracheque(String matricula) {
        Integer matriculaInt = parseInt(matricula);
        if (matriculaInt == null) {
            return List.of();
        }
        try {
            return contrachequeRepository.findCompetenciasByMatricula(matriculaInt).stream()
                    .map(competencia -> eventoContracheque(matricula, competencia))
                    .toList();
        } catch (RuntimeException e) {
            log.warn("Falha sanitizada ao detectar contracheques: {}", e.getClass().getSimpleName());
            return List.of();
        }
    }

    private EventoFuncionalNotificacao eventoContracheque(String matricula, CompetenciaDto competencia) {
        String mes = "%02d".formatted(competencia.getMes());
        String chave = "CONTRACHEQUE_%s_%d_%s".formatted(matricula, competencia.getAno(), mes);
        return new EventoFuncionalNotificacao(
                chave,
                matricula,
                EVENTO_CONTRACHEQUE,
                ROTA_CONTRACHEQUE,
                "Contracheque disponível",
                "O contracheque de %s/%d já está disponível.".formatted(mes, competencia.getAno()));
    }
}
