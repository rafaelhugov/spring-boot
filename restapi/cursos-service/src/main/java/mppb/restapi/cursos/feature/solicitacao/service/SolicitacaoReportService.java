package mppb.restapi.cursos.feature.solicitacao.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.infrastructure.report.JasperService;
import mppb.restapi.common.core.util.IoUtil;
import mppb.restapi.cursos.feature.solicitacao.entity.SolicitacaoCurso;
import mppb.restapi.cursos.feature.solicitacao.entity.SolicitacaoAgenda;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@Slf4j
@Service
@RequiredArgsConstructor
public class SolicitacaoReportService {

        private final mppb.restapi.common.infrastructure.report.JasperService jasperService;
        private final mppb.restapi.cursos.feature.common.service.ServidorService servidorService;

        /**
         * Gera o PDF de requisição do curso preenchendo o template JasperReports.
         * 
         * @param solicitacao A solicitação salva no banco de dados.
         * @return O arquivo PDF em array de bytes.
         */
        public byte[] gerarPdfSolicitacao(SolicitacaoCurso solicitacao) {
                log.info("Gerando PDF para a solicitação de curso: {}", solicitacao.getNomeCurso());

                Map<String, Object> parametros = new HashMap<>();
                parametros.put("LOGO_MPPB",
                                mppb.restapi.common.core.util.IoUtil.getResource("classpath:reports/logo_mppb.jpg"));

                // 1. Identificação do Interessado
                String nomeInteressado = "ID: " + solicitacao.getIdServidorInteressado();
                try {
                        if (solicitacao.getIdServidorInteressado() != null) {
                                var servidor = servidorService.buscarServidorPorId(solicitacao.getIdServidorInteressado().intValue());
                                if (servidor.isPresent()) {
                                        nomeInteressado = servidor.get().getNome();
                                }
                        }
                } catch (Exception e) {
                        log.warn("Erro ao buscar nome do servidor {}: {}", solicitacao.getIdServidorInteressado(), e.getMessage());
                }
                parametros.put("NOME_INTERESSADO", nomeInteressado);

                // 2. Informações Gerais do Evento
                parametros.put("NOME_EVENTO", solicitacao.getNomeCurso());
                parametros.put("ESTIMATIVA_PARTICIPANTES", solicitacao.getEstimativaParticipantes());
                parametros.put("MODALIDADE",
                                solicitacao.getModalidade() != null ? solicitacao.getModalidade().getDescricao()
                                                : "N/A");
                String publicoAlvoStr = "N/A";
                if (solicitacao.getPublicosAlvo() != null && !solicitacao.getPublicosAlvo().isEmpty()) {
                        publicoAlvoStr = solicitacao.getPublicosAlvo().stream()
                                        .map(mppb.restapi.cursos.feature.lookup.entities.PublicoAlvo::getNome)
                                        .collect(java.util.stream.Collectors.joining(", "));
                }
                parametros.put("PUBLICO_ALVO", publicoAlvoStr);

                parametros.put("IDENTIFICA_ORGAO",
                                Boolean.TRUE.equals(solicitacao.getIdentificacaoOrgaoMembros()) ? "SIM" : "NÃO");
                parametros.put("ESPECIFICA_ORGAO",
                                solicitacao.getEspecificacaoOrgaoMembros() != null
                                                ? solicitacao.getEspecificacaoOrgaoMembros()
                                                : "");

                // 3. Localização
                String local = "Não informado";
                if (solicitacao.getLocalEvento() != null) {
                        local = solicitacao.getLocalEvento().getDescricao();
                } else if (solicitacao.getDescricaoLocalExterno() != null
                                && !solicitacao.getDescricaoLocalExterno().isBlank()) {
                        local = "Externo: " + solicitacao.getDescricaoLocalExterno();
                }
                parametros.put("LOCAL_EVENTO", local);

                // 4. Apoio Logístico — Only true items
                StringBuilder sbLogistica = new StringBuilder();
                if (Boolean.TRUE.equals(solicitacao.getCardDivulgacao())) sbLogistica.append("• Card Divulgação\n");
                if (Boolean.TRUE.equals(solicitacao.getComunicacaoImprensa())) sbLogistica.append("• Comunicação Imprensa\n");
                if (Boolean.TRUE.equals(solicitacao.getOficioConvite())) sbLogistica.append("• Ofício Convite\n");
                if (Boolean.TRUE.equals(solicitacao.getCoffeeBreak())) sbLogistica.append("• Coffee Break\n");
                if (Boolean.TRUE.equals(solicitacao.getKitParticipante())) sbLogistica.append("• Kit Participante\n");
                if (Boolean.TRUE.equals(solicitacao.getProgramacaoImpressa())) sbLogistica.append("• Programação Impressa\n");
                if (Boolean.TRUE.equals(solicitacao.getTrasladoMinistrante())) sbLogistica.append("• Traslado Ministrante\n");
                parametros.put("APOIO_LOGISTICO", sbLogistica.toString().trim());

                // 5. Financeiro — Only true items
                StringBuilder sbFinanceiro = new StringBuilder();
                if (Boolean.TRUE.equals(solicitacao.getPagamentoHonorario())) sbFinanceiro.append("• Pagamento Honorário\n");
                if (Boolean.TRUE.equals(solicitacao.getPassagemAerea())) sbFinanceiro.append("• Passagem Aérea\n");
                if (Boolean.TRUE.equals(solicitacao.getHospedagem())) sbFinanceiro.append("• Hospedagem\n");
                parametros.put("FINANCEIRO", sbFinanceiro.toString().trim());

                // 6. Observações
                parametros.put("OBSERVACOES_GERAIS",
                                solicitacao.getObservacoesGerais() != null ? solicitacao.getObservacoesGerais() : "");

                // 7. Agendas - grouped by date range, Brazilian date format
                DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                StringBuilder sbAgenda = new StringBuilder();
                if (solicitacao.getAgendas() != null) {
                        // Group intervals by "dataInicio a dataFim"
                        Map<String, List<SolicitacaoAgenda>> grouped = new LinkedHashMap<>();
                        for (SolicitacaoAgenda a : solicitacao.getAgendas()) {
                                String inicio = a.getDataInicio() != null ? a.getDataInicio().format(dtFmt) : "?";
                                String fim = a.getDataFim() != null ? a.getDataFim().format(dtFmt) : "?";
                                String key = inicio.equals(fim) ? inicio : inicio + " a " + fim;
                                grouped.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(a);
                        }
                        grouped.forEach((periodo, agendas) -> {
                                sbAgenda.append("• ").append(periodo).append(":\n");
                                agendas.forEach(a -> sbAgenda.append(String.format(
                                                "    - %s às %s\n",
                                                a.getHorarioInicio() != null ? a.getHorarioInicio().toString() : "?",
                                                a.getHorarioFim() != null ? a.getHorarioFim().toString() : "?")));
                        });
                }
                parametros.put("LISTA_AGENDAS", sbAgenda.toString().trim());

                // 8. Ministrantes — name/role on line 1, email on line 2, phone on line 3
                StringBuilder sbMin = new StringBuilder();
                if (solicitacao.getMinistrantes() != null) {
                        solicitacao.getMinistrantes().forEach(m -> {
                                String papel = m.getPapelMinistrante() != null ? m.getPapelMinistrante().getNome()
                                                : "N/A";
                                sbMin.append(String.format("• %s (%s)\n", m.getNomeMinistrante(), papel));
                                if (m.getEmail() != null && !m.getEmail().isBlank()) {
                                        sbMin.append(String.format("    E-mail: %s\n", m.getEmail()));
                                }
                                if (m.getTelefone() != null && !m.getTelefone().isBlank()) {
                                        sbMin.append(String.format("    Tel: %s\n", m.getTelefone()));
                                }
                                sbMin.append("\n");
                        });
                }
                parametros.put("LISTA_MINISTRANTES", sbMin.toString().trim());

                                // Preferir versão pré-compilada (.jasper) se disponível no classpath
                                String jasperName = "solicitacao_curso.jasper";
                                try {
                                        if (IoUtil.getResource("classpath:reports/" + jasperName) != null) {
                                                        log.info("Usando relatório pré-compilado: {}", jasperName);
                                                        // Relatório usa apenas parâmetros (title/labels), portanto usamos datasource vazio
                                                        return jasperService.gerarPdfEmptyDatasource(jasperName, parametros);
                                                }
                                } catch (Exception e) {
                                        log.warn("Não foi possível localizar o .jasper pré-compilado, retornando ao JRXML. {}", e.getMessage());
                                }

                                try {
                                        // JRXML de solicitação trabalha com parâmetros — usar JREmptyDataSource evita incongruências
                                        return jasperService.gerarPdfEmptyDatasource("solicitacao_curso.jrxml", parametros);
                                } catch (Exception e) {
                                        log.error("Falha ao compilar/gerar solicitacao_curso.jrxml, usando fallback report.jasper: {}", e.getMessage());
                                        return jasperService.gerarPdfEmptyDatasource("report.jasper", parametros);
                                }
        }
}
