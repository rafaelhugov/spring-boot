package mppb.restapi.cursos.feature.common.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.util.StringUtil;
import mppb.restapi.common.feature.configuracao.service.ConfiguracaoService;
import mppb.restapi.common.feature.autorizacao.service.SistemaService;
import mppb.restapi.cursos.feature.inscricao.entity.Inscricao;
import mppb.restapi.cursos.feature.inscricao.entity.InscricaoConfirmacaoToken;
import mppb.restapi.cursos.feature.participante.dto.ParticipanteDto;
import mppb.restapi.cursos.feature.participante.service.ParticipanteService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException; // Importado
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final ConfiguracaoService configuracaoService;
    private final SistemaService sistemaService;
    private final ParticipanteService participanteService;

    private static final String NOME_REMETENTE_EMAIL = "CEAF MPPB";

    @Value("${sistema.sigla:CURSOS}")
    private String siglaSistema;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Transactional(readOnly = true)
    public void enviarEmailConfirmacao(Inscricao inscricao, InscricaoConfirmacaoToken token) {

        String emailFrom = configuracaoService.get("global.instituicao.email_ceaf");
        String frontendBaseUrl = sistemaService.findUrlBySigla(siglaSistema);
        Integer validadeMinutos = configuracaoService.getAsInteger("cursos.inscricao.token-validade-minutos");

        try {
            String emailDestinatario = inscricao.getParticipante().getEmail();
            String nomeDestinatario = inscricao.getParticipante().getNome();
            String tituloCurso = inscricao.getCurso().getTitulo();
            String tokenString = token.getToken();

            String urlConfirmacao = frontendBaseUrl + "/inscricao/confirmar-email/" + tokenString;

            // 1. Constrói apenas o *conteúdo* principal do e-mail
            String conteudoPrincipal = String.format("""
                <p>Recebemos a sua pré-inscrição para o curso: <strong>%s</strong>.</p>
                <p>Para confirmar a sua inscrição, por favor, clique no link abaixo. Este link é válido por %d minutos.</p>
                <p style="margin: 25px 0px;"><a href="%s" style="padding: 10px 15px; background-color: #a01d1e; color: #e6c376; text-decoration: none; border-radius: 5px; font-weight: bold;">
                    Confirmar Minha Inscrição
                </a></p>
                <p>Se você não solicitou esta inscrição, por favor, ignore este e-mail.</p>
                """,
                    tituloCurso,
                    validadeMinutos,
                    urlConfirmacao
            );

            // 2. Constrói o e-mail final usando o template
            String mensagemHtml = buildEmailTemplate(nomeDestinatario, conteudoPrincipal);

            // 3. Envia
            enviarEmail(emailFrom, emailDestinatario, "Confirme sua Inscrição: " + tituloCurso, mensagemHtml);
            log.info("E-mail de confirmação enviado para: {}", emailDestinatario);

        } catch (Exception e) {
            log.error("Erro inesperado ao preparar e-mail de confirmação para {}: {}", inscricao.getParticipante().getEmail(), e.getMessage());
        }
    }

    /**
     * Envia o e-mail de "Inscrição Realizada" (Boas-Vindas).
     */
    @Transactional(readOnly = true)
    public void enviarEmailInscricaoRealizada(Inscricao inscricao) {

        ParticipanteDto participante = participanteService.toEnrichedDto(inscricao.getParticipante());

        String emailDestinatario = participante.getEmail();
        String nomeDestinatario = participante.getNome();
        String tituloCurso = inscricao.getCurso().getTitulo();

        String dataRealizacao = formatarPeriodo(
                inscricao.getCurso().getRealizacaoDataInicial(),
                inscricao.getCurso().getRealizacaoDataFinal()
        );
        String cargaHoraria = StringUtil.formatCargaHoraria(inscricao.getCurso().getCargaHoraria());
        String linkProgramacao = Objects.toString(inscricao.getCurso().getLinkProgramacao(), "");

        String emailFrom = configuracaoService.get("global.instituicao.email_ceaf");

        try {
            // 1. Constrói apenas o *conteúdo* principal
            String conteudoPrincipal = String.format("""
                <p>Sua inscrição no curso <strong>%s</strong> foi realizada com sucesso.</p>
                <hr style="border: 0; border-top: 1px solid #eee;">
                <p><strong>Detalhes do Curso:</strong></p>
                <ul>
                    <li><strong>Período de Realização:</strong> %s</li>
                    <li><strong>Carga Horária:</strong> %s</li>
                    %s
                </ul>
                <p>Fique atento a futuros e-mails com mais informações, incluindo o link de acesso (caso seja on-line).</p>
                """,
                    tituloCurso,
                    dataRealizacao,
                    cargaHoraria,
                    linkProgramacao.isBlank() ? "" : String.format("<li><strong>Link da Programação:</strong> <a href=\"%1$s\">%1$s</a></li>", linkProgramacao)
            );

            // 2. Constrói o e-mail final usando o template
            String mensagemHtml = buildEmailTemplate(nomeDestinatario, conteudoPrincipal);

            // 3. Enviar
            enviarEmail(emailFrom, emailDestinatario, "Inscrição Confirmada: " + tituloCurso, mensagemHtml);
            log.info("E-mail de Inscrição Realizada enviado para: {}", emailDestinatario);

        } catch (Exception e) {
            log.error("Erro inesperado ao preparar e-mail de inscrição realizada para {}: {}", emailDestinatario, e.getMessage());
        }
    }

    /**
     * Constrói o "envelope" do e-mail (cabeçalho e rodapé)
     */
    private String buildEmailTemplate(String nomeDestinatario, String conteudoPrincipal) {
        String nome = (nomeDestinatario != null && !nomeDestinatario.isBlank()) ? nomeDestinatario : "Participante";

        String header = String.format("<p>Olá, %s!</p>", nome);

        String footer = """
            <br>
            <p>Atenciosamente,<br>CEAF/MPPB</p>
            """;

        return header + conteudoPrincipal + footer;
    }

    /**
     * Método auxiliar privado para envio de e-mail, centralizando a lógica.
     * CORRIGIDO: Adicionado try-catch para a UnsupportedEncodingException.
     */
    private void enviarEmail(String from, String to, String subject, String htmlBody) throws MessagingException {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            helper.setFrom(from, NOME_REMETENTE_EMAIL);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = Habilita HTML

            mailSender.send(mimeMessage);
        } catch (UnsupportedEncodingException e) {
            // Este erro é uma "checked exception" que nunca deve acontecer com "UTF-8".
            // Nós o capturamos e o relançamos como uma RuntimeException para evitar
            // poluir a assinatura de todos os métodos chamadores.
            log.error("Erro crítico de encoding (UTF-8) ao definir o remetente: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Método auxiliar para formatar datas (evita lógica no template string).
     */
    private String formatarPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null) {
            return "A definir";
        }
        return String.format("%s até %s", inicio.format(DATE_FORMATTER), fim.format(DATE_FORMATTER));
    }
}

