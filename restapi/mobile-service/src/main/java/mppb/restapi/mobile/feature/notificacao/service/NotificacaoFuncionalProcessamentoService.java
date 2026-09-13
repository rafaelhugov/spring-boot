package mppb.restapi.mobile.feature.notificacao.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.mobile.feature.notificacao.model.EventoFuncionalNotificacao;
import mppb.restapi.mobile.feature.notificacao.model.EventoFuncionalPendenteNotificacao;
import mppb.restapi.mobile.feature.notificacao.model.ResultadoEnvioFcm;
import mppb.restapi.mobile.feature.notificacao.model.ResultadoProcessamentoNotificacaoFuncional;
import mppb.restapi.mobile.feature.notificacao.model.StatusEnvioFcm;
import mppb.restapi.mobile.feature.notificacao.model.DispositivoFcm;
import mppb.restapi.mobile.feature.notificacao.repository.DispositivoFcmRepository;
import mppb.restapi.mobile.feature.notificacao.repository.NotificacaoDominioControleRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacaoFuncionalProcessamentoService {

    private static final int LIMITE_MENSAGEM_ERRO = 500;

    private final List<NotificacaoDominioControleRepository> controleRepositories;
    private final DispositivoFcmRepository dispositivoRepository;
    private final ObjectProvider<FcmSenderService> fcmSenderProvider;

    @Value("${notificacoes-funcionais.batch-size:100}")
    private int batchSize;

    @Value("${notificacoes-funcionais.max-tentativas:3}")
    private int maxTentativas;

    public List<ResultadoProcessamentoNotificacaoFuncional> processarPendentes() {
        FcmSenderService sender = fcmSenderProvider.getIfAvailable();
        if (sender == null) {
            log.info("Processamento de notificacoes funcionais ignorado: FCM indisponivel");
            return List.of();
        }

        String identificadorReserva = UUID.randomUUID().toString();
        List<EventoFuncionalPendenteNotificacao> pendentes = controleRepositories.stream()
                .flatMap(repository -> repository.reservarPendentesParaNotificacao(
                        batchSize,
                        maxTentativas,
                        identificadorReserva).stream()
                        .map(pendente -> pendente.comControleRepository(repository)))
                .toList();
        return pendentes.stream()
                .map(pendente -> processar(sender, pendente))
                .toList();
    }

    private ResultadoProcessamentoNotificacaoFuncional processar(
            FcmSenderService sender,
            EventoFuncionalPendenteNotificacao pendente) {
        EventoFuncionalNotificacao evento = pendente.evento();
        List<DispositivoFcm> dispositivos = dispositivoRepository.findAtivosByMatricula(evento.matricula());
        if (dispositivos.isEmpty()) {
            pendente.controleRepository().marcarNotificacaoErro(
                    pendente.id(),
                    pendente.identificadorReserva(),
                    "SEM_DISPOSITIVO_ATIVO");
            return resultado(pendente, "ER", "SEM_DISPOSITIVO_ATIVO", ResultadoEnvioFcm.falhaDesconhecida());
        }

        int aceitos = 0;
        StatusEnvioFcm piorStatus = StatusEnvioFcm.FALHA_DESCONHECIDA;
        for (DispositivoFcm dispositivo : dispositivos) {
            ResultadoEnvioFcm envio = sender.enviarEventoFuncional(dispositivo.tokenFcm(), evento);
            if (envio.status() == StatusEnvioFcm.ACEITO) {
                aceitos++;
            } else {
                piorStatus = envio.status();
            }
            if (envio.status() == StatusEnvioFcm.TOKEN_INVALIDO) {
                dispositivoRepository.desativarPorId(dispositivo.id());
            }
        }

        if (aceitos > 0) {
            pendente.controleRepository().marcarNotificacaoEnviada(pendente.id(), pendente.identificadorReserva());
            return resultado(pendente, "EN", null, ResultadoEnvioFcm.aceito("MULTI_DEVICE"));
        }

        String mensagemErro = mensagemErroSanitizada(piorStatus);
        pendente.controleRepository().marcarNotificacaoErro(pendente.id(), pendente.identificadorReserva(), mensagemErro);
        return resultado(pendente, "ER", mensagemErro, ResultadoEnvioFcm.falhaDesconhecida());
    }

    private ResultadoProcessamentoNotificacaoFuncional resultado(
            EventoFuncionalPendenteNotificacao pendente,
            String statusFinal,
            String mensagemErro,
            ResultadoEnvioFcm resultadoEnvio) {
        return new ResultadoProcessamentoNotificacaoFuncional(
                pendente.id(),
                pendente.statusNotificacao(),
                statusFinal,
                pendente.quantidadeTentativas(),
                mensagemErro,
                resultadoEnvio);
    }

    private static String mensagemErroSanitizada(StatusEnvioFcm status) {
        String mensagem = switch (status) {
            case TOKEN_INVALIDO -> "TOKENS_INVALIDOS";
            case FALHA_CONFIGURACAO -> "FALHA_CONFIGURACAO_FCM";
            case FALHA_TRANSITORIA -> "FALHA_TRANSITORIA_FCM";
            case FALHA_DESCONHECIDA -> "FALHA_DESCONHECIDA_FCM";
            case ACEITO -> "NOTIFICACAO_ENVIADA";
        };
        return mensagem.length() > LIMITE_MENSAGEM_ERRO ? mensagem.substring(0, LIMITE_MENSAGEM_ERRO) : mensagem;
    }
}
