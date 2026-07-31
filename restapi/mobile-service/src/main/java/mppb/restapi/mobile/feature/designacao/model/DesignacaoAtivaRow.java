package mppb.restapi.mobile.feature.designacao.model;

import java.time.LocalDate;

public record DesignacaoAtivaRow(
        Long id,
        String servidorMatricula,
        String tipoCodigo,
        String tipoDescricaoOriginal,
        String funcaoNome,
        String orgaoNome,
        String orgaoExecucaoNome,
        LocalDate dataInicial,
        LocalDate dataFinal,
        LocalDate dataPublicacao,
        String atoNumero,
        boolean documentoInicial,
        String tipoCargo,
        String tipoCargoNome) {
}
