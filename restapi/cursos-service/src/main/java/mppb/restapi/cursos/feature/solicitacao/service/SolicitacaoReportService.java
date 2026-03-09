package mppb.restapi.cursos.feature.solicitacao.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.infrastructure.report.JasperService;
import mppb.restapi.cursos.feature.solicitacao.entity.SolicitacaoCurso;
import mppb.restapi.cursos.feature.solicitacao.entity.SolicitacaoAgenda;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SolicitacaoReportService {

        private final JasperService jasperService;

        /**
         * Gera o PDF de requisição do curso preenchendo o template JasperReports.
         * 
         * @param solicitacao A solicitação salva no banco de dados.
         * @return O arquivo PDF em array de bytes.
         */
        public byte[] gerarPdfSolicitacao(SolicitacaoCurso solicitacao) {
                log.info("Gerando PDF para a solicitação de curso: {}", solicitacao.getNomeCurso());

                Map<String, Object> parametros = new HashMap<>();

                // 1. Identificação do Interessado
                parametros.put("NOME_INTERESSADO", "ID Servidor: " + solicitacao.getIdServidorInteressado());

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

                // 4. Apoio Logístico — [ ] unchecked / [X] checked
                parametros.put("CARD_DIVULGACAO", Boolean.TRUE.equals(solicitacao.getCardDivulgacao()) ? "[X]" : "[ ]");
                parametros.put("COMUNICACAO_IMPRENSA",
                                Boolean.TRUE.equals(solicitacao.getComunicacaoImprensa()) ? "[X]" : "[ ]");
                parametros.put("OFICIO_CONVITE", Boolean.TRUE.equals(solicitacao.getOficioConvite()) ? "[X]" : "[ ]");
                parametros.put("COFFEE_BREAK", Boolean.TRUE.equals(solicitacao.getCoffeeBreak()) ? "[X]" : "[ ]");
                parametros.put("KIT_PARTICIPANTE",
                                Boolean.TRUE.equals(solicitacao.getKitParticipante()) ? "[X]" : "[ ]");
                parametros.put("PROGRAMACAO_IMPRESSA",
                                Boolean.TRUE.equals(solicitacao.getProgramacaoImpressa()) ? "[X]" : "[ ]");
                parametros.put("TRASLADO_MINISTRANTE",
                                Boolean.TRUE.equals(solicitacao.getTrasladoMinistrante()) ? "[X]" : "[ ]");

                // 5. Financeiro — [ ] unchecked / [X] checked
                parametros.put("PAGAMENTO_HONORARIO",
                                Boolean.TRUE.equals(solicitacao.getPagamentoHonorario()) ? "[X]" : "[ ]");
                parametros.put("PASSAGEM_AEREA", Boolean.TRUE.equals(solicitacao.getPassagemAerea()) ? "[X]" : "[ ]");
                parametros.put("HOSPEDAGEM", Boolean.TRUE.equals(solicitacao.getHospedagem()) ? "[X]" : "[ ]");

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
                                sbAgenda.append(periodo).append(":\n");
                                agendas.forEach(a -> sbAgenda.append(String.format(
                                                "  - %s às %s\n",
                                                a.getHorarioInicio() != null ? a.getHorarioInicio().toString() : "?",
                                                a.getHorarioFim() != null ? a.getHorarioFim().toString() : "?")));
                        });
                }
                parametros.put("LISTA_AGENDAS", sbAgenda.toString());

                // 8. Ministrantes — name/role on line 1, email on line 2, phone on line 3
                StringBuilder sbMin = new StringBuilder();
                if (solicitacao.getMinistrantes() != null) {
                        solicitacao.getMinistrantes().forEach(m -> {
                                String papel = m.getPapelMinistrante() != null ? m.getPapelMinistrante().getNome()
                                                : "N/A";
                                sbMin.append(String.format("- %s  (%s)\n", m.getNomeMinistrante(), papel));
                                if (m.getEmail() != null && !m.getEmail().isBlank()) {
                                        sbMin.append(String.format("  E-mail: %s\n", m.getEmail()));
                                }
                                if (m.getTelefone() != null && !m.getTelefone().isBlank()) {
                                        sbMin.append(String.format("  Tel: %s\n", m.getTelefone()));
                                }
                                sbMin.append("\n");
                        });
                }
                parametros.put("LISTA_MINISTRANTES", sbMin.toString());

                return jasperService.gerarPdfEmptyDatasource("solicitacao_curso.jasper", parametros);
        }
}
