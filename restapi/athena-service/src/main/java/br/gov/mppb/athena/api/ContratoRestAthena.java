package br.gov.mppb.athena.api;

import br.gov.mppb.athena.dominio.ProjetoInstitucional;
import br.gov.mppb.athena.seguranca.ContextoAutorizacaoInstitucional;
import br.gov.mppb.athena.seguranca.OperacaoProjeto;
import br.gov.mppb.athena.seguranca.PoliticaSegurancaAthena;
import br.gov.mppb.athena.seguranca.ResultadoAutorizacao;
import java.util.List;
import java.util.Set;

public record ContratoRestAthena(
        GrupoOperacaoApi grupo,
        MetodoHttp metodo,
        String caminho,
        OperacaoProjeto operacaoAutorizacao,
        Set<String> entradas,
        Set<String> saidas,
        List<ErroFuncionalApi> errosFuncionais,
        boolean respostaOmiteCpf,
        boolean respostaLimitaRelacoesExplicitamenteRecebidas,
        Set<String> fontesRastreabilidade
) {

    public ContratoRestAthena {
        if (grupo == null) {
            throw new IllegalArgumentException("grupo da operacao de API deve ser informado.");
        }
        if (metodo == null) {
            throw new IllegalArgumentException("metodo HTTP deve ser informado.");
        }
        caminho = TextoApi.validar(caminho, "caminho REST");
        if (!caminho.startsWith("/api/")) {
            throw new IllegalArgumentException("caminho REST deve iniciar com /api/.");
        }
        if (operacaoAutorizacao == null) {
            throw new IllegalArgumentException("operacao de autorizacao deve ser informada.");
        }
        entradas = copiarConjuntoObrigatorio(entradas, "entradas do contrato REST");
        saidas = copiarConjuntoObrigatorio(saidas, "saidas do contrato REST");
        errosFuncionais = errosFuncionais == null ? List.of() : List.copyOf(errosFuncionais);
        if (errosFuncionais.isEmpty()) {
            throw new IllegalArgumentException("erros funcionais do contrato REST devem ser informados.");
        }
        fontesRastreabilidade = copiarConjuntoObrigatorio(fontesRastreabilidade, "fontes de rastreabilidade do contrato REST");
    }

    public ResultadoAutorizacao autorizar(ProjetoInstitucional projeto, ContextoAutorizacaoInstitucional contexto) {
        return PoliticaSegurancaAthena.autorizar(projeto, contexto, operacaoAutorizacao);
    }

    @Override
    public Set<String> entradas() {
        return Set.copyOf(entradas);
    }

    @Override
    public Set<String> saidas() {
        return Set.copyOf(saidas);
    }

    @Override
    public List<ErroFuncionalApi> errosFuncionais() {
        return List.copyOf(errosFuncionais);
    }

    @Override
    public Set<String> fontesRastreabilidade() {
        return Set.copyOf(fontesRastreabilidade);
    }

    private static Set<String> copiarConjuntoObrigatorio(Set<String> valores, String campo) {
        if (valores == null || valores.isEmpty()) {
            throw new IllegalArgumentException(campo + " devem ser informadas.");
        }
        return Set.copyOf(valores);
    }
}
