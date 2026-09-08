package br.gov.mppb.athena.api;

import br.gov.mppb.athena.seguranca.OperacaoProjeto;
import java.util.List;
import java.util.Set;

public final class ContratosRestAthena {

    private static final List<ErroFuncionalApi> ERROS_PADRAO = List.of(
            new ErroFuncionalApi(400, "REQUISICAO_INVALIDA", "entrada obrigatoria ausente ou invalida"),
            new ErroFuncionalApi(401, "IDENTIDADE_INSTITUCIONAL_AUSENTE", "identidade institucional autenticada nao informada"),
            new ErroFuncionalApi(403, "OPERACAO_NAO_AUTORIZADA", "usuario sem vinculo, papel ou permissao especifica para o projeto"),
            new ErroFuncionalApi(404, "RECURSO_NAO_ENCONTRADO", "projeto, documento ou item inexistente no contexto autorizado"),
            new ErroFuncionalApi(422, "REGRA_FUNCIONAL_VIOLADA", "operacao viola contrato funcional governado")
    );

    private static final Set<String> FONTES = Set.of(
            "BL.AC.001",
            "BL.AC.002",
            "BL.AC.003",
            "BL.AC.006",
            "ESP.REQ.007",
            "ESP.REQ.010",
            "ESP.RN.012",
            "ESP.RN.014",
            "ESP.DADO.007",
            "ESP.ACEITE.011",
            "ESP.ACEITE.012",
            "SOL.API.001",
            "SOL.SEGURANCA.001"
    );

