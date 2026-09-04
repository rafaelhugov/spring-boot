package mppb.restapi.mobile.feature.noticias.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.mobile.feature.noticias.model.NoticiaPendenteNotificacao;
import mppb.restapi.mobile.feature.noticias.model.ResultadoProcessamentoNotificacaoNoticia;
import mppb.restapi.mobile.feature.noticias.repository.NoticiaControleRepository;
import mppb.restapi.mobile.feature.notificacao.model.ResultadoEnvioFcm;
import mppb.restapi.mobile.feature.notificacao.model.StatusEnvioFcm;
import mppb.restapi.mobile.feature.notificacao.service.FcmSenderService;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticiaProcessamentoNotificacaoService {

    private static final int LIMITE_MENSAGEM_ERRO = 500;

    private final NoticiaControleRepository controleRepository;
    private final ObjectProvider<FcmSenderService> fcmSenderProvider;

    @Value("${noticias.notificacao.batch-size:50}")
    private int batchSize;

    @Value("${noticias.notificacao.max-tentativas:3}")
    private int maxTentativas;

    @Value("${noticias.notificacao.fcm.topic:mppb_noticias}")
    private String topic;

    public List<ResultadoProcessamentoNotificacaoNoticia> processarPendentes() {
        FcmSenderService sender = fcmSenderProvider.getIfAvailable();
        if (sender == null) {
            log.info("Processamento de noticias ignorado: FCM indisponivel");
            return List.of();
        }

        String identificadorReserva = UUID.randomUUID().toString();
        List<NoticiaPendenteNotificacao> pendentes = controleRepository.reservarPendentesParaNotificacao(
                batchSize,
                maxTentativas,
                identificadorReserva);
        return pendentes.stream()
                .map(pendente -> processar(sender, pendente))
                .toList();
    }

    private ResultadoProcessamentoNotificacaoNoticia processar(
            FcmSenderService sender,
            NoticiaPendenteNotificacao pendente) {
        ResultadoEnvioFcm resultado = sender.enviarNovaNoticiaTopico(topic);
        if (resultado.status() == StatusEnvioFcm.ACEITO) {
            controleRepository.marcarNotificacaoEnviada(pendente.id(), pendente.identificadorReserva());
            log.info("Controle de notificacao de noticia marcado como enviado: resultado=EN");
            return new ResultadoProcessamentoNotificacaoNoticia(
                    pendente.id(),
                    pendente.statusNotificacao(),
                    "EN",
                    pendente.quantidadeTentativas(),
                    null,
                    resultado);
        }

        String mensagemErro = mensagemErroSanitizada(resultado.status());
        controleRepository.marcarNotificacaoErro(pendente.id(), pendente.identificadorReserva(), mensagemErro);
        log.info("Controle de notificacao de noticia marcado como erro: resultado=ER, motivo={}", mensagemErro);
        return new ResultadoProcessamentoNotificacaoNoticia(
                pendente.id(),
                pendente.statusNotificacao(),
                "ER",
                pendente.quantidadeTentativas(),
                mensagemErro,
                resultado);
    }

    private static String mensagemErroSanitizada(StatusEnvioFcm status) {
        String mensagem = switch (status) {
            case TOKEN_INVALIDO -> "TOPICO_FCM_INVALIDO";
            case FALHA_CONFIGURACAO -> "FALHA_CONFIGURACAO_FCM";
            case FALHA_TRANSITORIA -> "FALHA_TRANSITORIA_FCM";
            case FALHA_DESCONHECIDA -> "FALHA_DESCONHECIDA_FCM";
            case ACEITO -> "NOTIFICACAO_ENVIADA";
        };
        return mensagem.length() > LIMITE_MENSAGEM_ERRO ? mensagem.substring(0, LIMITE_MENSAGEM_ERRO) : mensagem;
    }
}
