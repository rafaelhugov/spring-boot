package br.gov.mppb.athena.integracao;

import br.gov.mppb.athena.dominio.Documento;
import br.gov.mppb.athena.dominio.ItemDocumental;
import br.gov.mppb.athena.dominio.ProjetoInstitucional;
import br.gov.mppb.athena.dominio.UsuarioInstitucional;
import br.gov.mppb.athena.dominio.VersaoDocumento;
import br.gov.mppb.athena.persistencia.DadosDocumentoPersistente;
import br.gov.mppb.athena.persistencia.DadosItemDocumentalPersistente;
import br.gov.mppb.athena.persistencia.DadosProjetoPersistente;
import br.gov.mppb.athena.persistencia.DadosUsuarioPersistente;
import br.gov.mppb.athena.persistencia.DadosVersaoDocumentoPersistente;
import br.gov.mppb.athena.persistencia.DadosVinculoProjetoPersistente;
import br.gov.mppb.athena.persistencia.PersistenciaTransacionalAthena;
import br.gov.mppb.athena.persistencia.RegistroHistoricoDocumental;
import br.gov.mppb.athena.seguranca.ContextoAutorizacaoInstitucional;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class ServicoIngestaoDocumentalPersistente {

    private final ServicoIngestaoDocumentalExterna ingestao = new ServicoIngestaoDocumentalExterna();
    private final PersistenciaTransacionalAthena persistencia;

    public ServicoIngestaoDocumentalPersistente(PersistenciaTransacionalAthena persistencia) {
        if (persistencia == null) {
            throw new IllegalArgumentException("persistencia transacional deve ser informada.");
        }
        this.persistencia = persistencia;
    }

    public ResultadoIngestaoPersistente registrarPrimeiraVersao(
            ProjetoInstitucional projeto,
            ContextoAutorizacaoInstitucional contexto,
            PedidoIngestaoDocumentoExterno pedido
    ) {
        Documento documento = ingestao.registrarPrimeiraVersao(projeto, contexto, pedido);
        UsuarioInstitucional usuario = contexto.usuario();
        UUID usuarioId = id("usuario:" + usuario.identificador().valor());
        UUID projetoId = id("projeto:" + projeto.identificador().valor());
        UUID documentoId = id("documento:" + documento.projeto().valor() + ":" + documento.identificador());
        VersaoDocumento primeiraVersao = documento.versoes().get(0);

        persistencia.registrarHistoricoDocumental(new RegistroHistoricoDocumental(
                usuario(usuarioId, usuario),
                projeto(projetoId, projeto, usuarioId),
                vinculo(projetoId, usuarioId, primeiraVersao.recebidaEm()),
                documento(documentoId, documento, usuarioId, primeiraVersao.recebidaEm()),
                versao(documentoId, primeiraVersao, usuarioId, pedido.nomeFisicoArquivo()),
                itens(documentoId, primeiraVersao),
                primeiraVersao.recebidaEm(),
                id("auditoria:" + documentoId + ":1")
        ));
        return new ResultadoIngestaoPersistente(documento, projetoId, documentoId);
    }

    public ResultadoIngestaoPersistente registrarNovaVersao(
            ProjetoInstitucional projeto,
            ContextoAutorizacaoInstitucional contexto,
            Documento documentoAtual,
            PedidoIngestaoDocumentoExterno pedido
    ) {
        Documento documento = ingestao.registrarNovaVersao(projeto, contexto, documentoAtual, pedido);
        UsuarioInstitucional usuario = contexto.usuario();
        UUID usuarioId = id("usuario:" + usuario.identificador().valor());
        UUID projetoId = id("projeto:" + projeto.identificador().valor());
        UUID documentoId = id("documento:" + documento.projeto().valor() + ":" + documento.identificador());
        VersaoDocumento novaVersao = documento.versoes().get(documento.versoes().size() - 1);

        persistencia.registrarNovaVersaoDocumental(
                projetoId,
                usuarioId,
                versao(documentoId, novaVersao, usuarioId, pedido.nomeFisicoArquivo()),
                itens(documentoId, novaVersao),
                novaVersao.recebidaEm(),
                id("auditoria:" + documentoId + ":" + novaVersao.numero())
        );
        return new ResultadoIngestaoPersistente(documento, projetoId, documentoId);
    }

    public ContratoIntegracaoDocumentalExterna contratoDependenciaExternaGovernada() {
        return new ContratoIntegracaoDocumentalExterna(
                "ESP.INT.003",
                false,
                "Dependencia externa indisponivel deve ser registrada como contrato de integracao pendente, sem simulacao definitiva."
        );
    }

    private static DadosUsuarioPersistente usuario(UUID id, UsuarioInstitucional usuario) {
        return new DadosUsuarioPersistente(id, usuario.identificador().valor(), usuario.nome(), usuario.login(), usuario.unidade());
    }

    private static DadosProjetoPersistente projeto(UUID id, ProjetoInstitucional projeto, UUID usuarioId) {
        return new DadosProjetoPersistente(id, projeto.identificador().valor(), projeto.nome(), projeto.descricao(),
                projeto.situacao().name(), projeto.criadoEm(), usuarioId);
    }

    private static DadosVinculoProjetoPersistente vinculo(UUID projetoId, UUID usuarioId, OffsetDateTime inicioEm) {
        return new DadosVinculoProjetoPersistente(id("vinculo:" + projetoId + ":" + usuarioId), projetoId,
                usuarioId, "RESPONSAVEL_TECNICO", "DITEC", inicioEm, usuarioId);
    }

    private static DadosDocumentoPersistente documento(UUID id, Documento documento, UUID usuarioId, OffsetDateTime criadoEm) {
        return new DadosDocumentoPersistente(id, documento.identificador(), id("projeto:" + documento.projeto().valor()),
                documento.titulo(), documento.metadados().grupo(), documento.metadados().categoria(),
                documento.metadados().finalidade(), documento.metadados().apresentacao(), criadoEm, usuarioId);
    }

    private static DadosVersaoDocumentoPersistente versao(
            UUID documentoId,
            VersaoDocumento versao,
            UUID usuarioId,
            String origemExterna
    ) {
        return new DadosVersaoDocumentoPersistente(
                id("versao:" + documentoId + ":" + versao.numero()),
                documentoId,
                versao.numero(),
                versao.formato().name(),
                versao.conteudoOriginal(),
                Integer.toHexString(versao.conteudoOriginal().hashCode()),
                versao.recebidaEm(),
                usuarioId,
                origemExterna
        );
    }

    private static List<DadosItemDocumentalPersistente> itens(UUID documentoId, VersaoDocumento versao) {
        return versao.itens().stream()
                .map(item -> item(documentoId, item, versao))
                .toList();
    }

    private static DadosItemDocumentalPersistente item(UUID documentoId, ItemDocumental item, VersaoDocumento versao) {
        UUID versaoId = id("versao:" + documentoId + ":" + versao.numero());
        return new DadosItemDocumentalPersistente(
                id("item:" + versaoId + ":" + item.identificador()),
                versaoId,
                item.identificador(),
                item.titulo(),
                item.conteudo(),
                1,
                item.rastreabilidade().fontes().stream().toList()
        );
    }

    private static UUID id(String valor) {
        return UUID.nameUUIDFromBytes(valor.getBytes(StandardCharsets.UTF_8));
    }
}
