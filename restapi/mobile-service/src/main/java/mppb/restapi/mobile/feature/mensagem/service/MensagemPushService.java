package mppb.restapi.mobile.feature.mensagem.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

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

    private static final Set<String> PREPOSICOES = Set.of(
            "da", "de", "do", "das", "dos", "e", "em", "na", "no", "nas", "nos", "a", "o", "as", "os");

    private String resolverTituloPush(MensagemAviso mensagem, List<MensagemAnexo> anexos) {
        String remetente = formatarNomeProprio(mensagem.getRemetenteNome());
        if (!remetente.isEmpty()) {
            return remetente;
        }
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
        String tituloAviso = mensagem.getTitulo() != null ? mensagem.getTitulo().trim() : "";
        String conteudo = mensagem.getConteudo() != null ? mensagem.getConteudo().trim() : "";

        String corpoBase;
        if (!conteudo.isEmpty()) {
            corpoBase = conteudo;
        } else if (anexos != null && !anexos.isEmpty()) {
            CategoriaAnexo categoria = anexos.get(0).getCategoria();
            corpoBase = switch (categoria) {
                case AUDIO -> "Áudio enviado";
                case IMAGEM -> "Imagem enviada";
                case VIDEO -> "Vídeo enviado";
                case DOCUMENTO -> "Documento enviado";
            };
        } else {
            corpoBase = "Você recebeu um novo comunicado institucional.";
        }

        if (!tituloAviso.isEmpty() && !formatarNomeProprio(mensagem.getRemetenteNome()).isEmpty()) {
            return tituloAviso + ": " + corpoBase;
        }
        return corpoBase;
    }

    private static String formatarNomeProprio(String nome) {
        if (nome == null || nome.isBlank()) {
            return "";
        }
        String[] partes = nome.trim().toLowerCase(Locale.ROOT).split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < partes.length; i++) {
            String p = partes[i];
            if (p.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            if (i > 0 && PREPOSICOES.contains(p)) {
                sb.append(p);
            } else {
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
            }
        }
        return sb.toString();
    }
}

