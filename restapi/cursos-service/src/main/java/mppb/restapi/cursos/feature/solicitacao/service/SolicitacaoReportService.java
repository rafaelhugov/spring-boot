package mppb.restapi.cursos.feature.solicitacao.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.infrastructure.report.JasperService;
import mppb.restapi.cursos.feature.solicitacao.entity.SolicitacaoCurso;
import org.springframework.stereotype.Service;

import java.util.HashMap;
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

                // 1. Identificação do Interessado (Idealmente viria de um serviço de Servidor)
                parametros.put("NOME_INTERESSADO", "ID Servidor: " + solicitacao.getIdServidorInteressado());

                // 2. Informações Gerais do Evento
                parametros.put("NOME_EVENTO", solicitacao.getNomeCurso());
                parametros.put("ESTIMATIVA_PARTICIPANTES", solicitacao.getEstimativaParticipantes());
                parametros.put("MODALIDADE",
                                solicitacao.getModalidade() != null ? solicitacao.getModalidade().getDescricao()
                                                : "N/A");
                parametros.put("PUBLICO_ALVO",
                                solicitacao.getPublicoAlvo() != null ? solicitacao.getPublicoAlvo().getNome() : "N/A");

                parametros.put("IDENTIFICA_ORGAO", solicitacao.getIdentificacaoOrgaoMembros() ? "SIM" : "NÃO");
                parametros.put("ESPECIFICA_ORGAO", solicitacao.getEspecificacaoOrgaoMembros());

                // 3. Localização
                String local = "Não informado";
                if (solicitacao.getLocalEvento() != null) {
                        local = solicitacao.getLocalEvento().getDescricao();
                } else if (solicitacao.getDescricaoLocalExterno() != null
                                && !solicitacao.getDescricaoLocalExterno().isBlank()) {
                        local = "Externo: " + solicitacao.getDescricaoLocalExterno();
                }
                parametros.put("LOCAL_EVENTO", local);

                // 4. Apoio Logístico (Booleans -> String checkbox representation or just text)
                parametros.put("CARD_DIVULGACAO", solicitacao.getCardDivulgacao() ? "✓" : "");
                parametros.put("COMUNICACAO_IMPRENSA", solicitacao.getComunicacaoImprensa() ? "✓" : "");
                parametros.put("OFICIO_CONVITE", solicitacao.getOficioConvite() ? "✓" : "");
                parametros.put("COFFEE_BREAK", solicitacao.getCoffeeBreak() ? "✓" : "");
                parametros.put("KIT_PARTICIPANTE", solicitacao.getKitParticipante() ? "✓" : "");
                parametros.put("PROGRAMACAO_IMPRESSA", solicitacao.getProgramacaoImpressa() ? "✓" : "");
                parametros.put("TRASLADO_MINISTRANTE", solicitacao.getTrasladoMinistrante() ? "✓" : "");

                // 5. Financeiro
                parametros.put("PAGAMENTO_HONORARIO", solicitacao.getPagamentoHonorario() ? "✓" : "");
                parametros.put("PASSAGEM_AEREA", solicitacao.getPassagemAerea() ? "✓" : "");
                parametros.put("HOSPEDAGEM", solicitacao.getHospedagem() ? "✓" : "");

                // 6. Observações
                parametros.put("OBSERVACOES_GERAIS", solicitacao.getObservacoesGerais());

                // 7. Agendas (Formatado como String para o JRXML simplificado)
                StringBuilder sbAgenda = new StringBuilder();
                if (solicitacao.getAgendas() != null) {
                        solicitacao.getAgendas().forEach(a -> sbAgenda.append(String.format("- %s de %s às %s\n",
                                        a.getDataEvento(), a.getHorarioInicio(), a.getHorarioFim())));
                }
                parametros.put("LISTA_AGENDAS", sbAgenda.toString());

                // 8. Ministrantes
                StringBuilder sbMin = new StringBuilder();
                if (solicitacao.getMinistrantes() != null) {
                        solicitacao.getMinistrantes().forEach(m -> sbMin.append(String.format(
                                        "• %s (%s) - Contato: %s\n",
                                        m.getNomeMinistrante(),
                                        m.getPapelMinistrante() != null ? m.getPapelMinistrante().getNome()
                                                        : "N/A",
                                        m.getContatoInfo())));
                }
                parametros.put("LISTA_MINISTRANTES", sbMin.toString());

                return jasperService.gerarPdfEmptyDatasource("solicitacao_curso.jrxml", parametros);
        }
}
