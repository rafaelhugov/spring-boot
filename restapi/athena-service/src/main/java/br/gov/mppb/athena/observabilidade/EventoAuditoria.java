package br.gov.mppb.athena.observabilidade;

import br.gov.mppb.athena.dominio.IdentificadorProjeto;
import br.gov.mppb.athena.dominio.IdentificadorUsuario;
import java.time.OffsetDateTime;
import java.util.Map;

public record EventoAuditoria(
        IdentificadorUsuario ator,
        OffsetDateTime ocorridoEm,
        IdentificadorProjeto projeto,
        RecursoAuditavel recursoAfetado,
        AcaoAuditavel acao,
        ResultadoEventoAuditavel resultado,
        Map<String, String> detalhes,
        PoliticaRetencaoAuditavel politicaRetencao
) {

    public EventoAuditoria {
        if (ator == null) {
            throw new IllegalArgumentException("ator do evento de auditoria deve ser informado.");
        }
        if (ocorridoEm == null) {
            throw new IllegalArgumentException("data do evento de auditoria deve ser informada.");
        }
        if (projeto == null) {
            throw new IllegalArgumentException("projeto do evento de auditoria deve ser informado.");
        }
        if (recursoAfetado == null) {
            throw new IllegalArgumentException("recurso afetado do evento de auditoria deve ser informado.");
        }
        if (acao == null) {
            throw new IllegalArgumentException("acao do evento de auditoria deve ser informada.");
        }
        if (resultado == null) {
            throw new IllegalArgumentException("resultado do evento de auditoria deve ser informado.");
        }
        detalhes = detalhes == null ? Map.of() : Map.copyOf(detalhes);
        if (politicaRetencao == null) {
            politicaRetencao = PoliticaRetencaoAuditavel.PENDENTE_DEFINICAO_INSTITUCIONAL;
        }
    }

    @Override
    public Map<String, String> detalhes() {
        return Map.copyOf(detalhes);
    }
}
