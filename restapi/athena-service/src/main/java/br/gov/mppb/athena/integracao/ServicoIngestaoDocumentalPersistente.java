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
        UUID idUsuario = id("usuario:" + usuario.identificador().valor());
        UUID idProjeto = id("projeto:" + projeto.identificador().valor());
        UUID idDocumento = id("documento:" + documento.projeto().valor() + ":" + documento.identificador());
        VersaoDocumento primeiraVersao = documento.versoes().get(0);

        persistencia.registrarHistoricoDocumental(new RegistroHistoricoDocumental(
                usuario(idUsuario, usuario),
                projeto(idProjeto, projeto, idUsuario),
                vinculo(idProjeto, idUsuario, primeiraVersao.dataRecebimento()),
                documento(idDocumento, documento, idUsuario, primeiraVersao.dataRecebimento()),
                versao(idDocumento, primeiraVersao, idUsuario, pedido.nomeFisicoArquivo()),
                itens(idDocumento, primeiraVersao),
                primeiraVersao.dataRecebimento(),
                id("auditoria:" + idDocumento + ":1")
        ));
        return new ResultadoIngestaoPersistente(documento, idProjeto, idDocumento);
    }

    public ResultadoIngestaoPersistente registrarNovaVersao(
            ProjetoInstitucional projeto,
            ContextoAutorizacaoInstitucional contexto,
            Documento documentoAtual,
            PedidoIngestaoDocumentoExterno pedido
    ) {
        Documento documento = ingestao.registrarNovaVersao(projeto, contexto, documentoAtual, pedido);
        UsuarioInstitucional usuario = contexto.usuario();
        UUID idUsuario = id("usuario:" + usuario.identificador().valor());
        UUID idProjeto = id("projeto:" + projeto.identificador().valor());
        UUID idDocumento = id("documento:" + documento.projeto().valor() + ":" + documento.identificador());
        VersaoDocumento novaVersao = documento.versoes().get(documento.versoes().size() - 1);
        UUID idVersaoAnterior = id("versao:" + idDocumento + ":" + (novaVersao.numero() - 1));

        persistencia.registrarNovaVersaoDocumental(
                idProjeto,
                idUsuario,
                versao(idDocumento, novaVersao, idUsuario, pedido.nomeFisicoArquivo(), idVersaoAnterior),
                itens(idDocumento, novaVersao),
                novaVersao.dataRecebimento(),
                id("auditoria:" + idDocumento + ":" + novaVersao.numero())
        );
        return new ResultadoIngestaoPersistente(documento, idProjeto, idDocumento);
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

    private static DadosProjetoPersistente projeto(UUID id, ProjetoInstitucional projeto, UUID idUsuario) {
        return new DadosProjetoPersistente(id, projeto.identificador().valor(), projeto.nome(), projeto.descricao(),
                projeto.situacao().name(), projeto.dataCriacao(), idUsuario);
    }

    private static DadosVinculoProjetoPersistente vinculo(UUID idProjeto, UUID idUsuario, OffsetDateTime dataInicio) {
        return new DadosVinculoProjetoPersistente(id("vinculo:" + idProjeto + ":" + idUsuario), idProjeto,
                idUsuario, "RESPONSAVEL_TECNICO", "DITEC", dataInicio, idUsuario);
    }

    private static DadosDocumentoPersistente documento(UUID id, Documento documento, UUID idUsuario, OffsetDateTime dataCriacao) {
        return new DadosDocumentoPersistente(id, documento.identificador(), id("projeto:" + documento.projeto().valor()),
                documento.titulo(), documento.metadados().grupo(), documento.metadados().categoria(),
                documento.metadados().finalidade(), documento.metadados().apresentacao(), dataCriacao, idUsuario);
    }

    private static DadosVersaoDocumentoPersistente versao(
            UUID idDocumento,
            VersaoDocumento versao,
            UUID idUsuario,
            String origemExterna
    ) {
        return new DadosVersaoDocumentoPersistente(
                id("versao:" + idDocumento + ":" + versao.numero()),
                idDocumento,
                versao.numero(),
                versao.formato().name(),
                versao.conteudoOriginal(),
                Integer.toHexString(versao.conteudoOriginal().hashCode()),
                versao.dataRecebimento(),
                idUsuario,
                origemExterna
        );
    }

    private static DadosVersaoDocumentoPersistente versao(
            UUID idDocumento,
            VersaoDocumento versao,
            UUID idUsuario,
            String origemExterna,
            UUID idVersaoAnterior
    ) {
        return new DadosVersaoDocumentoPersistente(
                id("versao:" + idDocumento + ":" + versao.numero()),
                idDocumento,
                versao.numero(),
                versao.formato().name(),
                versao.conteudoOriginal(),
                Integer.toHexString(versao.conteudoOriginal().hashCode()),
                versao.dataRecebimento(),
                idUsuario,
                origemExterna,
                idVersaoAnterior,
                false,
                null
        );
    }

    private static List<DadosItemDocumentalPersistente> itens(UUID idDocumento, VersaoDocumento versao) {
        return versao.itens().stream()
                .map(item -> item(idDocumento, item, versao))
                .toList();
    }

    private static DadosItemDocumentalPersistente item(UUID idDocumento, ItemDocumental item, VersaoDocumento versao) {
        UUID idVersao = id("versao:" + idDocumento + ":" + versao.numero());
        return new DadosItemDocumentalPersistente(
                id("item:" + idVersao + ":" + item.identificador()),
                idVersao,
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
