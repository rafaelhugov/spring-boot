package br.gov.mppb.athena.persistencia;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record RegistroHistoricoDocumental(
        DadosUsuarioPersistente usuario,
        DadosProjetoPersistente projeto,
        DadosVinculoProjetoPersistente vinculo,
        DadosDocumentoPersistente documento,
        DadosVersaoDocumentoPersistente versao,
        List<DadosItemDocumentalPersistente> itens,
        OffsetDateTime ocorridoEm,
        UUID eventoAuditoriaId
) {
}
