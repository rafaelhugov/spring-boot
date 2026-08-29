package br.gov.mppb.athena.persistencia;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.ConnectionCallback;
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
        this.jdbc = new JdbcTemplate(Objects.requireNonNull(
                Objects.requireNonNull(jdbc, "jdbc deve ser informado.").getDataSource(),
                "fonte de dados deve ser informada."));
        this.transacao = new TransactionTemplate(Objects.requireNonNull(
                gerenciadorTransacao, "gerenciador de transacao deve ser informado."));
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
            validarVinculoNovaVersao(versao);
            inserirVersao(versao);
            itens.forEach(this::inserirItem);
            vincularPedidoAjusteAVersaoResultante(versao, ocorridoEm);
            jdbc.update("""
                    INSERT INTO athena.eventoauditoria
                        (id, projetoid, documentoid, versaoid, usuarioid, tipo, dados, ocorridoem, registradoporid)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, eventoAuditoriaId, projetoId, versao.documentoId(), versao.id(), usuarioId,
                    "NOVA_VERSAO_DOCUMENTAL_REGISTRADA", "{}", ocorridoEm, usuarioId);
        });
    }

    public void registrarDocumentoComPrimeiraVersao(
            UUID projetoId,
            UUID usuarioId,
            DadosDocumentoPersistente documento,
            DadosVersaoDocumentoPersistente versao,
            List<DadosItemDocumentalPersistente> itens,
            OffsetDateTime ocorridoEm,
            UUID eventoAuditoriaId
    ) {
        transacao.executeWithoutResult(status -> {
            inserirDocumento(documento);
            inserirVersao(versao);
            itens.forEach(this::inserirItem);
            jdbc.update("""
                    INSERT INTO athena.eventoauditoria
                        (id, projetoid, documentoid, versaoid, usuarioid, tipo, dados, ocorridoem, registradoporid)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, eventoAuditoriaId, projetoId, documento.id(), versao.id(), usuarioId,
                    "HISTORICO_DOCUMENTAL_REGISTRADO", "{}", ocorridoEm, usuarioId);
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

    public void registrarLeituraTecnica(
            UUID projetoId,
            UUID documentoId,
            UUID itemId,
            UUID usuarioId,
            String tipo,
            OffsetDateTime ocorridoEm
    ) {
        transacao.executeWithoutResult(status -> jdbc.update("""
                INSERT INTO athena.eventoauditoria
                    (id, projetoid, documentoid, itemid, usuarioid, tipo, dados, ocorridoem, registradoporid)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), projetoId, documentoId, itemId, usuarioId, tipo, "{}", ocorridoEm, usuarioId));
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
                """, this::mapearProjeto, Objects.requireNonNull(documento, "documento deve existir.").projetoId());
        List<DadosVersaoDocumentoPersistente> versoes = jdbc.query("""
                SELECT id, documentoid, numero, formato, conteudooriginal, hashconteudo,
                       recebidaem, recebidaporid, origemexterna, versaoanteriorid,
                       confirmada, pedidoajusteorigemid
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

    public Optional<DadosProjetoPersistente> consultarProjetoPorIdentificador(String identificadorProjeto) {
        List<DadosProjetoPersistente> projetos = jdbc.query("""
                SELECT id, identificador, nome, descricao, situacao, criadoem, criadoporid
                  FROM athena.projeto
                 WHERE identificador = ?
                """, this::mapearProjeto, identificadorProjeto);
        return projetos.stream().findFirst();
    }

    public List<DadosParticipanteProjetoPersistente> listarParticipantesProjeto(UUID projetoId) {
        return jdbc.query("""
                SELECT u.id, u.identificador, u.nome, u.login, u.unidade, v.papel
                  FROM athena.vinculoprojeto v
                  JOIN athena.usuarioinstitucional u ON u.id = v.usuarioid
                 WHERE v.projetoid = ?
                   AND v.fimem IS NULL
                 ORDER BY u.nome, v.papel
                """, (rs, linha) -> new DadosParticipanteProjetoPersistente(
                uuid(rs, "id"),
                rs.getString("identificador"),
                rs.getString("nome"),
                rs.getString("login"),
                rs.getString("unidade"),
                rs.getString("papel")
        ), projetoId);
    }

    public List<DadosDocumentoPersistente> listarDocumentosProjeto(UUID projetoId) {
        return jdbc.query("""
                SELECT id, identificador, projetoid, titulo, grupo, categoria, finalidade,
                       apresentacao, criadoem, criadoporid
                  FROM athena.documento
                 WHERE projetoid = ?
                 ORDER BY grupo, categoria, titulo
                """, this::mapearDocumento, projetoId);
    }

    public Optional<DadosDocumentoPersistente> consultarDocumento(UUID projetoId, String identificadorDocumento) {
        List<DadosDocumentoPersistente> documentos = jdbc.query("""
                SELECT id, identificador, projetoid, titulo, grupo, categoria, finalidade,
                       apresentacao, criadoem, criadoporid
                  FROM athena.documento
                 WHERE projetoid = ?
                   AND identificador = ?
                """, this::mapearDocumento, projetoId, identificadorDocumento);
        return documentos.stream().findFirst();
    }

    public List<DadosVersaoDocumentoPersistente> listarVersoesDocumento(UUID documentoId) {
        return jdbc.query("""
                SELECT id, documentoid, numero, formato, conteudooriginal, hashconteudo,
                       recebidaem, recebidaporid, origemexterna, versaoanteriorid,
                       confirmada, pedidoajusteorigemid
                  FROM athena.versaodocumento
                 WHERE documentoid = ?
                 ORDER BY numero
                """, this::mapearVersao, documentoId);
    }

    public List<DadosItemDocumentalPersistente> listarItensDocumento(UUID documentoId) {
        return jdbc.query("""
                SELECT i.id, i.versaoid, i.identificador, i.titulo, i.conteudo, i.ordem,
                       i.fontesrastreabilidade
                  FROM athena.itemdocumental i
                  JOIN athena.versaodocumento v ON v.id = i.versaoid
                 WHERE v.documentoid = ?
                 ORDER BY v.numero, i.ordem
                """, this::mapearItem, documentoId);
    }

    public Optional<DadosItemDocumentalPersistente> consultarItem(UUID documentoId, String identificadorItem) {
        List<DadosItemDocumentalPersistente> itens = jdbc.query("""
                SELECT i.id, i.versaoid, i.identificador, i.titulo, i.conteudo, i.ordem,
                       i.fontesrastreabilidade
                  FROM athena.itemdocumental i
                  JOIN athena.versaodocumento v ON v.id = i.versaoid
                 WHERE v.documentoid = ?
                   AND i.identificador = ?
                 ORDER BY v.numero DESC, i.ordem
                """, this::mapearItem, documentoId, identificadorItem);
        return itens.stream().findFirst();
    }

    public List<Map<String, Object>> listarEventosAuditoriaDocumento(UUID documentoId) {
        return jdbc.query("""
                SELECT e.id, e.tipo, e.decisao, e.justificativa, e.ocorridoem,
                       u.identificador AS usuarioidentificador, u.login AS usuariologin,
                       i.identificador AS itemidentificador, d.identificador AS documentoidentificador
                  FROM athena.eventoauditoria e
                  LEFT JOIN athena.usuarioinstitucional u ON u.id = e.usuarioid
                  LEFT JOIN athena.itemdocumental i ON i.id = e.itemid
                  LEFT JOIN athena.versaodocumento v ON v.id = i.versaoid
                  LEFT JOIN athena.documento d ON d.id = COALESCE(e.documentoid, v.documentoid)
                 WHERE d.id = ?
                 ORDER BY e.ocorridoem, e.id
                """, (rs, linha) -> Map.of(
                "id", rs.getString("id"),
                "tipo", rs.getString("tipo"),
                "decisao", valorOuVazio(rs.getString("decisao")),
                "justificativa", valorOuVazio(rs.getString("justificativa")),
                "ocorridoEm", horario(rs, "ocorridoem"),
                "usuario", Map.of(
                        "identificador", valorOuVazio(rs.getString("usuarioidentificador")),
                        "login", valorOuVazio(rs.getString("usuariologin"))
                ),
                "documento", valorOuVazio(rs.getString("documentoidentificador")),
                "item", valorOuVazio(rs.getString("itemidentificador"))
        ), documentoId);
    }

    public List<Map<String, Object>> listarEventosAuditoriaProjeto(UUID projetoId, String identificadorDocumento) {
        Object[] argumentos = identificadorDocumento == null || identificadorDocumento.isBlank()
                ? new Object[] {projetoId, projetoId}
                : new Object[] {projetoId, projetoId, identificadorDocumento};
        String filtroDocumento = identificadorDocumento == null || identificadorDocumento.isBlank()
                ? ""
                : " AND d.identificador = ?\n";
        String consulta = """
                SELECT e.id, e.tipo, e.decisao, e.justificativa, e.ocorridoem,
                       p.identificador AS projetoidentificador,
                       d.identificador AS documentoidentificador,
                       i.identificador AS itemidentificador,
                       u.login AS usuariologin
                  FROM athena.eventoauditoria e
                  LEFT JOIN athena.itemdocumental i ON i.id = e.itemid
                  LEFT JOIN athena.versaodocumento v ON v.id = i.versaoid
                  LEFT JOIN athena.documento d ON d.id = COALESCE(e.documentoid, v.documentoid)
                  LEFT JOIN athena.projeto p ON p.id = COALESCE(e.projetoid, d.projetoid)
                  LEFT JOIN athena.usuarioinstitucional u ON u.id = e.usuarioid
                 WHERE (e.projetoid = ? OR d.projetoid = ?)
                """ + filtroDocumento + """
                 ORDER BY e.ocorridoem, e.id
                """;
        return jdbc.query(consulta, (rs, linha) -> Map.of(
                "correlacao", rs.getString("id"),
                "autorTecnico", valorOuVazio(rs.getString("usuariologin")),
                "projeto", valorOuVazio(rs.getString("projetoidentificador")),
                "documento", valorOuVazio(rs.getString("documentoidentificador")),
                "recurso", recursoAuditado(rs),
                "acao", rs.getString("tipo"),
                "resultado", "REGISTRADO",
                "detalhes", Map.of(
                        "decisao", valorOuVazio(rs.getString("decisao")),
                        "justificativa", valorOuVazio(rs.getString("justificativa"))
                ),
                "ocorridoEm", horario(rs, "ocorridoem"),
                "politicaRetencao", "PENDENTE_DEFINICAO_INSTITUCIONAL"
        ), argumentos);
    }

    public List<Map<String, String>> listarRelacoesDocumento(UUID documentoId) {
        return listarRelacoes("documentoid", documentoId);
    }

    public List<Map<String, String>> listarRelacoesItem(UUID itemId) {
        return listarRelacoes("itemid", itemId);
    }

    public Map<String, Long> calcularIndicadoresProjeto(UUID projetoId) {
        long documentos = valorContador(jdbc.queryForObject("""
                SELECT COUNT(*) FROM athena.documento WHERE projetoid = ?
                """, Long.class, projetoId));
        long versoes = valorContador(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM athena.versaodocumento v
                  JOIN athena.documento d ON d.id = v.documentoid
                 WHERE d.projetoid = ?
                """, Long.class, projetoId));
        long participantes = valorContador(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM athena.vinculoprojeto
                 WHERE projetoid = ?
                   AND fimem IS NULL
                """, Long.class, projetoId));
        long interacoes = valorContador(jdbc.queryForObject("""
                SELECT
                    (SELECT COUNT(*) FROM athena.avaliacao a
                      JOIN athena.itemdocumental i ON i.id = a.itemid
                      JOIN athena.versaodocumento v ON v.id = i.versaoid
                      JOIN athena.documento d ON d.id = v.documentoid
                     WHERE d.projetoid = ?)
                  + (SELECT COUNT(*) FROM athena.comentario c
                      JOIN athena.itemdocumental i ON i.id = c.itemid
                      JOIN athena.versaodocumento v ON v.id = i.versaoid
                      JOIN athena.documento d ON d.id = v.documentoid
                     WHERE d.projetoid = ?)
                  + (SELECT COUNT(*) FROM athena.pedidoajuste p
                      JOIN athena.itemdocumental i ON i.id = p.itemid
                      JOIN athena.versaodocumento v ON v.id = i.versaoid
                      JOIN athena.documento d ON d.id = v.documentoid
                     WHERE d.projetoid = ?)
                  + (SELECT COUNT(*) FROM athena.confirmacao f
                      JOIN athena.itemdocumental i ON i.id = f.itemid
                      JOIN athena.versaodocumento v ON v.id = i.versaoid
                      JOIN athena.documento d ON d.id = v.documentoid
                     WHERE d.projetoid = ?)
                """, Long.class, projetoId, projetoId, projetoId, projetoId));
        return Map.of(
                "documentos", documentos,
                "versoes", versoes,
                "participantes", participantes,
                "interacoes", interacoes
        );
    }

    private List<Map<String, String>> listarRelacoes(String coluna, UUID recursoId) {
        String consulta = """
                SELECT tipo, recursorelacionado
                  FROM athena.relacaorecebida
                """ + " WHERE " + coluna + " = ?\n" + """
                 ORDER BY tipo, recursorelacionado
                """;
        return jdbc.query(consulta, (rs, linha) -> Map.of(
                "tipo", rs.getString("tipo"),
                "recursoRelacionado", rs.getString("recursorelacionado")
        ), recursoId);
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
                     recebidaem, recebidaporid, origemexterna, versaoanteriorid,
                     confirmada, pedidoajusteorigemid)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, versao.id(), versao.documentoId(), versao.numero(), versao.formato(),
                versao.conteudoOriginal(), versao.hashConteudo(), versao.recebidaEm(),
                versao.recebidaPorId(), versao.origemExterna(), versao.versaoAnteriorId(),
                versao.confirmada(), versao.pedidoAjusteOrigemId());
    }

    private void inserirItem(DadosItemDocumentalPersistente item) {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            Array fontes = connection.createArrayOf("VARCHAR", item.fontesRastreabilidade().toArray());
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO athena.itemdocumental
                        (id, versaoid, identificador, titulo, conteudo, ordem, fontesrastreabilidade)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """)) {
                ps.setObject(1, item.id());
                ps.setObject(2, item.versaoId());
                ps.setString(3, item.identificador());
                ps.setString(4, item.titulo());
                ps.setString(5, item.conteudo());
                ps.setInt(6, item.ordem());
                ps.setArray(7, fontes);
                ps.executeUpdate();
            } finally {
                fontes.free();
            }
            return null;
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
                    (id, itemid, solicitanteid, descricao, justificativa, situacao, solicitadoem, versaoresultanteid)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, registro.id(), registro.itemId(), registro.usuarioId(), registro.conteudo(),
                registro.justificativa(), registro.decisao(), registro.registradaEm(), null);
    }

    private void inserirConfirmacao(RegistroInteracaoHumana registro) {
        jdbc.update("""
                INSERT INTO athena.confirmacao (id, itemid, responsavelid, decisao, justificativa, confirmadaem)
                VALUES (?, ?, ?, ?, ?, ?)
                """, registro.id(), registro.itemId(), registro.usuarioId(), registro.decisao(),
                registro.justificativa(), registro.registradaEm());
        jdbc.update("""
                UPDATE athena.versaodocumento
                   SET confirmada = TRUE
                 WHERE id = (SELECT versaoid FROM athena.itemdocumental WHERE id = ?)
                """, registro.itemId());
    }

    private void vincularPedidoAjusteAVersaoResultante(
            DadosVersaoDocumentoPersistente versao,
            OffsetDateTime ocorridoEm
    ) {
        if (versao.pedidoAjusteOrigemId() == null) {
            return;
        }
        int atualizados = jdbc.update("""
                UPDATE athena.pedidoajuste
                   SET versaoresultanteid = ?, concluidoem = ?
                 WHERE id = ?
                """, versao.id(), ocorridoEm, versao.pedidoAjusteOrigemId());
        if (atualizados != 1) {
            throw new IllegalArgumentException("pedido de ajuste origem deve existir para vincular nova versao.");
        }
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
                horario(rs, "recebidaem"), uuid(rs, "recebidaporid"), rs.getString("origemexterna"),
                uuidOpcional(rs, "versaoanteriorid"), rs.getBoolean("confirmada"),
                uuidOpcional(rs, "pedidoajusteorigemid"));
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

    private static UUID uuidOpcional(ResultSet rs, String coluna) throws SQLException {
        Object valor = rs.getObject(coluna);
        if (valor == null) {
            return null;
        }
        return valor instanceof UUID uuid ? uuid : UUID.fromString(valor.toString());
    }

    private void validarVinculoNovaVersao(DadosVersaoDocumentoPersistente versao) {
        if (versao.numero() <= 1 || versao.versaoAnteriorId() == null) {
            throw new IllegalArgumentException("nova versao documental deve referenciar versao anterior existente.");
        }
        Boolean existe = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                      FROM athena.versaodocumento
                     WHERE id = ?
                       AND documentoid = ?
                       AND numero = ?
                )
                """, Boolean.class, versao.versaoAnteriorId(), versao.documentoId(), versao.numero() - 1);
        if (!Boolean.TRUE.equals(existe)) {
            throw new IllegalArgumentException("versao anterior da nova versao documental deve existir no mesmo documento.");
        }
    }

    private static OffsetDateTime horario(ResultSet rs, String coluna) throws SQLException {
        Object valor = rs.getObject(coluna);
        if (valor instanceof OffsetDateTime horario) {
            return horario;
        }
        return rs.getTimestamp(coluna).toInstant().atOffset(OffsetDateTime.now().getOffset());
    }

    private static String valorOuVazio(String valor) {
        return valor == null ? "" : valor;
    }

    private static long valorContador(Long valor) {
        return valor == null ? 0L : valor;
    }

    private static String recursoAuditado(ResultSet rs) throws SQLException {
        String item = rs.getString("itemidentificador");
        if (item != null && !item.isBlank()) {
            return item;
        }
        String documento = rs.getString("documentoidentificador");
        if (documento != null && !documento.isBlank()) {
            return documento;
        }
        return valorOuVazio(rs.getString("projetoidentificador"));
    }
}
