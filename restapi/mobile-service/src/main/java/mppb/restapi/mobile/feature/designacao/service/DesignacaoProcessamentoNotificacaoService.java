package mppb.restapi.mobile.feature.designacao.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.mobile.feature.designacao.model.DesignacaoControlePendenteNotificacao;
import mppb.restapi.mobile.feature.designacao.model.ResultadoNotificacaoDesignacao;
import mppb.restapi.mobile.feature.designacao.model.ResultadoProcessamentoNotificacaoDesignacao;
import mppb.restapi.mobile.feature.designacao.repository.DesignacaoControleRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class DesignacaoProcessamentoNotificacaoService {

    private static final int LIMITE_MENSAGEM_ERRO = 500;

    private final DesignacaoControleRepository controleRepository;
    private final DesignacaoNotificacaoService notificacaoService;

    @Value("${designacoes.notificacao.batch-size:100}")
    private int batchSize;

    @Value("${designacoes.notificacao.max-tentativas:3}")
    private int maxTentativas;

    public List<ResultadoProcessamentoNotificacaoDesignacao> processarPendentes() {
        String identificadorReserva = UUID.randomUUID().toString();
        List<DesignacaoControlePendenteNotificacao> pendentes = controleRepository.reservarPendentesParaNotificacao(
                batchSize,
                maxTentativas,
                identificadorReserva);
        return pendentes.stream()
                .map(this::processar)
                .toList();
    }

    private ResultadoProcessamentoNotificacaoDesignacao processar(DesignacaoControlePendenteNotificacao pendente) {
        ResultadoNotificacaoDesignacao resultado = notificacaoService.notificarNovaDesignacao(pendente.designacao());
        if (resultado.algumEnvioAceito()) {
            controleRepository.marcarNotificacaoEnviada(pendente.id(), pendente.identificadorReserva());
            log.info("Controle de notificacao de designacao marcado como enviado: resultado=EN");
            return new ResultadoProcessamentoNotificacaoDesignacao(
                    pendente.id(),
                    pendente.statusNotificacao(),
                    "EN",
                    pendente.quantidadeTentativas(),
                    null,
                    resultado);
        }

        String mensagemErro = mensagemErroSanitizada(resultado);
        controleRepository.marcarNotificacaoErro(pendente.id(), pendente.identificadorReserva(), mensagemErro);
        log.info("Controle de notificacao de designacao marcado como erro: resultado=ER, motivo={}", mensagemErro);
        return new ResultadoProcessamentoNotificacaoDesignacao(
                pendente.id(),
                pendente.statusNotificacao(),
                "ER",
                pendente.quantidadeTentativas(),
                mensagemErro,
                resultado);
    }

    private static String mensagemErroSanitizada(ResultadoNotificacaoDesignacao resultado) {
        String mensagem;
        if (resultado.fcmDesligado()) {
            mensagem = "FCM_DESLIGADO";
        } else if (resultado.nenhumDispositivoAtivo()) {
            mensagem = "SEM_DISPOSITIVO_ATIVO";
        } else if (resultado.possuiFalhaConfiguracao()) {
            mensagem = "FALHA_CONFIGURACAO_FCM";
        } else if (resultado.possuiFalhaTransitoria()) {
            mensagem = "FALHA_TRANSITORIA_FCM";
        } else if (resultado.possuiFalhaDesconhecida()) {
            mensagem = "FALHA_DESCONHECIDA_FCM";
        } else if (resultado.todosTokensInvalidos()) {
            mensagem = "TOKENS_INVALIDOS";
        } else {
            mensagem = "NOTIFICACAO_NAO_ENVIADA";
        }
        return mensagem.length() > LIMITE_MENSAGEM_ERRO ? mensagem.substring(0, LIMITE_MENSAGEM_ERRO) : mensagem;
    }
}
