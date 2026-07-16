package mppb.restapi.cursos.feature.curso.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.error.BusinessRuleException;
import mppb.restapi.common.core.error.NotFoundException;
import mppb.restapi.cursos.feature.common.entities.Arquivo;
import mppb.restapi.cursos.feature.common.service.ArquivoService;
import mppb.restapi.cursos.feature.common.service.QrCodeService;
import mppb.restapi.cursos.feature.common.service.ServidorService;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.curso.repository.CursoMinistranteRepository;
import mppb.restapi.cursos.feature.curso.repository.CursoRepository;
import mppb.restapi.cursos.feature.inscricao.entity.Inscricao;
import mppb.restapi.cursos.feature.inscricao.repository.InscricaoRepository;
import mppb.restapi.cursos.feature.participante.entity.Participante;
import mppb.restapi.cursos.feature.participante.repository.ParticipanteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@Slf4j
public class CertificadoService {

    private static final java.util.Set<String> ACRONYMS = new java.util.HashSet<>(java.util.Arrays.asList(
            "CEAF", "MPPB", "GEDIR", "MP", "MPF", "CNMP", "EAD", "STF", "STJ"));

    private static boolean isAllUpperAlpha(String str) {
        if (str == null)
            return false;
        boolean hasLetter = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isLetter(c)) {
                hasLetter = true;
                if (!Character.isUpperCase(c))
                    return false;
            }
        }
        return hasLetter;
    }

    private static String capitalizeWord(String w, java.util.Locale loc) {
        if (w.isEmpty())
            return w;
        String first = w.substring(0, 1).toUpperCase(loc);
        String rest = w.substring(1).toLowerCase(loc);
        return first + rest;
    }

    private static String toTitleCasePt(String text) {
        if (text == null)
            return null;
        java.util.Locale loc = java.util.Locale.of("pt", "BR");
        String[] lowerWords = { "de", "da", "do", "das", "dos", "e", "a", "o", "as", "os", "em", "no", "na", "nos",
                "nas", "para", "por", "com", "ao", "à", "às", "aos", "dum", "duma", "dumas", "duns", "num", "numa",
                "nuns", "numas", "sob", "sobre", "se", "que", "um", "uma", "uns", "umas", "pelo", "pela", "pelos", "pelas" };
        java.util.Set<String> lowerSet = new java.util.HashSet<>();
        for (String lw : lowerWords)
            lowerSet.add(lw);
        StringBuilder out = new StringBuilder();
        String[] tokens = text.split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            String rebuilt = processHyphenToken(token, i == 0, loc, lowerSet);
            if (i > 0)
                out.append(' ');
            out.append(rebuilt);
        }
        return out.toString();
    }

    private static String processHyphenToken(String token, boolean isFirst, java.util.Locale loc,
            java.util.Set<String> lowerSet) {
        if (ACRONYMS.contains(token))
            return token; // manter siglas conhecidas
        if (token.matches("[IVXLCDM]+"))
            return token; // romano
        if (token.contains("-")) {
            String[] parts = token.split("-");
            StringBuilder b = new StringBuilder();
            for (int j = 0; j < parts.length; j++) {
                String part = parts[j];
                String lower = part.toLowerCase(loc);
                if (!isFirst && lowerSet.contains(lower))
                    b.append(lower);
                else
                    b.append(capitalizeWord(lower, loc));
                if (j < parts.length - 1)
                    b.append('-');
            }
            return b.toString();
        } else {
            String lower = token.toLowerCase(loc);
            if (!isFirst && lowerSet.contains(lower))
                return lower;
            return capitalizeWord(lower, loc);
        }
    }

    private static String normalizeCaseSmart(String text) {
        if (text == null)
            return null;
        if (isAllUpperAlpha(text)) {
            return toTitleCasePt(text);
        }
        return text; // mantém como veio se não for todo maiúsculo
    }

    private final TemplateEngine templateEngine;
    private final CursoRepository cursoRepository;
    private final ParticipanteRepository participanteRepository;
    private final ArquivoService arquivoService;
    private final ServidorService servidorService;
    private final InscricaoRepository inscricaoRepository;
    private final CursoMinistranteRepository cursoMinistranteRepository;

    private final QrCodeService qrCodeService;

    @Autowired
    public CertificadoService(
            TemplateEngine templateEngine,
            CursoRepository cursoRepository,
            ParticipanteRepository participanteRepository,
            ArquivoService arquivoService,
            ServidorService servidorService,
            InscricaoRepository inscricaoRepository,
            CursoMinistranteRepository cursoMinistranteRepository,
            QrCodeService qrCodeService) {
        this.templateEngine = templateEngine;
        this.cursoRepository = cursoRepository;
        this.participanteRepository = participanteRepository;
        this.arquivoService = arquivoService;
        this.servidorService = servidorService;
        this.inscricaoRepository = inscricaoRepository;
        this.cursoMinistranteRepository = cursoMinistranteRepository;
        this.qrCodeService = qrCodeService;
    }

    public byte[] gerarCertificadoPdf(long cursoId, long pessoaId) throws Exception {
        try {
            log.info("Iniciando geração de PDF para Curso: {}, Pessoa: {}", cursoId, pessoaId);

            Curso curso = cursoRepository.findById(cursoId)
                    .orElseThrow(() -> new NotFoundException("Curso não encontrado com o ID fornecido: " + cursoId));

            if (curso.getStatus() != mppb.restapi.cursos.feature.curso.enums.StatusCursoEnum.FINALIZADO) {
                throw new mppb.restapi.common.core.error.BusinessRuleException("Não é possível emitir certificados, pois o curso ainda não foi finalizado.");
            }

            String nome = "";
            String papel = "participante";

            // 1. Tentar encontrar como Participante (Inscrito no Curso)
            var inscricaoOpt = inscricaoRepository.findByCursoIdAndParticipanteId(cursoId, pessoaId);
            if (inscricaoOpt.isPresent()) {
                Participante p = inscricaoOpt.get().getParticipante();
                nome = p.getNome();
                // Resolver nome se for interno e estiver nulo
                if ((nome == null || nome.trim().isEmpty()) && p.getIdServidor() != null) {
                    nome = servidorService.buscarServidorPorId(p.getIdServidor())
                            .map(mppb.restapi.cursos.feature.common.projection.ServidorProjection::getNome)
                            .orElse("Servidor " + p.getIdServidor());
                }
                papel = "participante";
            } else {
                // 2. Se não for participante, tentar encontrar como Ministrante
                // Aqui tratamos o pessoaId como o ID do Ministrante (tabela curso.ministrante)
                var cursoMinistranteOpt = cursoMinistranteRepository.findByCursoId(cursoId).stream()
                        .filter(cm -> cm.getMinistrante().getId().equals(pessoaId))
                        .findFirst();

                if (cursoMinistranteOpt.isPresent()) {
                    var cm = cursoMinistranteOpt.get();
                    nome = cm.getMinistrante().getNome();
                    papel = cm.getPapel() != null ? cm.getPapel().getNome().toLowerCase() : "ministrante";
                } else {
                    // Fallback: Tenta buscar o participante pelo ID diretamente se não houver
                    // inscrição vinculada (emergência)
                    var partOpt = participanteRepository.findById(pessoaId);
                    if (partOpt.isPresent()) {
                        Participante p = partOpt.get();
                        nome = p.getNome();
                        papel = "participante";
                        if ((nome == null || nome.trim().isEmpty()) && p.getIdServidor() != null) {
                            nome = servidorService.buscarServidorPorId(p.getIdServidor())
                                    .map(mppb.restapi.cursos.feature.common.projection.ServidorProjection::getNome)
                                    .orElse("Servidor " + p.getIdServidor());
                        }
                    } else {
                        throw new NotFoundException("Titular do certificado não encontrado para o curso " + cursoId);
                    }
                }
            }

            if (nome == null || nome.isEmpty()) {
                nome = "Participante Não Identificado";
            }

            // Preparar variáveis para o template
            String imageHtml = "";
            if (curso.getCertificateTemplateId() != null) {
                try {
                    byte[] imageBytes = arquivoService.buscarPorId(curso.getCertificateTemplateId())
                            .map(mppb.restapi.cursos.feature.common.entities.Arquivo::getBytes)
                            .orElse(null);

                    if (imageBytes != null && imageBytes.length > 0) {
                        String base64Image = java.util.Base64.getEncoder().encodeToString(imageBytes);
                        imageHtml = "data:image/png;base64," + base64Image;
                    }
                } catch (Exception e) {
                    log.error("Erro ao carregar imagem de fundo do certificado: {}", e.getMessage());
                }
            }

            // Formatadores de Data
            Locale locale = Locale.of("pt", "BR");
            DateTimeFormatter extensoFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", locale);

            String dataExtenso = "";
            if (curso.getRealizacaoDataInicial() != null && curso.getRealizacaoDataFinal() != null) {
                if (curso.getRealizacaoDataInicial().toLocalDate()
                        .isEqual(curso.getRealizacaoDataFinal().toLocalDate())) {
                    dataExtenso = "no dia " + curso.getRealizacaoDataInicial().format(extensoFormatter);
                } else {
                    dataExtenso = "no período de "
                            + curso.getRealizacaoDataInicial()
                                    .format(DateTimeFormatter.ofPattern("d 'de' MMMM", locale))
                            + " a " + curso.getRealizacaoDataFinal().format(extensoFormatter);
                }
            } else if (curso.getRealizacaoDataInicial() != null) {
                dataExtenso = "no dia " + curso.getRealizacaoDataInicial().format(extensoFormatter);
            } else if (curso.getRealizacaoDataFinal() != null) {
                dataExtenso = "no dia " + curso.getRealizacaoDataFinal().format(extensoFormatter);
            }

            String dataHojeExtenso = java.time.LocalDate.now().format(extensoFormatter);

            // Modalidade
            String modalidadeTxt = "presencial";
            if (curso.getModalidade() != null) {
                Long modId = curso.getModalidade().getId();
                if (modId == 1L)
                    modalidadeTxt = "online";
                else if (modId == 3L)
                    modalidadeTxt = "híbrida";
            }

            // Carga Horária (Limpar 00:00:00 se necessário)
            String ch = curso.getCargaHoraria();
            if (ch != null && ch.contains(":")) {
                ch = ch.split(":")[0]; // Pega apenas as horas
            }

            Context context = new Context();
            // Aplicar normalização para evitar textos em "TUDO MAIÚSCULO" no certificado
            context.setVariable("nomeParticipante", normalizeCaseSmart(nome));
            context.setVariable("papel", papel);
            context.setVariable("tipoEvento",
                    curso.getTipoEvento() != null ? normalizeCaseSmart(curso.getTipoEvento().getDescricao()) : "Evento");
            context.setVariable("nomeCurso", normalizeCaseSmart(curso.getTitulo()));
            context.setVariable("descricaoCurso",
                    normalizeCaseSmart(curso.getDescricao() != null && !curso.getDescricao().trim().isEmpty() 
                            ? curso.getDescricao() : curso.getTitulo()));
            context.setVariable("entidadeTutora",
                    curso.getEntidadeTutora() != null ? curso.getEntidadeTutora().getNome() : "CEAF/MPPB");
            context.setVariable("dataExtenso", dataExtenso);
            context.setVariable("modalidade", modalidadeTxt);
            context.setVariable("cargaHoraria", ch != null ? ch : "0");
            context.setVariable("backgroundImage", imageHtml);
            context.setVariable("dataGeracaoExtenso", dataHojeExtenso);
            context.setVariable("cidade", "João Pessoa - PB");

            String htmlContent = templateEngine.process("certificado", context);
            log.debug("HTML do certificado gerado.");

            ByteArrayOutputStream target = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(htmlContent, "///");
            builder.toStream(target);
            builder.run();
            log.info("Certificado PDF gerado com sucesso para curso {} e pessoa {}", cursoId, pessoaId);
            return target.toByteArray();
        } catch (Exception e) {
            log.error("Erro fatal ao gerar certificado PDF (Curso: {}, Pessoa: {}): {}", cursoId, pessoaId,
                    e.getMessage());
            throw e;
        }
    }

    public byte[] gerarFlyerInscricaoPdf(Long cursoId, String baseUrl) throws Exception {
        log.info("Gerando flyer de inscrição para curso: {} com baseUrl: {}", cursoId, baseUrl);

        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new NotFoundException("Curso não encontrado"));

        if (curso.getHashInscricao() == null) {
            throw new BusinessRuleException("Este curso não possui um link de inscrição ativo.");
        }

        // Construir URL de inscrição (removendo barra final da baseUrl se existir)
        String cleanBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String inscricaoUrl = cleanBaseUrl + "/inscricao/" + curso.getHashInscricao();

        // Gerar QR Code
        String qrCodeBase64 = qrCodeService.generateQrCodeBase64(inscricaoUrl, 400, 400);

        Context context = new Context();
        context.setVariable("nomeCurso", curso.getTitulo());
        context.setVariable("descricaoCurso", curso.getDescricao() != null ? curso.getDescricao() : "");
        context.setVariable("qrCodeImage", qrCodeBase64);

        String htmlContent = templateEngine.process("qrcode-flyer", context);

        ByteArrayOutputStream target = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.withHtmlContent(htmlContent, "///");
        builder.toStream(target);
        builder.run();

        return target.toByteArray();
    }
}
