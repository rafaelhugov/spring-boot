package mppb.restapi.mobile.feature.designacao.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.mobile.feature.designacao.model.DesignacaoPendenteNotificacao;
import mppb.restapi.mobile.feature.designacao.repository.DesignacaoControleRepository;
import mppb.restapi.mobile.feature.designacao.repository.DesignacaoViewRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class DesignacaoDeteccaoService {

    private final DesignacaoViewRepository viewRepository;
    private final DesignacaoControleRepository controleRepository;
    private final TransactionTemplate transactionTemplate;

    @Value("${designacoes.notificacao.batch-size:100}")
    private int batchSize;

    public int detectarNovasDesignacoes() {
        Instant inicio = Instant.now();
        List<DesignacaoPendenteNotificacao> novas = viewRepository.findNovasParaUsuariosInicializados(batchSize);
        int inseridas = 0;
        for (DesignacaoPendenteNotificacao pendente : novas) {
            if (registrarNovaEmTransacaoCurta(pendente)) {
                inseridas++;
            }
        }
        long millis = Duration.between(inicio, Instant.now()).toMillis();
        log.info("Scheduler de designacoes finalizado: candidatas={}, novas={}, tempoMs={}",
                novas.size(), inseridas, millis);
        return inseridas;
    }

    private boolean registrarNovaEmTransacaoCurta(DesignacaoPendenteNotificacao pendente) {
        Boolean inseriu = transactionTemplate.execute(status -> {
            boolean registrada = controleRepository.registrarNova(pendente.servidorMatricula(), pendente.designacao());
            controleRepository.atualizarProcessamento(pendente.controleId());
            return registrada;
        });
        return Boolean.TRUE.equals(inseriu);
    }
}
