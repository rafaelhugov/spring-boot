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
import org.springframework.web.util.HtmlUtils;

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

        // TODO colocar essa variavel como constante
        String emailFrom = configuracaoService.get("global.instituicao.email_ceaf");
        String frontendBaseUrl = sistemaService.findUrlBySigla(siglaSistema);
        Integer validadeMinutos = configuracaoService.getAsInteger("cursos.inscricao.token-validade-minutos");

        try {
            String emailDestinatario = inscricao.getParticipante().getEmail();
            String nomeDestinatario = inscricao.getParticipante().getNome();
            String tituloCurso = inscricao.getCurso().getTitulo();
            String tokenString = token.getToken();
            String criteriosEspecificos = formatarCriteriosEspecificos(
                    inscricao.getCriteriosPublicoAlvoDeclarados());

            String urlConfirmacao = frontendBaseUrl + "/inscricao/confirmar-email/" + tokenString;

            // 1. Constrói apenas o *conteúdo* principal do e-mail
            String conteudoPrincipal = String.format(
                    "<p>Recebemos a sua pré-inscrição para o curso: <strong>%s</strong>.</p>\n" +
                            "<p>Para confirmar a sua inscrição, por favor, clique no link abaixo. Este link é válido por %d minutos.</p>\n"
                            +
                            "<p style=\"margin: 25px 0px;\"><a href=\"%s\" style=\"padding: 10px 15px; background-color: #a01d1e; color: #e6c376; text-decoration: none; border-radius: 5px; font-weight: bold;\">\n"
                            +
                            "    Confirmar Minha Inscrição\n" +
                            "</a></p>\n" +
                            "%s" +
                            "<p>Se você não solicitou esta inscrição, por favor, ignore este e-mail.</p>",
                    escapar(tituloCurso),
                    validadeMinutos,
                    escapar(urlConfirmacao),
                    criteriosEspecificos);

            // 2. Constrói o e-mail final usando o template
            String mensagemHtml = buildEmailTemplate(nomeDestinatario, conteudoPrincipal);

            // 3. Envia
            enviarEmail(emailFrom, emailDestinatario, "Confirme sua Inscrição: " + tituloCurso, mensagemHtml);
            log.info("E-mail de confirmação enviado para: {}", emailDestinatario);

        } catch (Exception e) {
            log.error("Erro inesperado ao preparar e-mail de confirmação para {}: {}",
                    inscricao.getParticipante().getEmail(), e.getMessage());
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
        String criteriosEspecificos = formatarCriteriosEspecificos(
                inscricao.getCriteriosPublicoAlvoDeclarados());

        String dataRealizacao = formatarPeriodo(
                inscricao.getCurso().getRealizacaoDataInicial(),
                inscricao.getCurso().getRealizacaoDataFinal());
        String cargaHoraria = StringUtil.formatCargaHoraria(inscricao.getCurso().getCargaHoraria());
        String linkProgramacao = Objects.toString(inscricao.getCurso().getLinkProgramacao(), "");

        String emailFrom = configuracaoService.get("global.instituicao.email_ceaf");

        try {
            // 1. Constrói apenas o *conteúdo* principal
            String conteudoPrincipal = String.format(
                    "<p>Sua inscrição no curso <strong>%s</strong> foi realizada com sucesso.</p>\n" +
                            "<hr style=\"border: 0; border-top: 1px solid #eee;\">\n" +
                            "<p><strong>Detalhes do Curso:</strong></p>\n" +
                            "<ul>\n" +
                            "    <li><strong>Período de Realização:</strong> %s</li>\n" +
                            "    <li><strong>Carga Horária:</strong> %s</li>\n" +
                            "    %s\n" +
                            "</ul>\n" +
                            "%s" +
                            "<p>Enviaremos e-mail em caso de novas informações ou mudanças na programação.</p>",
                    escapar(tituloCurso),
                    dataRealizacao,
                    cargaHoraria,
                    linkProgramacao.isBlank() ? ""
                            : String.format("<li><strong>Link da videochamada:</strong> <a href=\"%1$s\">%1$s</a></li>",
                                    escapar(linkProgramacao)),
                    criteriosEspecificos);

            // 2. Constrói o e-mail final usando o template
            String mensagemHtml = buildEmailTemplate(nomeDestinatario, conteudoPrincipal);

            // 3. Enviar
            enviarEmail(emailFrom, emailDestinatario, "Inscrição Confirmada: " + tituloCurso, mensagemHtml);
            log.info("E-mail de Inscrição Realizada enviado para: {}", emailDestinatario);

        } catch (Exception e) {
            log.error("Erro inesperado ao preparar e-mail de inscrição realizada para {}: {}", emailDestinatario,
                    e.getMessage());
        }
    }

    /**
     * Envia o e-mail contendo o código de confirmação (OTP) para registro de frequência.
     */
    @Transactional(readOnly = true)
    public void enviarEmailOtpFrequencia(String emailDestinatario, String nomeDestinatario, String codigoOtp, String tituloCurso) {
        String emailFrom = configuracaoService.get("global.instituicao.email_ceaf");

        try {
            // 1. Constrói o conteúdo principal do OTP
            String conteudoPrincipal = String.format(
                    "<p>Você solicitou o registro de presença para o curso: <strong>%s</strong>.</p>\n" +
                            "<p>Para confirmar a sua presença, informe o código de 6 dígitos abaixo na tela do sistema:</p>\n" +
                            "<div style=\"text-align: center; margin: 30px 0;\">\n" +
                            "    <span style=\"padding: 15px 25px; background-color: #f5f5f5; border: 2px dashed #a01d1e; color: #a01d1e; font-size: 24px; font-weight: bold; letter-spacing: 5px; border-radius: 5px;\">\n" +
                            "        %s\n" +
                            "    </span>\n" +
                            "</div>\n" +
                            "<p><em>Este código é válido por 5 minutos.</em></p>\n" +
                            "<p>Se você não está tentando registrar presença neste curso, por favor, ignore este e-mail.</p>",
                    tituloCurso,
                    codigoOtp);

            // 2. Constrói o e-mail final usando o template
            String mensagemHtml = buildEmailTemplate(nomeDestinatario, conteudoPrincipal);

            // 3. Enviar
            enviarEmail(emailFrom, emailDestinatario, "Código de Presença: " + tituloCurso, mensagemHtml);
            log.info("E-mail com OTP enviado para: {}", emailDestinatario);

        } catch (Exception e) {
            log.error("Erro inesperado ao preparar e-mail de OTP para {}: {}", emailDestinatario, e.getMessage());
        }
    }

    /**
     * Constrói o "envelope" do e-mail (cabeçalho e rodapé)
     */
    private String buildEmailTemplate(String nomeDestinatario, String conteudoPrincipal) {
        String nome = (nomeDestinatario != null && !nomeDestinatario.isBlank()) ? nomeDestinatario : "Participante";

        String header = String.format("<p>Olá, %s!</p>", escapar(nome));

        String footer = "<br>\n<p>Atenciosamente,<br>Equipe CEAF/MPPB</p>\n";

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

    private String formatarCriteriosEspecificos(String criterios) {
        if (criterios == null || criterios.isBlank()) {
            return "";
        }

        String textoSeguro = escapar(criterios.trim())
                .replace("\r\n", "<br>")
                .replace("\n", "<br>")
                .replace("\r", "<br>");
        return "<div style=\"margin: 18px 0; padding: 14px; border-left: 4px solid #a01d1e; background-color: #fff7f0;\">"
                + "<p><strong>Critérios específicos de participação:</strong></p>"
                + "<p>" + textoSeguro + "</p>"
                + "<p><em>Você declarou ciência e atendimento a esses critérios no momento da inscrição.</em></p>"
                + "</div>";
    }

    private String escapar(String valor) {
        return HtmlUtils.htmlEscape(Objects.toString(valor, ""));
    }
}
