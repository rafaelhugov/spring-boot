package br.gov.mppb.athena.integracao;

import br.gov.mppb.athena.dominio.FormatoDocumento;
import br.gov.mppb.athena.dominio.IdentificadorProjeto;
import br.gov.mppb.athena.dominio.ItemDocumental;
import br.gov.mppb.athena.dominio.MetadadosDocumento;
import java.time.OffsetDateTime;
import java.util.List;

public record PedidoIngestaoDocumentoExterno(
        String identificadorDocumento,
        IdentificadorProjeto projeto,
        String tituloDocumento,
        MetadadosDocumento metadados,
        FormatoDocumento formato,
        String conteudoOriginal,
        List<ItemDocumental> representacaoInterna,
        OffsetDateTime recebidaEm,
        String nomeFisicoArquivo
) {

    public PedidoIngestaoDocumentoExterno {
        identificadorDocumento = validarTexto(identificadorDocumento, "identificador do documento");
        if (projeto == null) {
            throw new IllegalArgumentException("projeto do documento deve ser informado.");
        }
        tituloDocumento = validarTexto(tituloDocumento, "titulo do documento");
        if (metadados == null) {
            throw new IllegalArgumentException("metadados do documento devem ser informados.");
        }
        if (formato == null) {
            throw new IllegalArgumentException("formato do documento deve ser informado explicitamente.");
        }
        conteudoOriginal = validarTexto(conteudoOriginal, "conteudo original do documento");
        if (representacaoInterna == null || representacaoInterna.isEmpty()) {
            throw new IllegalArgumentException("representacao interna do documento deve ser informada.");
        }
        representacaoInterna = List.copyOf(representacaoInterna);
        if (recebidaEm == null) {
            throw new IllegalArgumentException("data de recebimento do documento deve ser informada.");
        }
    }

    private static String validarTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " deve ser informado.");
        }
        return valor.strip();
    }
}
