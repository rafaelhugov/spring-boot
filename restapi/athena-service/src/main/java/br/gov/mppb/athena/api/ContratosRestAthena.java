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
            "SOL.API.001",
            "SOL.SEGURANCA.001"
    );

    private static final List<ContratoRestAthena> CONTRATOS = List.of(
            contrato(GrupoOperacaoApi.PROJETO, MetodoHttp.POST, "/api/projetos", OperacaoProjeto.ADMINISTRAR,
                    Set.of("nome", "descricao", "unidades", "participantes"),
                    Set.of("identificador", "nome", "descricao", "unidades", "participantes", "situacao", "criadoEm", "criadoPor")),
            contrato(GrupoOperacaoApi.PROJETO, MetodoHttp.PUT, "/api/projetos/{projetoId}", OperacaoProjeto.ADMINISTRAR,
                    Set.of("projetoId", "nome", "descricao", "unidades", "participantes", "situacao"),
                    Set.of("identificador", "nome", "descricao", "unidades", "participantes", "situacao", "criadoEm", "criadoPor")),
            contrato(GrupoOperacaoApi.PROJETO, MetodoHttp.GET, "/api/projetos/{projetoId}", OperacaoProjeto.CONSULTAR,
                    Set.of("projetoId"),
                    Set.of("identificador", "nome", "descricao", "unidades", "participantes", "situacao", "criadoEm", "criadoPor")),
            contrato(GrupoOperacaoApi.DOCUMENTO, MetodoHttp.GET, "/api/projetos/{projetoId}/documentos", OperacaoProjeto.CONSULTAR,
                    Set.of("projetoId", "grupo", "categoria"),
                    Set.of("documentos", "grupo", "categoria", "finalidade", "apresentacao", "relacoesRecebidas")),
            contrato(GrupoOperacaoApi.DOCUMENTO, MetodoHttp.GET, "/api/projetos/{projetoId}/documentos/{documentoId}", OperacaoProjeto.CONSULTAR,
                    Set.of("projetoId", "documentoId"),
                    Set.of("identificador", "titulo", "metadados", "versoes", "relacoesRecebidas")),
            contrato(GrupoOperacaoApi.ITEM_DOCUMENTAL, MetodoHttp.GET, "/api/projetos/{projetoId}/documentos/{documentoId}/itens/{itemId}", OperacaoProjeto.CONSULTAR,
                    Set.of("projetoId", "documentoId", "itemId"),
                    Set.of("identificador", "titulo", "conteudo", "rastreabilidade", "relacoesRecebidas")),
            contrato(GrupoOperacaoApi.AVALIACAO, MetodoHttp.POST, "/api/projetos/{projetoId}/documentos/{documentoId}/avaliacoes", OperacaoProjeto.AVALIAR,
                    Set.of("projetoId", "documentoId", "versao", "itemId", "decisao", "justificativa"),
                    Set.of("referencia", "responsavel", "data", "decisao", "justificativa")),
            contrato(GrupoOperacaoApi.COMENTARIO, MetodoHttp.POST, "/api/projetos/{projetoId}/documentos/{documentoId}/comentarios", OperacaoProjeto.COMENTAR,
                    Set.of("projetoId", "documentoId", "versao", "itemId", "texto", "decisaoRelacionada"),
                    Set.of("referencia", "autor", "data", "texto", "decisaoRelacionada")),
            contrato(GrupoOperacaoApi.PEDIDO_AJUSTE, MetodoHttp.POST, "/api/projetos/{projetoId}/documentos/{documentoId}/ajustes", OperacaoProjeto.AVALIAR,
                    Set.of("projetoId", "documentoId", "versao", "itemId", "justificativa", "processoExternoResponsavel"),
                    Set.of("documentoOriginal", "solicitante", "data", "justificativa", "processoExternoResponsavel")),
            contrato(GrupoOperacaoApi.CONFIRMACAO, MetodoHttp.POST, "/api/projetos/{projetoId}/documentos/{documentoId}/confirmacoes", OperacaoProjeto.CONFIRMAR,
                    Set.of("projetoId", "documentoId", "versaoConfirmada", "escopo"),
                    Set.of("projeto", "documento", "versaoConfirmada", "responsavel", "data", "escopo")),
            contrato(GrupoOperacaoApi.PARTICIPANTE, MetodoHttp.GET, "/api/projetos/{projetoId}/participantes/pesquisa", OperacaoProjeto.ADMINISTRAR,
                    Set.of("projetoId", "termo", "matricula", "unidadeOuLotacao"),
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
