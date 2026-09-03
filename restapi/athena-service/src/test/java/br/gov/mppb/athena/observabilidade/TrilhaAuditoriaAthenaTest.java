package br.gov.mppb.athena.observabilidade;

import static org.assertj.core.api.Assertions.assertThat;

import br.gov.mppb.athena.dominio.IdentificadorProjeto;
import br.gov.mppb.athena.dominio.IdentificadorUsuario;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TrilhaAuditoriaAthenaTest {

    private final IdentificadorUsuario ator = new IdentificadorUsuario("usuario-auditor");
    private final IdentificadorProjeto projeto = new IdentificadorProjeto("ATHENA");

    @Test
    void deveDefinirEventosAuditaveisObrigatoriosComAtorDataProjetoRecursoAcaoEResultado() {
        assertThat(TrilhaAuditoriaAthena.acoesObrigatorias()).containsExactlyInAnyOrder(
                AcaoAuditavel.CRIACAO_PROJETO,
                AcaoAuditavel.ALTERACAO_PROJETO,
                AcaoAuditavel.INGESTAO_VERSAO_DOCUMENTAL,
                AcaoAuditavel.REGISTRO_AVALIACAO,
                AcaoAuditavel.REGISTRO_COMENTARIO,
                AcaoAuditavel.REGISTRO_PEDIDO_AJUSTE,
                AcaoAuditavel.TRATAMENTO_PEDIDO_AJUSTE,
                AcaoAuditavel.REGISTRO_CONFIRMACAO,
                AcaoAuditavel.MUDANCA_SITUACAO_PROJETO,
                AcaoAuditavel.ACESSO_ADMINISTRATIVO,
                AcaoAuditavel.ENVIO_RESULTADO_ESTRUTURADO
        );

        EventoAuditoria evento = eventoCom(Map.of("decisao", "APROVADO"));

        assertThat(evento)
                .extracting(EventoAuditoria::ator, EventoAuditoria::dataOcorrencia, EventoAuditoria::projeto,
                        EventoAuditoria::recursoAfetado, EventoAuditoria::acao, EventoAuditoria::resultado)
                .containsExactly(ator, OffsetDateTime.parse("2026-08-27T14:00:00-03:00"), projeto,
                        new RecursoAuditavel("item-documental", "ESP.REQ.009"),
                        AcaoAuditavel.REGISTRO_AVALIACAO, ResultadoEventoAuditavel.SUCESSO);
    }

    @Test
    void devePermitirReconstruirHistoricoSemExporCpfOuConteudoSensivelNosDetalhes() {
        EventoAuditoria evento = eventoCom(Map.of(
                "versao", "2",
                "decisao", "APROVADO",
                "cpf", "123.456.789-10",
                "observacao", "participante 12345678910 avaliou item",
                "conteudoOriginal", "texto integral do documento"
        ));

        assertThat(evento.detalhes())
                .containsEntry("versao", "2")
                .containsEntry("decisao", "APROVADO")
                .containsEntry("observacao", "participante <CPF_REMOVIDO> avaliou item")
                .doesNotContainKeys("cpf", "conteudoOriginal");
    }

    @Test
    void deveRepresentarRetencaoComoPendenciaExplicitaSemAssumirComportamentoDefinitivo() {
        EventoAuditoria evento = eventoCom(Map.of("justificativa", "registro necessario para rastreabilidade"));

        assertThat(evento.politicaRetencao())
                .isEqualTo(PoliticaRetencaoAuditavel.PENDENTE_DEFINICAO_INSTITUCIONAL);
    }

    @Test
    void deveEmitirEventosObrigatoriosPeloAdaptadorWsLogQuandoConfigurado() {
        ClienteWsLogEmMemoria cliente = new ClienteWsLogEmMemoria();
        AdaptadorWsLogAuditoriaInstitucional adaptador = new AdaptadorWsLogAuditoriaInstitucional(
                ConfiguracaoWsLogAuditoria.configurada(URI.create("https://wslog.institucional.example/auditoria")),
                cliente
        );

        TrilhaAuditoriaAthena.acoesObrigatorias().forEach(acao -> {
            EventoAuditoria evento = TrilhaAuditoriaAthena.registrar(
                    ator,
                    OffsetDateTime.parse("2026-08-27T14:00:00-03:00"),
                    projeto,
                    new RecursoAuditavel("projeto", "ATHENA"),
                    acao,
                    ResultadoEventoAuditavel.SUCESSO,
                    Map.of("correlacao", "TT.OBS.003")
            );

            assertThat(adaptador.emitir(evento).enviado()).isTrue();
        });

        assertThat(cliente.payloads())
                .hasSize(TrilhaAuditoriaAthena.acoesObrigatorias().size())
                .extracting(payload -> payload.get("acao"))
                .contains(
                        "REGISTRO_PEDIDO_AJUSTE",
                        "TRATAMENTO_PEDIDO_AJUSTE",
                        "REGISTRO_CONFIRMACAO",
                        "INGESTAO_VERSAO_DOCUMENTAL",
                        "REGISTRO_AVALIACAO",
                        "REGISTRO_COMENTARIO",
                        "ACESSO_ADMINISTRATIVO"
                );
        assertThat(cliente.endpoints()).containsOnly(URI.create("https://wslog.institucional.example/auditoria"));
    }

    @Test
    void deveManterEndpointWsLogConfiguravelSemCodificarHomologacaoOuExigirDisponibilidade() {
        ClienteWsLogEmMemoria cliente = new ClienteWsLogEmMemoria();
        AdaptadorWsLogAuditoriaInstitucional adaptadorDesabilitado = new AdaptadorWsLogAuditoriaInstitucional(
                ConfiguracaoWsLogAuditoria.desabilitada(),
                cliente
        );

        ResultadoEnvioWsLog resultado = adaptadorDesabilitado.emitir(eventoCom(Map.of("origem", "teste")));

        assertThat(resultado.enviado()).isFalse();
        assertThat(resultado.motivo()).isEqualTo("WSLOG_NAO_CONFIGURADO");
        assertThat(cliente.payloads()).isEmpty();
        assertThat(ConfiguracaoWsLogAuditoria.configurada(URI.create("https://ambiente-parametrizado.example/wslog"))
                .endpoint())
                .hasToString("https://ambiente-parametrizado.example/wslog");
    }

    @Test
    void deveSepararAuditoriaInstitucionalWsLogDeLogsTecnicosMetricasEExceptions() {
        ClienteWsLogEmMemoria cliente = new ClienteWsLogEmMemoria();
        AdaptadorWsLogAuditoriaInstitucional adaptador = new AdaptadorWsLogAuditoriaInstitucional(
                ConfiguracaoWsLogAuditoria.configurada(URI.create("https://wslog.institucional.example/auditoria")),
                cliente
        );

        adaptador.emitir(eventoCom(Map.of(
                "observacao", "participante 12345678910 avaliou item",
                "tokenTecnico", "nao deve sair"
        )));

        assertThat(cliente.payloads()).singleElement().satisfies(payload -> {
            assertThat(payload).containsKeys("ator", "dataOcorrencia", "projeto", "recurso", "acao", "resultado",
                    "detalhes", "politicaRetencao");
            assertThat(payload).doesNotContainKeys("logger", "metricas", "exception", "stackTrace", "httpStatus");
            @SuppressWarnings("unchecked")
            Map<String, String> detalhes = (Map<String, String>) payload.get("detalhes");
            assertThat(detalhes)
                    .containsEntry("observacao", "participante <CPF_REMOVIDO> avaliou item")
                    .doesNotContainKey("tokenTecnico");
        });
    }

    private EventoAuditoria eventoCom(Map<String, String> detalhes) {
        return TrilhaAuditoriaAthena.registrar(
                ator,
                OffsetDateTime.parse("2026-08-27T14:00:00-03:00"),
                projeto,
                new RecursoAuditavel("item-documental", "ESP.REQ.009"),
                AcaoAuditavel.REGISTRO_AVALIACAO,
                ResultadoEventoAuditavel.SUCESSO,
                detalhes
        );
    }

    private static final class ClienteWsLogEmMemoria implements ClienteWsLogAuditoria {

        private final List<URI> endpoints = new ArrayList<>();
        private final List<Map<String, Object>> payloads = new ArrayList<>();

        @Override
        public void enviar(URI endpoint, Map<String, Object> evento) {
            endpoints.add(endpoint);
            payloads.add(evento);
        }

        List<URI> endpoints() {
            return List.copyOf(endpoints);
        }

        List<Map<String, Object>> payloads() {
            return List.copyOf(payloads);
        }
    }
}
