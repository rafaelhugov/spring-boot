package mppb.restapi.mobile.feature.mensagem.service;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.mobile.feature.mensagem.entity.MensagemAnexo;
import mppb.restapi.mobile.feature.mensagem.entity.MensagemAviso;
import mppb.restapi.mobile.feature.mensagem.enums.CategoriaAnexo;
import mppb.restapi.mobile.feature.notificacao.model.DispositivoFcm;
import mppb.restapi.mobile.feature.notificacao.repository.DispositivoFcmRepository;
import mppb.restapi.mobile.feature.notificacao.service.FcmSenderService;

@Service
@RequiredArgsConstructor
@Slf4j
public class MensagemPushService {

    private final DispositivoFcmRepository dispositivoFcmRepository;
    private final ObjectProvider<FcmSenderService> fcmSenderProvider;

    @Async
    public void notificarDestinatarios(
            MensagemAviso mensagem,
            List<MensagemAnexo> anexos,
            List<String> matriculasDestinatarias) {

        FcmSenderService fcmSender = fcmSenderProvider.getIfAvailable();
        if (fcmSender == null || matriculasDestinatarias == null || matriculasDestinatarias.isEmpty()) {
            return;
        }

        String titulo = resolverTituloPush(mensagem, anexos);
        String corpo = resolverCorpoPush(mensagem, anexos);
        String prioridade = mensagem.getPrioridade() != null ? mensagem.getPrioridade().name() : "NORMAL";

        for (String matricula : matriculasDestinatarias) {
            try {
                List<DispositivoFcm> dispositivos = dispositivoFcmRepository.findAtivosByMatricula(matricula);
                for (DispositivoFcm dispositivo : dispositivos) {
                    fcmSender.enviarAvisoInstitucional(
                            dispositivo.tokenFcm(),
                            mensagem.getId(),
                            titulo,
                            corpo,
                            prioridade);
                }
            } catch (RuntimeException e) {
                log.warn("Falha ao enviar push FCM da mensagem {} para matrícula {}", mensagem.getId(), matricula, e);
            }
        }
    }

    private String resolverTituloPush(MensagemAviso mensagem, List<MensagemAnexo> anexos) {
        if (mensagem.getTitulo() != null && !mensagem.getTitulo().isBlank()) {
            return mensagem.getTitulo().trim();
        }
        if (anexos != null && !anexos.isEmpty()) {
            CategoriaAnexo categoria = anexos.get(0).getCategoria();
            return switch (categoria) {
                case AUDIO -> "Novo aviso em áudio";
                case IMAGEM -> "Nova imagem institucional";
                case VIDEO -> "Novo vídeo institucional";
                case DOCUMENTO -> "Novo documento institucional";
            };
        }
        return "Novo aviso institucional";
    }

    private String resolverCorpoPush(MensagemAviso mensagem, List<MensagemAnexo> anexos) {
        if (mensagem.getConteudo() != null && !mensagem.getConteudo().isBlank()) {
            return mensagem.getConteudo().trim();
        }
        if (anexos != null && !anexos.isEmpty()) {
            return "Toque para visualizar o conteúdo enviado por " + mensagem.getRemetenteNome() + ".";
        }
        return "Você recebeu um novo comunicado institucional.";
    }
}

