package br.gov.mppb.athena.aplicacao;

import br.gov.mppb.athena.dominio.IdentificadorProjeto;
import br.gov.mppb.athena.dominio.IdentificadorUsuario;

import java.time.OffsetDateTime;

public record ConfirmacaoEntendimentoRegistrada(
        IdentificadorProjeto projeto,
        String documento,
        int versaoConfirmada,
        IdentificadorUsuario responsavel,
        OffsetDateTime data,
        String escopo
) {
}
