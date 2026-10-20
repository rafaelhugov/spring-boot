package mppb.restapi.mobile.feature.designacao.service;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.mobile.feature.designacao.model.DesignacaoAtivaRow;
import mppb.restapi.mobile.feature.designacao.model.ResultadoNotificacaoDesignacao;
import mppb.restapi.mobile.feature.notificacao.model.DispositivoFcm;
import mppb.restapi.mobile.feature.notificacao.model.ResultadoEnvioFcm;
import mppb.restapi.mobile.feature.notificacao.model.StatusEnvioFcm;
import mppb.restapi.mobile.feature.notificacao.repository.DispositivoFcmRepository;
import mppb.restapi.mobile.feature.notificacao.service.FcmSenderService;

@Slf4j
@Service
@RequiredArgsConstructor
public class DesignacaoNotificacaoService {

    private final DispositivoFcmRepository dispositivoRepository;
    private final ObjectProvider<FcmSenderService> fcmSenderProvider;

    @Value("${designacoes.notificacao.fcm.enabled:false}")
    private boolean fcmEnabled;

    public ResultadoNotificacaoDesignacao notificarNovaDesignacao(DesignacaoAtivaRow designacao) {
        return notificarNovaDesignacaoInterno(designacao, true);
    }

    public ResultadoNotificacaoDesignacao notificarNovaDesignacao(DesignacaoAtivaRow designacao, boolean exibirPush) {
        return exibirPush ? notificarNovaDesignacao(designacao) : notificarNovaDesignacaoInterno(designacao, false);
    }

    private ResultadoNotificacaoDesignacao notificarNovaDesignacaoInterno(DesignacaoAtivaRow designacao, boolean exibirPush) {
        if (!fcmEnabled) {
            log.debug("Notificacao FCM de designacoes desabilitada por configuracao");
            return ResultadoNotificacaoDesignacao.desligado();
        }

        String matricula = designacao == null ? null : designacao.servidorMatricula();
        if (matricula == null || matricula.isBlank()) {
            log.info("Notificacao FCM de designacoes ignorada: sem destinatario autenticavel");
            return ResultadoNotificacaoDesignacao.semDispositivos();
        }

        List<DispositivoFcm> dispositivos = dispositivoRepository.findAtivosByMatricula(matricula.trim());
        if (dispositivos.isEmpty()) {
            log.info("Notificacao FCM de designacoes sem dispositivos ativos");
            return ResultadoNotificacaoDesignacao.semDispositivos();
        }

        FcmSenderService fcmSender = fcmSenderProvider.getIfAvailable();
        if (fcmSender == null) {
            log.warn("Notificacao FCM de designacoes sem FirebaseMessaging disponivel");
            return new ResultadoNotificacaoDesignacao(dispositivos.size(), 0, 0, 0, dispositivos.size(), 0, false);
        }

        ContadoresEnvio contadores = new ContadoresEnvio(dispositivos.size());
        for (DispositivoFcm dispositivo : dispositivos) {
            ResultadoEnvioFcm resultado = enviarParaDispositivo(fcmSender, dispositivo, designacao, exibirPush);
            contadores.registrar(resultado.status());
            if (resultado.status() == StatusEnvioFcm.TOKEN_INVALIDO) {
                desativarTokenInvalido(dispositivo);
            }
        }

        ResultadoNotificacaoDesignacao resultado = contadores.toResultado();
        log.info("Notificacao FCM de designacoes processada: dispositivos={}, aceitos={}, invalidos={}, transitorios={}, configuracao={}, desconhecidos={}",
                resultado.dispositivosAtivos(),
                resultado.aceitos(),
                resultado.tokensInvalidos(),
                resultado.falhasTransitorias(),
                resultado.falhasConfiguracao(),
                resultado.falhasDesconhecidas());
        return resultado;
    }

    private ResultadoEnvioFcm enviarParaDispositivo(
            FcmSenderService fcmSender,
            DispositivoFcm dispositivo,
            DesignacaoAtivaRow designacao, boolean exibirPush) {
        try {
            return fcmSender.enviarNovaDesignacao(dispositivo.tokenFcm(), designacao.id(), exibirPush);
        } catch (RuntimeException e) {
            return ResultadoEnvioFcm.falhaDesconhecida();
        }
    }

    private void desativarTokenInvalido(DispositivoFcm dispositivo) {
        try {
            dispositivoRepository.desativarPorId(dispositivo.id());
        } catch (RuntimeException e) {
            log.warn("Falha ao desativar dispositivo FCM invalido");
        }
    }

    private static class ContadoresEnvio {
        private final int dispositivosAtivos;
        private int aceitos;
        private int tokensInvalidos;
        private int falhasTransitorias;
        private int falhasConfiguracao;
        private int falhasDesconhecidas;

        private ContadoresEnvio(int dispositivosAtivos) {
            this.dispositivosAtivos = dispositivosAtivos;
        }

        private void registrar(StatusEnvioFcm status) {
            switch (status) {
                case ACEITO -> aceitos++;
                case TOKEN_INVALIDO -> tokensInvalidos++;
                case FALHA_TRANSITORIA -> falhasTransitorias++;
                case FALHA_CONFIGURACAO -> falhasConfiguracao++;
                case FALHA_DESCONHECIDA -> falhasDesconhecidas++;
            }
        }

        private ResultadoNotificacaoDesignacao toResultado() {
            return new ResultadoNotificacaoDesignacao(
                    dispositivosAtivos,
                    aceitos,
                    tokensInvalidos,
                    falhasTransitorias,
                    falhasConfiguracao,
                    falhasDesconhecidas,
                    false);
        }
    }
}
