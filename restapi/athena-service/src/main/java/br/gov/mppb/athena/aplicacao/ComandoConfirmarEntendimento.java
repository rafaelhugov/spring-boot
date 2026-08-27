package br.gov.mppb.athena.aplicacao;

import br.gov.mppb.athena.dominio.IdentificadorProjeto;
import br.gov.mppb.athena.dominio.IdentificadorUsuario;

import java.time.OffsetDateTime;

public record ComandoConfirmarEntendimento(
        IdentificadorProjeto projeto,
        String documento,
        int versaoConfirmada,
        IdentificadorUsuario responsavel,
        OffsetDateTime data,
        String escopo
) {

    public ComandoConfirmarEntendimento {
        if (projeto == null) {
            throw new IllegalArgumentException("projeto da confirmacao deve ser informado.");
        }
        documento = TextoAplicacao.validar(documento, "documento da confirmacao");
        if (versaoConfirmada < 1) {
            throw new IllegalArgumentException("versao confirmada deve ser positiva.");
        }
        if (responsavel == null) {
            throw new IllegalArgumentException("responsavel da confirmacao deve ser informado.");
        }
        if (data == null) {
            throw new IllegalArgumentException("data da confirmacao deve ser informada.");
        }
        escopo = TextoAplicacao.validar(escopo, "escopo da confirmacao");
    }
}
