package mppb.restapi.cursos.feature.curso.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.error.BusinessRuleException;
import mppb.restapi.common.core.error.NotFoundException;
import mppb.restapi.cursos.feature.common.entities.Arquivo;
import mppb.restapi.cursos.feature.common.service.ArquivoService;
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

    private final TemplateEngine templateEngine;
    private final CursoRepository cursoRepository;
    private final ParticipanteRepository participanteRepository;
    private final ArquivoService arquivoService;
    private final ServidorService servidorService;
    private final InscricaoRepository inscricaoRepository;
    private final CursoMinistranteRepository cursoMinistranteRepository;

    @Autowired
    public CertificadoService(
            TemplateEngine templateEngine,
            CursoRepository cursoRepository,
            ParticipanteRepository participanteRepository,
            ArquivoService arquivoService,
            ServidorService servidorService,
            InscricaoRepository inscricaoRepository,
            CursoMinistranteRepository cursoMinistranteRepository) {
        this.templateEngine = templateEngine;
        this.cursoRepository = cursoRepository;
        this.participanteRepository = participanteRepository;
        this.arquivoService = arquivoService;
        this.servidorService = servidorService;
        this.inscricaoRepository = inscricaoRepository;
        this.cursoMinistranteRepository = cursoMinistranteRepository;
    }

    public byte[] gerarCertificadoPdf(Long cursoId, Long pessoaId) throws Exception {
        try {
            log.info("Iniciando geração de PDF para Curso: {}, Pessoa: {}", cursoId, pessoaId);

            Curso curso = cursoRepository.findById(cursoId)
                    .orElseThrow(() -> new NotFoundException("Curso não encontrado"));

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
                    // Fallback: Tenta buscar o participante pelo ID diretamente se não houver inscrição vinculada (emergência)
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
            Locale locale = new Locale("pt", "BR");
            DateTimeFormatter extensoFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", locale);

            String dataExtenso = curso.getRealizacaoDataFinal() != null 
                ? curso.getRealizacaoDataFinal().format(extensoFormatter) 
                : (curso.getRealizacaoDataInicial() != null ? curso.getRealizacaoDataInicial().format(extensoFormatter) : "");

            String dataHojeExtenso = java.time.LocalDate.now().format(extensoFormatter);

            // Modalidade
            String modalidadeTxt = "presencial";
            if (curso.getModalidade() != null) {
                Long modId = curso.getModalidade().getId();
                if (modId == 1L) modalidadeTxt = "online";
                else if (modId == 3L) modalidadeTxt = "híbrida";
            }

            // Carga Horária (Limpar 00:00:00 se necessário)
            String ch = curso.getCargaHoraria();
            if (ch != null && ch.contains(":")) {
                ch = ch.split(":")[0]; // Pega apenas as horas
            }

            Context context = new Context();
            context.setVariable("nomeParticipante", nome.toUpperCase());
            context.setVariable("papel", papel);
            context.setVariable("nomeCurso", curso.getTitulo());
            context.setVariable("entidadeTutora", curso.getEntidadeTutora() != null ? curso.getEntidadeTutora().getNome() : "CEAF/MPPB");
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
            log.error("Erro fatal ao gerar certificado PDF (Curso: {}, Pessoa: {}): {}", cursoId, pessoaId, e.getMessage());
            throw e;
        }
    }
}
