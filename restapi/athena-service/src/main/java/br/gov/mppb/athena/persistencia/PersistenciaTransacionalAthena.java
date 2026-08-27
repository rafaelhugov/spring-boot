package br.gov.mppb.athena.persistencia;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class PersistenciaTransacionalAthena {

    private static final String ESQUEMA = "athena.";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacao;

    public PersistenciaTransacionalAthena(JdbcTemplate jdbc, PlatformTransactionManager gerenciadorTransacao) {
        this.jdbc = jdbc;
        this.transacao = new TransactionTemplate(gerenciadorTransacao);
    }

    public void registrarHistoricoDocumental(RegistroHistoricoDocumental registro) {
        transacao.executeWithoutResult(status -> {
            inserirUsuario(registro.usuario());
            inserirProjeto(registro.projeto());
            inserirVinculo(registro.vinculo());
            inserirDocumento(registro.documento());
            inserirVersao(registro.versao());
            registro.itens().forEach(this::inserirItem);
            inserirAuditoriaHistorico(registro);
        });
    }

    public void registrarNovaVersaoDocumental(
            UUID projetoId,
            UUID usuarioId,
            DadosVersaoDocumentoPersistente versao,
            List<DadosItemDocumentalPersistente> itens,
            OffsetDateTime ocorridoEm,
            UUID eventoAuditoriaId
    ) {
        transacao.executeWithoutResult(status -> {
            inserirVersao(versao);
            itens.forEach(this::inserirItem);
            jdbc.update("""
                    INSERT INTO athena.eventoauditoria
                        (id, projetoid, documentoid, versaoid, usuarioid, tipo, dados, ocorridoem, registradoporid)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, eventoAuditoriaId, projetoId, versao.documentoId(), versao.id(), usuarioId,
                    "NOVA_VERSAO_DOCUMENTAL_REGISTRADA", "{}", ocorridoEm, usuarioId);
        });
    }

    public void registrarInteracaoHumana(RegistroInteracaoHumana registro) {
        transacao.executeWithoutResult(status -> {
            switch (registro.tipo()) {
                case AVALIACAO -> inserirAvaliacao(registro);
                case COMENTARIO -> inserirComentario(registro);
                case PEDIDO_AJUSTE -> inserirPedidoAjuste(registro);
                case CONFIRMACAO -> inserirConfirmacao(registro);
            }
            inserirAuditoriaInteracao(registro);
        });
    }

    public void atualizarSituacaoProjeto(UUID projetoId, String situacao, UUID alteradoPorId, OffsetDateTime alteradoEm) {
        transacao.executeWithoutResult(status -> jdbc.update("""
                UPDATE athena.projeto
                   SET situacao = ?, alteradoporid = ?, alteradoem = ?
                 WHERE id = ?
                """, situacao, alteradoPorId, alteradoEm, projetoId));
    }

    public HistoricoDocumentoPersistente consultarHistoricoDocumento(UUID documentoId) {
        DadosDocumentoPersistente documento = jdbc.queryForObject("""
                SELECT id, identificador, projetoid, titulo, grupo, categoria, finalidade,
                       apresentacao, criadoem, criadoporid
                  FROM athena.documento
                 WHERE id = ?
                """, this::mapearDocumento, documentoId);
        DadosProjetoPersistente projeto = jdbc.queryForObject("""
                SELECT id, identificador, nome, descricao, situacao, criadoem, criadoporid
                  FROM athena.projeto
                 WHERE id = ?
                """, this::mapearProjeto, documento.projetoId());
        List<DadosVersaoDocumentoPersistente> versoes = jdbc.query("""
                SELECT id, documentoid, numero, formato, conteudooriginal, hashconteudo,
                       recebidaem, recebidaporid, origemexterna
                  FROM athena.versaodocumento
                 WHERE documentoid = ?
                 ORDER BY numero
                """, this::mapearVersao, documentoId);
        List<DadosItemDocumentalPersistente> itens = jdbc.query("""
                SELECT i.id, i.versaoid, i.identificador, i.titulo, i.conteudo, i.ordem,
                       i.fontesrastreabilidade
                  FROM athena.itemdocumental i
                  JOIN athena.versaodocumento v ON v.id = i.versaoid
                 WHERE v.documentoid = ?
                 ORDER BY v.numero, i.ordem
                """, this::mapearItem, documentoId);

        return new HistoricoDocumentoPersistente(projeto, documento, versoes, itens);
    }

    private void inserirUsuario(DadosUsuarioPersistente usuario) {
        jdbc.update("""
                INSERT INTO athena.usuarioinstitucional (id, identificador, nome, login, unidade)
                VALUES (?, ?, ?, ?, ?)
                """, usuario.id(), usuario.identificador(), usuario.nome(), usuario.login(), usuario.unidade());
    }

    private void inserirProjeto(DadosProjetoPersistente projeto) {
        jdbc.update("""
                INSERT INTO athena.projeto (id, identificador, nome, descricao, situacao, criadoem, criadoporid)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, projeto.id(), projeto.identificador(), projeto.nome(), projeto.descricao(),
                projeto.situacao(), projeto.criadoEm(), projeto.criadoPorId());
    }

    private void inserirVinculo(DadosVinculoProjetoPersistente vinculo) {
        jdbc.update("""
                INSERT INTO athena.vinculoprojeto
                    (id, projetoid, usuarioid, papel, unidade, inicioem, registradoporid)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, vinculo.id(), vinculo.projetoId(), vinculo.usuarioId(), vinculo.papel(),
                vinculo.unidade(), vinculo.inicioEm(), vinculo.registradoPorId());
    }

    private void inserirDocumento(DadosDocumentoPersistente documento) {
        jdbc.update("""
                INSERT INTO athena.documento
                    (id, identificador, projetoid, titulo, grupo, categoria, finalidade,
                     apresentacao, criadoem, criadoporid)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, documento.id(), documento.identificador(), documento.projetoId(), documento.titulo(),
                documento.grupo(), documento.categoria(), documento.finalidade(), documento.apresentacao(),
                documento.criadoEm(), documento.criadoPorId());
    }

    private void inserirVersao(DadosVersaoDocumentoPersistente versao) {
        jdbc.update("""
                INSERT INTO athena.versaodocumento
                    (id, documentoid, numero, formato, conteudooriginal, hashconteudo,
                     recebidaem, recebidaporid, origemexterna)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, versao.id(), versao.documentoId(), versao.numero(), versao.formato(),
                versao.conteudoOriginal(), versao.hashConteudo(), versao.recebidaEm(),
                versao.recebidaPorId(), versao.origemExterna());
    }

    private void inserirItem(DadosItemDocumentalPersistente item) {
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO athena.itemdocumental
                        (id, versaoid, identificador, titulo, conteudo, ordem, fontesrastreabilidade)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """);
            ps.setObject(1, item.id());
            ps.setObject(2, item.versaoId());
            ps.setString(3, item.identificador());
            ps.setString(4, item.titulo());
            ps.setString(5, item.conteudo());
            ps.setInt(6, item.ordem());
            ps.setArray(7, connection.createArrayOf("VARCHAR", item.fontesRastreabilidade().toArray()));
            return ps;
        });
    }

    private void inserirAvaliacao(RegistroInteracaoHumana registro) {
        jdbc.update("""
                INSERT INTO athena.avaliacao (id, itemid, responsavelid, decisao, justificativa, avaliadaem)
                VALUES (?, ?, ?, ?, ?, ?)
                """, registro.id(), registro.itemId(), registro.usuarioId(), registro.decisao(),
                registro.justificativa(), registro.registradaEm());
    }

    private void inserirComentario(RegistroInteracaoHumana registro) {
        jdbc.update("""
                INSERT INTO athena.comentario (id, itemid, autorid, conteudo, registradoem, origem)
                VALUES (?, ?, ?, ?, ?, ?)
                """, registro.id(), registro.itemId(), registro.usuarioId(), registro.conteudo(),
                registro.registradaEm(), "ATHENA");
    }

    private void inserirPedidoAjuste(RegistroInteracaoHumana registro) {
        jdbc.update("""
                INSERT INTO athena.pedidoajuste
                    (id, itemid, solicitanteid, descricao, justificativa, situacao, solicitadoem)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, registro.id(), registro.itemId(), registro.usuarioId(), registro.conteudo(),
                registro.justificativa(), registro.decisao(), registro.registradaEm());
    }

    private void inserirConfirmacao(RegistroInteracaoHumana registro) {
        jdbc.update("""
                INSERT INTO athena.confirmacao (id, itemid, responsavelid, decisao, justificativa, confirmadaem)
                VALUES (?, ?, ?, ?, ?, ?)
                """, registro.id(), registro.itemId(), registro.usuarioId(), registro.decisao(),
                registro.justificativa(), registro.registradaEm());
    }

    private void inserirAuditoriaHistorico(RegistroHistoricoDocumental registro) {
        jdbc.update("""
                INSERT INTO athena.eventoauditoria
                    (id, projetoid, documentoid, versaoid, usuarioid, tipo, dados, ocorridoem, registradoporid)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, registro.eventoAuditoriaId(), registro.projeto().id(), registro.documento().id(),
                registro.versao().id(), registro.usuario().id(), "HISTORICO_DOCUMENTAL_REGISTRADO",
                "{}", registro.ocorridoEm(), registro.usuario().id());
    }

    private void inserirAuditoriaInteracao(RegistroInteracaoHumana registro) {
        jdbc.update("""
                INSERT INTO athena.eventoauditoria
                    (id, itemid, usuarioid, tipo, decisao, justificativa, dados, ocorridoem, registradoporid)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, registro.eventoAuditoriaId(), registro.itemId(), registro.usuarioId(),
                registro.tipo().name(), registro.decisao(), registro.justificativa(), "{}",
                registro.registradaEm(), registro.usuarioId());
    }

    private DadosProjetoPersistente mapearProjeto(ResultSet rs, int linha) throws SQLException {
        return new DadosProjetoPersistente(uuid(rs, "id"), rs.getString("identificador"), rs.getString("nome"),
                rs.getString("descricao"), rs.getString("situacao"), horario(rs, "criadoem"), uuid(rs, "criadoporid"));
    }

    private DadosDocumentoPersistente mapearDocumento(ResultSet rs, int linha) throws SQLException {
        return new DadosDocumentoPersistente(uuid(rs, "id"), rs.getString("identificador"), uuid(rs, "projetoid"),
                rs.getString("titulo"), rs.getString("grupo"), rs.getString("categoria"),
                rs.getString("finalidade"), rs.getString("apresentacao"), horario(rs, "criadoem"),
                uuid(rs, "criadoporid"));
    }

    private DadosVersaoDocumentoPersistente mapearVersao(ResultSet rs, int linha) throws SQLException {
        return new DadosVersaoDocumentoPersistente(uuid(rs, "id"), uuid(rs, "documentoid"), rs.getInt("numero"),
                rs.getString("formato"), rs.getString("conteudooriginal"), rs.getString("hashconteudo"),
                horario(rs, "recebidaem"), uuid(rs, "recebidaporid"), rs.getString("origemexterna"));
    }

    private DadosItemDocumentalPersistente mapearItem(ResultSet rs, int linha) throws SQLException {
        Array fontes = rs.getArray("fontesrastreabilidade");
        List<String> valores = fontes == null
                ? List.of()
                : Arrays.stream((Object[]) fontes.getArray()).map(Object::toString).toList();
        return new DadosItemDocumentalPersistente(uuid(rs, "id"), uuid(rs, "versaoid"),
                rs.getString("identificador"), rs.getString("titulo"), rs.getString("conteudo"),
                rs.getInt("ordem"), valores);
    }

    private static UUID uuid(ResultSet rs, String coluna) throws SQLException {
        Object valor = rs.getObject(coluna);
        return valor instanceof UUID uuid ? uuid : UUID.fromString(valor.toString());
    }

    private static OffsetDateTime horario(ResultSet rs, String coluna) throws SQLException {
        Object valor = rs.getObject(coluna);
        if (valor instanceof OffsetDateTime horario) {
            return horario;
        }
        return rs.getTimestamp(coluna).toInstant().atOffset(OffsetDateTime.now().getOffset());
    }
}
