package br.gov.mppb.athena.integracao;

import br.gov.mppb.athena.dominio.Documento;
import br.gov.mppb.athena.dominio.VersaoDocumento;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ServicoIngestaoDocumentalExterna {

    public Documento registrarPrimeiraVersao(PedidoIngestaoDocumentoExterno pedido) {
        validarPedido(pedido);
        return new Documento(
                pedido.identificadorDocumento(),
                pedido.projeto(),
                pedido.tituloDocumento(),
                pedido.metadados(),
                List.of(criarVersao(1, pedido))
        );
    }

    public Documento registrarNovaVersao(
            Documento documentoAtual,
            PedidoIngestaoDocumentoExterno pedido
    ) {
        if (documentoAtual == null) {
            throw new IllegalArgumentException("documento atual deve ser informado.");
        }
        validarPedido(pedido);
        if (!Objects.equals(documentoAtual.identificador(), pedido.identificadorDocumento())
                || !Objects.equals(documentoAtual.projeto(), pedido.projeto())) {
            throw new IllegalArgumentException("nova versao deve referenciar o mesmo documento e projeto.");
        }

        List<VersaoDocumento> versoesPreservadas = new ArrayList<>(documentoAtual.versoes());
        int proximoNumero = versoesPreservadas.stream()
                .map(VersaoDocumento::numero)
                .max(Comparator.naturalOrder())
                .orElse(0) + 1;
        versoesPreservadas.add(criarVersao(proximoNumero, pedido));

        return new Documento(
                documentoAtual.identificador(),
                documentoAtual.projeto(),
                pedido.tituloDocumento(),
                pedido.metadados(),
                versoesPreservadas
        );
    }

    private static void validarPedido(PedidoIngestaoDocumentoExterno pedido) {
        if (pedido == null) {
            throw new IllegalArgumentException("pedido de ingestao documental deve ser informado.");
        }
    }

    private static VersaoDocumento criarVersao(int numero, PedidoIngestaoDocumentoExterno pedido) {
        return new VersaoDocumento(
                numero,
                pedido.formato(),
                pedido.conteudoOriginal(),
                pedido.recebidaEm(),
                pedido.representacaoInterna()
        );
    }
}