    private static final List<ContratoRestAthena> CONTRATOS = List.of(
            contrato(GrupoOperacaoApi.PROJETO, MetodoHttp.POST, "/api/projetos", OperacaoProjeto.ADMINISTRAR,
                    Set.of("nome", "descricao", "unidades", "participantes"),
                    Set.of("identificador", "nome", "descricao", "unidades", "participantes", "situacao", "dataCriacao", "usuarioCriacao")),
            contrato(GrupoOperacaoApi.PROJETO, MetodoHttp.PUT, "/api/projetos/{idProjeto}", OperacaoProjeto.ADMINISTRAR,
                    Set.of("idProjeto", "nome", "descricao", "unidades", "participantes", "situacao"),
                    Set.of("identificador", "nome", "descricao", "unidades", "participantes", "situacao", "dataCriacao", "usuarioCriacao")),
            contrato(GrupoOperacaoApi.PROJETO, MetodoHttp.GET, "/api/projetos/{idProjeto}", OperacaoProjeto.CONSULTAR,
                    Set.of("idProjeto"),
                    Set.of("identificador", "nome", "descricao", "unidades", "participantes", "situacao", "dataCriacao", "usuarioCriacao")),
            contrato(GrupoOperacaoApi.DOCUMENTO, MetodoHttp.GET, "/api/projetos/{idProjeto}/documentos", OperacaoProjeto.CONSULTAR,
                    Set.of("idProjeto", "grupo", "categoria"),
                    Set.of("documentos", "grupo", "categoria", "finalidade", "apresentacao", "relacoesRecebidas")),
            contrato(GrupoOperacaoApi.DOCUMENTO, MetodoHttp.GET, "/api/projetos/{idProjeto}/documentos/{idDocumento}", OperacaoProjeto.CONSULTAR,
                    Set.of("idProjeto", "idDocumento"),
                    Set.of("identificador", "titulo", "metadados", "versoes", "relacoesRecebidas")),
            contrato(GrupoOperacaoApi.DOCUMENTO, MetodoHttp.POST, "/api/projetos/{idProjeto}/documentos/ingestoes", OperacaoProjeto.ADMINISTRAR,
                    Set.of("idProjeto", "identificadorDocumento", "tituloDocumento", "formato", "conteudoOriginal", "representacaoInterna"),
                    Set.of("documento", "versao", "formato", "itens", "relacoesRecebidas", "dependenciaExternaCorporativa")),
            contrato(GrupoOperacaoApi.DOCUMENTO, MetodoHttp.POST, "/api/projetos/{idProjeto}/documentos/{idDocumento}/versoes", OperacaoProjeto.ADMINISTRAR,
                    Set.of("idProjeto", "idDocumento", "versaoAnterior", "formato", "conteudoOriginal", "representacaoInterna", "pedidoAjusteId"),
                    Set.of("documento", "versao", "versaoAnterior", "idVersaoAnterior", "idPedidoAjusteOrigem", "itens", "relacoesRecebidas")),
            contrato(GrupoOperacaoApi.ITEM_DOCUMENTAL, MetodoHttp.GET, "/api/projetos/{idProjeto}/documentos/{idDocumento}/itens/{idItem}", OperacaoProjeto.CONSULTAR,
                    Set.of("idProjeto", "idDocumento", "idItem"),
                    Set.of("identificador", "titulo", "conteudo", "rastreabilidade", "relacoesRecebidas")),
            contrato(GrupoOperacaoApi.AVALIACAO, MetodoHttp.POST, "/api/projetos/{idProjeto}/documentos/{idDocumento}/avaliacoes", OperacaoProjeto.AVALIAR,
                    Set.of("idProjeto", "idDocumento", "versao", "idItem", "decisao", "justificativa"),
                    Set.of("referencia", "responsavel", "data", "decisao", "justificativa")),
            contrato(GrupoOperacaoApi.COMENTARIO, MetodoHttp.POST, "/api/projetos/{idProjeto}/documentos/{idDocumento}/comentarios", OperacaoProjeto.COMENTAR,
                    Set.of("idProjeto", "idDocumento", "versao", "idItem", "texto", "decisaoRelacionada"),
                    Set.of("referencia", "autor", "data", "texto", "decisaoRelacionada")),
            contrato(GrupoOperacaoApi.PEDIDO_AJUSTE, MetodoHttp.POST, "/api/projetos/{idProjeto}/documentos/{idDocumento}/ajustes", OperacaoProjeto.AVALIAR,
                    Set.of("idProjeto", "idDocumento", "versao", "idItem", "justificativa"),
                    Set.of("id", "documentoOriginal", "solicitante", "data", "justificativa", "responsavelTecnico", "estado")),
            contrato(GrupoOperacaoApi.PEDIDO_AJUSTE, MetodoHttp.POST, "/api/projetos/{idProjeto}/documentos/{idDocumento}/ajustes/{pedidoAjusteId}/tratamentos", OperacaoProjeto.AVALIAR,
                    Set.of("idProjeto", "idDocumento", "pedidoAjusteId", "decisao", "justificativa"),
                    Set.of("id", "documento", "idItem", "estado", "responsavelTecnico", "dataSolicitacao", "dataTratamento",
                            "decisaoTratamento", "justificativaTratamento", "idVersaoResultante")),
            contrato(GrupoOperacaoApi.CONFIRMACAO, MetodoHttp.POST, "/api/projetos/{idProjeto}/documentos/{idDocumento}/confirmacoes", OperacaoProjeto.CONFIRMAR,
                    Set.of("idProjeto", "idDocumento", "versaoConfirmada", "escopo"),
                    Set.of("projeto", "documento", "versaoConfirmada", "responsavel", "data", "escopo")),
            contrato(GrupoOperacaoApi.HISTORICO, MetodoHttp.GET, "/api/projetos/{idProjeto}/documentos/{idDocumento}/historico", OperacaoProjeto.CONSULTAR,
                    Set.of("idProjeto", "idDocumento"),
                    Set.of("eventos", "tipo", "decisao", "justificativa", "dataOcorrencia", "usuario", "documento", "item")),
            contrato(GrupoOperacaoApi.AUDITORIA, MetodoHttp.GET, "/api/projetos/{idProjeto}/auditoria", OperacaoProjeto.CONSULTAR,
                    Set.of("idProjeto", "idDocumento"),
                    Set.of("eventos", "correlacao", "autorTecnico", "projeto", "documento", "recurso", "acao",
                            "resultado", "detalhes", "dataOcorrencia", "politicaRetencao")),
            contrato(GrupoOperacaoApi.PARTICIPANTE, MetodoHttp.POST, "/api/projetos/{idProjeto}/participantes", OperacaoProjeto.ADMINISTRAR,
                    Set.of("idProjeto", "identificador", "nome", "login", "unidade", "papel"),
                    Set.of("nome", "login", "unidade", "papel")),
            contrato(GrupoOperacaoApi.PARTICIPANTE, MetodoHttp.GET, "/api/projetos/{idProjeto}/participantes", OperacaoProjeto.ADMINISTRAR,
                    Set.of("idProjeto"),
                    Set.of("participantes", "nome", "login", "unidade", "papel")),
            contrato(GrupoOperacaoApi.PARTICIPANTE, MetodoHttp.GET, "/api/projetos/{idProjeto}/participantes/pesquisa", OperacaoProjeto.ADMINISTRAR,
                    Set.of("idProjeto", "termo", "matricula", "unidadeOuLotacao"),
                    Set.of("nome", "matricula", "vinculo", "cargoOuFuncao", "email", "login", "unidadeOuLotacao"))
    );

    private ContratosRestAthena() {
    }

    public static List<ContratoRestAthena> todos() {
        return CONTRATOS;
    }

    public static List<ContratoRestAthena> porGrupo(GrupoOperacaoApi grupo) {
        if (grupo == null) {
            throw new IllegalArgumentException("grupo da operacao de API deve ser informado.");
        }
        return CONTRATOS.stream()
                .filter(contrato -> contrato.grupo() == grupo)
                .toList();
    }

    private static ContratoRestAthena contrato(
            GrupoOperacaoApi grupo,
            MetodoHttp metodo,
            String caminho,
            OperacaoProjeto operacaoAutorizacao,
            Set<String> entradas,
            Set<String> saidas
    ) {
        return new ContratoRestAthena(
                grupo,
                metodo,
                caminho,
                operacaoAutorizacao,
                entradas,
                saidas,
                ERROS_PADRAO,
                true,
                true,
                FONTES
        );
    }
}
