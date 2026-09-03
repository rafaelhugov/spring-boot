package br.gov.mppb.athena.integracao;

import br.gov.mppb.athena.dominio.Documento;
import br.gov.mppb.athena.dominio.ProjetoInstitucional;
import br.gov.mppb.athena.dominio.VersaoDocumento;
import br.gov.mppb.athena.seguranca.ContextoAutorizacaoInstitucional;
import br.gov.mppb.athena.seguranca.OperacaoProjeto;
import br.gov.mppb.athena.seguranca.PoliticaSegurancaAthena;
import br.gov.mppb.athena.seguranca.ResultadoAutorizacao;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ServicoIngestaoDocumentalExterna {

    public Documento registrarPrimeiraVersao(
            ProjetoInstitucional projeto,
            ContextoAutorizacaoInstitucional contexto,
            PedidoIngestaoDocumentoExterno pedido
    ) {
        validarPedido(pedido);
        autorizarAdministracao(projeto, contexto, pedido);
        return new Documento(
                pedido.identificadorDocumento(),
                pedido.projeto(),
                pedido.tituloDocumento(),
                pedido.metadados(),
                List.of(criarVersao(1, pedido))
        );
    }

    public Documento registrarNovaVersao(
            ProjetoInstitucional projeto,
            ContextoAutorizacaoInstitucional contexto,
            Documento documentoAtual,
            PedidoIngestaoDocumentoExterno pedido
    ) {
        if (documentoAtual == null) {
            throw new IllegalArgumentException("documento atual deve ser informado.");
        }
        validarPedido(pedido);
        autorizarAdministracao(projeto, contexto, pedido);
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

    private static void autorizarAdministracao(
            ProjetoInstitucional projeto,
            ContextoAutorizacaoInstitucional contexto,
            PedidoIngestaoDocumentoExterno pedido
    ) {
        if (projeto == null) {
            throw new IllegalArgumentException("projeto protegido deve ser informado.");
        }
        if (!projeto.identificador().equals(pedido.projeto())) {
            throw new IllegalArgumentException("ingestao documental deve referenciar o projeto protegido informado.");
        }

        ResultadoAutorizacao autorizacao = PoliticaSegurancaAthena.autorizar(projeto, contexto, OperacaoProjeto.ADMINISTRAR);
        if (!autorizacao.permitido()) {
            throw new IllegalStateException("ingestao documental externa nao autorizada: " + autorizacao.motivo());
        }
    }

    private static VersaoDocumento criarVersao(int numero, PedidoIngestaoDocumentoExterno pedido) {
        return new VersaoDocumento(
                numero,
                pedido.formato(),
                pedido.conteudoOriginal(),
                pedido.dataRecebimento(),
                pedido.representacaoInterna()
        );
    }
}
