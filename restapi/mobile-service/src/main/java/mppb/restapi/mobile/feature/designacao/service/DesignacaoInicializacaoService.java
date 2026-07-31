package mppb.restapi.mobile.feature.designacao.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.mobile.feature.designacao.model.DesignacaoAtivaRow;
import mppb.restapi.mobile.feature.designacao.repository.DesignacaoControleRepository;
import mppb.restapi.mobile.feature.designacao.repository.DesignacaoViewRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class DesignacaoInicializacaoService {

    private final DesignacaoControleRepository controleRepository;
    private final DesignacaoViewRepository viewRepository;

    @Transactional
    public void inicializarSeNecessario(String matricula) {
        controleRepository.criarControleSeAusente(matricula);
        controleRepository.lockControle(matricula);
        if (controleRepository.isInicializado(matricula)) {
            return;
        }

        List<DesignacaoAtivaRow> ativas = viewRepository.findAtivasByMatricula(matricula);
        controleRepository.registrarConhecidasIniciais(matricula, ativas);
        controleRepository.marcarInicializado(matricula);
        log.info("Designacoes inicializadas para matricula mascarada {}: {} registro(s)",
                mascarar(matricula), ativas.size());
    }

    private String mascarar(String matricula) {
        if (matricula == null || matricula.length() < 3) {
            return "***";
        }
        return "***" + matricula.substring(matricula.length() - 3);
    }
}
