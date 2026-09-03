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

    public DadosProjetoPersistente criarProjetoComResponsavel(
            DadosUsuarioPersistente usuario,
            DadosProjetoPersistente projeto,
            DadosVinculoProjetoPersistente vinculo,
            OffsetDateTime dataOcorrencia,
            UUID eventoAuditoriaId
    ) {
        transacao.executeWithoutResult(status -> {
            inserirUsuario(usuario);
            inserirProjeto(projeto);
            inserirVinculo(vinculo);
            jdbc.update("""
                    INSERT INTO athena.eventoauditoria
                        (id, id_projeto, id_usuario, tipo, dados, dataocorrencia, id_usuarioregistro)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, eventoAuditoriaId, projeto.id(), usuario.id(), "PROJETO_CRIADO", "{}", dataOcorrencia, usuario.id());
        });
        return projeto;
    }

    public DadosProjetoPersistente atualizarProjeto(
            String identificador,
            String nome,
            String descricao,
            String situacao,
            UUID idUsuarioAlteracao,
            OffsetDateTime dataAlteracao
    ) {
        transacao.executeWithoutResult(status -> jdbc.update("""
                UPDATE athena.projeto
                   SET nome = ?,
                       descricao = ?,
                       situacao = ?,
                       id_usuarioalteracao = ?,
                       dataalteracao = ?
                 WHERE identificador = ?
                """, nome, descricao, situacao, idUsuarioAlteracao, dataAlteracao, identificador));
        return consultarProjetoPorIdentificador(identificador)
                .orElseThrow(() -> new IllegalArgumentException("projeto deve existir para atualizacao."));
    }

    public DadosParticipanteProjetoPersistente vincularParticipante(
            UUID idProjeto,
            DadosUsuarioPersistente usuario,
            String papel,
            String unidade,
            UUID idUsuarioRegistro,
            OffsetDateTime dataInicio
    ) {
        transacao.executeWithoutResult(status -> {
            inserirUsuario(usuario);
            jdbc.update("""
                    INSERT INTO athena.vinculoprojeto
                        (id, id_projeto, id_usuario, papel, unidade, datainicio, id_usuarioregistro)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, UUID.randomUUID(), idProjeto, usuario.id(), papel, unidade, dataInicio, idUsuarioRegistro);
            jdbc.update("""
                    INSERT INTO athena.eventoauditoria
                        (id, id_projeto, id_usuario, tipo, dados, dataocorrencia, id_usuarioregistro)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, UUID.randomUUID(), idProjeto, usuario.id(), "PARTICIPANTE_VINCULADO", "{}", dataInicio, idUsuarioRegistro);
        });
        return new DadosParticipanteProjetoPersistente(usuario.id(), usuario.identificador(), usuario.nome(), usuario.login(), unidade, papel);
    }

    public void registrarNovaVersaoDocumental(
            UUID idProjeto,
            UUID idUsuario,
            DadosVersaoDocumentoPersistente versao,
            List<DadosItemDocumentalPersistente> itens,
            OffsetDateTime dataOcorrencia,
            UUID eventoAuditoriaId
    ) {
        transacao.executeWithoutResult(status -> {
            validarVinculoNovaVersao(versao);
            inserirVersao(versao);
            itens.forEach(this::inserirItem);
            vincularPedidoAjusteAVersaoResultante(versao, dataOcorrencia);
            jdbc.update("""
                    INSERT INTO athena.eventoauditoria
                        (id, id_projeto, id_documento, id_versao, id_usuario, tipo, dados, dataocorrencia, id_usuarioregistro)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, eventoAuditoriaId, idProjeto, versao.idDocumento(), versao.id(), idUsuario,
                    "NOVA_VERSAO_DOCUMENTAL_REGISTRADA", "{}", dataOcorrencia, idUsuario);
        });
    }

    public void registrarDocumentoComPrimeiraVersao(
            UUID idProjeto,
            UUID idUsuario,
            DadosDocumentoPersistente documento,
            DadosVersaoDocumentoPersistente versao,
            List<DadosItemDocumentalPersistente> itens,
            OffsetDateTime dataOcorrencia,
            UUID eventoAuditoriaId
    ) {
        transacao.executeWithoutResult(status -> {
            inserirDocumento(documento);
            inserirVersao(versao);
            itens.forEach(this::inserirItem);
            jdbc.update("""
                    INSERT INTO athena.eventoauditoria
                        (id, id_projeto, id_documento, id_versao, id_usuario, tipo, dados, dataocorrencia, id_usuarioregistro)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, eventoAuditoriaId, idProjeto, documento.id(), versao.id(), idUsuario,
                    "HISTORICO_DOCUMENTAL_REGISTRADO", "{}", dataOcorrencia, idUsuario);
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

    public void tratarPedidoAjuste(
            UUID pedidoId,
            String decisao,
            String justificativaTratamento,
            OffsetDateTime dataTratamento,
            UUID idResponsavel,
            UUID eventoAuditoriaId
    ) {
        transacao.executeWithoutResult(status -> {
            DadosPedidoAjustePersistente pedido = consultarPedidoAjuste(pedidoId)
                    .orElseThrow(() -> new IllegalArgumentException("pedido de ajuste deve existir para tratamento."));
            jdbc.update("""
                    UPDATE athena.pedidoajuste
                       SET situacao = ?,
                           datatratamento = ?,
                           decisaotratamento = ?,
                           justificativatratamento = ?
                     WHERE id = ?
                    """, decisao, dataTratamento, decisao, justificativaTratamento, pedidoId);
            jdbc.update("""
                    INSERT INTO athena.eventoauditoria
                        (id, id_item, id_usuario, tipo, decisao, justificativa, dados, dataocorrencia, id_usuarioregistro)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, eventoAuditoriaId, pedido.idItem(), idResponsavel, "TRATAMENTO_PEDIDO_AJUSTE",
                    decisao, justificativaTratamento, "{}", dataTratamento, idResponsavel);
        });
    }

    public void vincularVersaoResultanteAoPedidoAjuste(UUID pedidoId, UUID idVersaoResultante) {
        transacao.executeWithoutResult(status -> jdbc.update("""
                UPDATE athena.pedidoajuste
                   SET id_versao_resultante = ?
                 WHERE id = ?
                """, idVersaoResultante, pedidoId));
    }

    public void confirmarVersaoDocumental(UUID idVersao) {
        transacao.executeWithoutResult(status -> jdbc.update("""
                UPDATE athena.versaodocumento
                   SET confirmada = TRUE
                 WHERE id = ?
                """, idVersao));
    }

    public void registrarLeituraTecnica(
            UUID idProjeto,
            UUID idDocumento,
            UUID idItem,
            UUID idUsuario,
            String tipo,
            OffsetDateTime dataOcorrencia
    ) {
        transacao.executeWithoutResult(status -> jdbc.update("""
                INSERT INTO athena.eventoauditoria
                    (id, id_projeto, id_documento, id_item, id_usuario, tipo, dados, dataocorrencia, id_usuarioregistro)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), idProjeto, idDocumento, idItem, idUsuario, tipo, "{}", dataOcorrencia, idUsuario));
    }

    public void atualizarSituacaoProjeto(UUID idProjeto, String situacao, UUID idUsuarioAlteracao, OffsetDateTime dataAlteracao) {
        transacao.executeWithoutResult(status -> jdbc.update("""
                UPDATE athena.projeto
                   SET situacao = ?, id_usuarioalteracao = ?, dataalteracao = ?
                 WHERE id = ?
                """, situacao, idUsuarioAlteracao, dataAlteracao, idProjeto));
    }

    public HistoricoDocumentoPersistente consultarHistoricoDocumento(UUID idDocumento) {
        DadosDocumentoPersistente documento = jdbc.queryForObject("""
                SELECT id, identificador, id_projeto, titulo, grupo, categoria, finalidade,
                       apresentacao, datacriacao, id_usuariocriacao
                  FROM athena.documento
                 WHERE id = ?
                """, this::mapearDocumento, idDocumento);
        DadosProjetoPersistente projeto = jdbc.queryForObject("""
                SELECT id, identificador, nome, descricao, situacao, datacriacao, id_usuariocriacao
                  FROM athena.projeto
                 WHERE id = ?
                """, this::mapearProjeto, Objects.requireNonNull(documento, "documento deve existir.").idProjeto());
        List<DadosVersaoDocumentoPersistente> versoes = jdbc.query("""
                SELECT id, id_documento, numero, formato, conteudooriginal, hashconteudo,
                       datarecebimento, id_usuariorecebimento, origemexterna, id_versao_anterior,
                       confirmada, id_pedido_ajuste_origem
                  FROM athena.versaodocumento
                 WHERE id_documento = ?
                 ORDER BY numero
                """, this::mapearVersao, idDocumento);
        List<DadosItemDocumentalPersistente> itens = jdbc.query("""
                SELECT i.id, i.id_versao, i.identificador, i.titulo, i.conteudo, i.ordem,
                       i.fontesrastreabilidade
                  FROM athena.itemdocumental i
                  JOIN athena.versaodocumento v ON v.id = i.id_versao
                 WHERE v.id_documento = ?
                 ORDER BY v.numero, i.ordem
                """, this::mapearItem, idDocumento);

        return new HistoricoDocumentoPersistente(projeto, documento, versoes, itens);
    }

    public Optional<DadosProjetoPersistente> consultarProjetoPorIdentificador(String identificadorProjeto) {
        List<DadosProjetoPersistente> projetos = jdbc.query("""
                SELECT id, identificador, nome, descricao, situacao, datacriacao, id_usuariocriacao
                  FROM athena.projeto
                 WHERE identificador = ?
                """, this::mapearProjeto, identificadorProjeto);
        return projetos.stream().findFirst();
    }

    public List<DadosProjetoPersistente> listarProjetos() {
        return jdbc.query("""
                SELECT id, identificador, nome, descricao, situacao, datacriacao, id_usuariocriacao
                  FROM athena.projeto
                 ORDER BY nome, identificador
                """, this::mapearProjeto);
    }

    public List<DadosProjetoPersistente> listarProjetosPorLogin(String login) {
        return jdbc.query("""
                SELECT DISTINCT p.id, p.identificador, p.nome, p.descricao, p.situacao, p.datacriacao, p.id_usuariocriacao
                  FROM athena.projeto p
                  JOIN athena.vinculoprojeto v ON v.id_projeto = p.id
                  JOIN athena.usuarioinstitucional u ON u.id = v.id_usuario
                 WHERE u.login = ?
                   AND v.datafim IS NULL
                 ORDER BY p.nome, p.identificador
                """, this::mapearProjeto, login);
    }

    public Optional<DadosUsuarioPersistente> consultarUsuarioPorLogin(String login) {
        List<DadosUsuarioPersistente> usuarios = jdbc.query("""
                SELECT id, identificador, nome, login, unidade
                  FROM athena.usuarioinstitucional
                 WHERE login = ?
                """, this::mapearUsuario, login);
        return usuarios.stream().findFirst();
    }

    public List<DadosParticipanteProjetoPersistente> listarParticipantesProjeto(UUID idProjeto) {
        return jdbc.query("""
                SELECT u.id, u.identificador, u.nome, u.login, u.unidade, v.papel
                  FROM athena.vinculoprojeto v
                  JOIN athena.usuarioinstitucional u ON u.id = v.id_usuario
                 WHERE v.id_projeto = ?
                   AND v.datafim IS NULL
                 ORDER BY u.nome, v.papel
                """, (rs, linha) -> new DadosParticipanteProjetoPersistente(
                uuid(rs, "id"),
                rs.getString("identificador"),
                rs.getString("nome"),
                rs.getString("login"),
                rs.getString("unidade"),
                rs.getString("papel")
        ), idProjeto);
    }

    public List<DadosDocumentoPersistente> listarDocumentosProjeto(UUID idProjeto) {
        return jdbc.query("""
                SELECT id, identificador, id_projeto, titulo, grupo, categoria, finalidade,
                       apresentacao, datacriacao, id_usuariocriacao
                  FROM athena.documento
                 WHERE id_projeto = ?
                 ORDER BY grupo, categoria, titulo
                """, this::mapearDocumento, idProjeto);
    }

    public Optional<DadosDocumentoPersistente> consultarDocumento(UUID idProjeto, String identificadorDocumento) {
        List<DadosDocumentoPersistente> documentos = jdbc.query("""
                SELECT id, identificador, id_projeto, titulo, grupo, categoria, finalidade,
                       apresentacao, datacriacao, id_usuariocriacao
                  FROM athena.documento
                 WHERE id_projeto = ?
                   AND identificador = ?
                """, this::mapearDocumento, idProjeto, identificadorDocumento);
        return documentos.stream().findFirst();
    }

    public List<DadosVersaoDocumentoPersistente> listarVersoesDocumento(UUID idDocumento) {
        return jdbc.query("""
                SELECT id, id_documento, numero, formato, conteudooriginal, hashconteudo,
                       datarecebimento, id_usuariorecebimento, origemexterna, id_versao_anterior,
                       confirmada, id_pedido_ajuste_origem
                  FROM athena.versaodocumento
                 WHERE id_documento = ?
                 ORDER BY numero
                """, this::mapearVersao, idDocumento);
    }

    public Optional<DadosVersaoDocumentoPersistente> consultarVersaoDocumento(UUID idDocumento, int numero) {
        List<DadosVersaoDocumentoPersistente> versoes = jdbc.query("""
                SELECT id, id_documento, numero, formato, conteudooriginal, hashconteudo,
                       datarecebimento, id_usuariorecebimento, origemexterna, id_versao_anterior,
                       confirmada, id_pedido_ajuste_origem
                  FROM athena.versaodocumento
                 WHERE id_documento = ?
                   AND numero = ?
                """, this::mapearVersao, idDocumento, numero);
        return versoes.stream().findFirst();
    }

    public List<DadosItemDocumentalPersistente> listarItensDocumento(UUID idDocumento) {
        return jdbc.query("""
                SELECT i.id, i.id_versao, i.identificador, i.titulo, i.conteudo, i.ordem,
                       i.fontesrastreabilidade
                  FROM athena.itemdocumental i
                  JOIN athena.versaodocumento v ON v.id = i.id_versao
                 WHERE v.id_documento = ?
                 ORDER BY v.numero, i.ordem
                """, this::mapearItem, idDocumento);
    }

    public Optional<DadosItemDocumentalPersistente> consultarItem(UUID idDocumento, String identificadorItem) {
        List<DadosItemDocumentalPersistente> itens = jdbc.query("""
                SELECT i.id, i.id_versao, i.identificador, i.titulo, i.conteudo, i.ordem,
                       i.fontesrastreabilidade
                  FROM athena.itemdocumental i
                  JOIN athena.versaodocumento v ON v.id = i.id_versao
                 WHERE v.id_documento = ?
                   AND i.identificador = ?
                 ORDER BY v.numero DESC, i.ordem
                """, this::mapearItem, idDocumento, identificadorItem);
        return itens.stream().findFirst();
    }

    public Optional<DadosPedidoAjustePersistente> consultarPedidoAjuste(UUID pedidoId) {
        List<DadosPedidoAjustePersistente> pedidos = jdbc.query("""
                SELECT id, id_item, id_solicitante, descricao, justificativa, situacao,
                       datasolicitacao, responsaveltecnico, datatratamento, decisaotratamento,
                       justificativatratamento, id_versao_resultante
                  FROM athena.pedidoajuste
                 WHERE id = ?
                """, this::mapearPedidoAjuste, pedidoId);
        return pedidos.stream().findFirst();
    }

    public List<Map<String, Object>> listarEventosAuditoriaDocumento(UUID idDocumento) {
        return jdbc.query("""
                SELECT e.id, e.tipo, e.decisao, e.justificativa, e.dataocorrencia,
                       u.identificador AS usuarioidentificador, u.login AS usuariologin,
                       i.identificador AS itemidentificador, d.identificador AS documentoidentificador
                  FROM athena.eventoauditoria e
                  LEFT JOIN athena.usuarioinstitucional u ON u.id = e.id_usuario
                  LEFT JOIN athena.itemdocumental i ON i.id = e.id_item
                  LEFT JOIN athena.versaodocumento v ON v.id = i.id_versao
                  LEFT JOIN athena.documento d ON d.id = COALESCE(e.id_documento, v.id_documento)
                 WHERE d.id = ?
                 ORDER BY e.dataocorrencia, e.id
                """, (rs, linha) -> Map.of(
                "id", rs.getString("id"),
                "tipo", rs.getString("tipo"),
                "decisao", valorOuVazio(rs.getString("decisao")),
                "justificativa", valorOuVazio(rs.getString("justificativa")),
                "dataOcorrencia", horario(rs, "dataocorrencia"),
                "usuario", Map.of(
                        "identificador", valorOuVazio(rs.getString("usuarioidentificador")),
                        "login", valorOuVazio(rs.getString("usuariologin"))
                ),
                "documento", valorOuVazio(rs.getString("documentoidentificador")),
                "item", valorOuVazio(rs.getString("itemidentificador"))
        ), idDocumento);
    }

    public List<Map<String, Object>> listarEventosAuditoriaProjeto(UUID idProjeto, String identificadorDocumento) {
        Object[] argumentos = identificadorDocumento == null || identificadorDocumento.isBlank()
                ? new Object[] {idProjeto, idProjeto}
                : new Object[] {idProjeto, idProjeto, identificadorDocumento};
        String filtroDocumento = identificadorDocumento == null || identificadorDocumento.isBlank()
                ? ""
                : " AND d.identificador = ?\n";
        String consulta = """
                SELECT e.id, e.tipo, e.decisao, e.justificativa, e.dataocorrencia,
                       p.identificador AS projetoidentificador,
                       d.identificador AS documentoidentificador,
                       i.identificador AS itemidentificador,
                       u.login AS usuariologin
                  FROM athena.eventoauditoria e
                  LEFT JOIN athena.itemdocumental i ON i.id = e.id_item
                  LEFT JOIN athena.versaodocumento v ON v.id = i.id_versao
                  LEFT JOIN athena.documento d ON d.id = COALESCE(e.id_documento, v.id_documento)
                  LEFT JOIN athena.projeto p ON p.id = COALESCE(e.id_projeto, d.id_projeto)
                  LEFT JOIN athena.usuarioinstitucional u ON u.id = e.id_usuario
                 WHERE (e.id_projeto = ? OR d.id_projeto = ?)
                """ + filtroDocumento + """
                 ORDER BY e.dataocorrencia, e.id
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
                "dataOcorrencia", horario(rs, "dataocorrencia"),
                "politicaRetencao", "PENDENTE_DEFINICAO_INSTITUCIONAL"
        ), argumentos);
    }

    public List<Map<String, String>> listarRelacoesDocumento(UUID idDocumento) {
        return listarRelacoes("id_documento", idDocumento);
    }

    public List<Map<String, String>> listarRelacoesItem(UUID idItem) {
        return listarRelacoes("id_item", idItem);
    }

    public Map<String, Long> calcularIndicadoresProjeto(UUID idProjeto) {
        long documentos = valorContador(jdbc.queryForObject("""
                SELECT COUNT(*) FROM athena.documento WHERE id_projeto = ?
                """, Long.class, idProjeto));
        long versoes = valorContador(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM athena.versaodocumento v
                  JOIN athena.documento d ON d.id = v.id_documento
                 WHERE d.id_projeto = ?
                """, Long.class, idProjeto));
        long participantes = valorContador(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM athena.vinculoprojeto
                 WHERE id_projeto = ?
                   AND datafim IS NULL
                """, Long.class, idProjeto));
        long interacoes = valorContador(jdbc.queryForObject("""
                SELECT
                    (SELECT COUNT(*) FROM athena.avaliacao a
                      JOIN athena.itemdocumental i ON i.id = a.id_item
                      JOIN athena.versaodocumento v ON v.id = i.id_versao
                      JOIN athena.documento d ON d.id = v.id_documento
                     WHERE d.id_projeto = ?)
                  + (SELECT COUNT(*) FROM athena.comentario c
                      JOIN athena.itemdocumental i ON i.id = c.id_item
                      JOIN athena.versaodocumento v ON v.id = i.id_versao
                      JOIN athena.documento d ON d.id = v.id_documento
                     WHERE d.id_projeto = ?)
                  + (SELECT COUNT(*) FROM athena.pedidoajuste p
                      JOIN athena.itemdocumental i ON i.id = p.id_item
                      JOIN athena.versaodocumento v ON v.id = i.id_versao
                      JOIN athena.documento d ON d.id = v.id_documento
                     WHERE d.id_projeto = ?)
                  + (SELECT COUNT(*) FROM athena.confirmacao f
                      JOIN athena.itemdocumental i ON i.id = f.id_item
                      JOIN athena.versaodocumento v ON v.id = i.id_versao
                      JOIN athena.documento d ON d.id = v.id_documento
                     WHERE d.id_projeto = ?)
                """, Long.class, idProjeto, idProjeto, idProjeto, idProjeto));
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
        consultarUsuarioPorLogin(usuario.login()).ifPresentOrElse(existente -> jdbc.update("""
                UPDATE athena.usuarioinstitucional
                   SET identificador = ?, nome = ?, unidade = ?
                 WHERE id = ?
                """, usuario.identificador(), usuario.nome(), usuario.unidade(), existente.id()), () -> jdbc.update("""
                INSERT INTO athena.usuarioinstitucional (id, identificador, nome, login, unidade)
                VALUES (?, ?, ?, ?, ?)
                """, usuario.id(), usuario.identificador(), usuario.nome(), usuario.login(), usuario.unidade()));
    }

    private void inserirProjeto(DadosProjetoPersistente projeto) {
        jdbc.update("""
                INSERT INTO athena.projeto (id, identificador, nome, descricao, situacao, datacriacao, id_usuariocriacao)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, projeto.id(), projeto.identificador(), projeto.nome(), projeto.descricao(),
                projeto.situacao(), projeto.dataCriacao(), projeto.idUsuarioCriacao());
    }

    private void inserirVinculo(DadosVinculoProjetoPersistente vinculo) {
        jdbc.update("""
                INSERT INTO athena.vinculoprojeto
                    (id, id_projeto, id_usuario, papel, unidade, datainicio, id_usuarioregistro)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, vinculo.id(), vinculo.idProjeto(), vinculo.idUsuario(), vinculo.papel(),
                vinculo.unidade(), vinculo.dataInicio(), vinculo.idUsuarioRegistro());
    }

    private void inserirDocumento(DadosDocumentoPersistente documento) {
        jdbc.update("""
                INSERT INTO athena.documento
                    (id, identificador, id_projeto, titulo, grupo, categoria, finalidade,
                     apresentacao, datacriacao, id_usuariocriacao)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, documento.id(), documento.identificador(), documento.idProjeto(), documento.titulo(),
                documento.grupo(), documento.categoria(), documento.finalidade(), documento.apresentacao(),
                documento.dataCriacao(), documento.idUsuarioCriacao());
    }

    private void inserirVersao(DadosVersaoDocumentoPersistente versao) {
        jdbc.update("""
                INSERT INTO athena.versaodocumento
                    (id, id_documento, numero, formato, conteudooriginal, hashconteudo,
                     datarecebimento, id_usuariorecebimento, origemexterna, id_versao_anterior,
                     confirmada, id_pedido_ajuste_origem)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, versao.id(), versao.idDocumento(), versao.numero(), versao.formato(),
                versao.conteudoOriginal(), versao.hashConteudo(), versao.dataRecebimento(),
                versao.idUsuarioRecebimento(), versao.origemExterna(), versao.idVersaoAnterior(),
                versao.confirmada(), versao.idPedidoAjusteOrigem());
    }

    private void inserirItem(DadosItemDocumentalPersistente item) {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            Array fontes = connection.createArrayOf("VARCHAR", item.fontesRastreabilidade().toArray());
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO athena.itemdocumental
                        (id, id_versao, identificador, titulo, conteudo, ordem, fontesrastreabilidade)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """)) {
                ps.setObject(1, item.id());
                ps.setObject(2, item.idVersao());
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
                INSERT INTO athena.avaliacao (id, id_item, id_responsavel, decisao, justificativa, dataavaliacao)
                VALUES (?, ?, ?, ?, ?, ?)
                """, registro.id(), registro.idItem(), registro.idUsuario(), registro.decisao(),
                registro.justificativa(), registro.dataRegistro());
    }

    private void inserirComentario(RegistroInteracaoHumana registro) {
        jdbc.update("""
                INSERT INTO athena.comentario (id, id_item, id_autor, conteudo, dataregistro, origem)
                VALUES (?, ?, ?, ?, ?, ?)
                """, registro.id(), registro.idItem(), registro.idUsuario(), registro.conteudo(),
                registro.dataRegistro(), "ATHENA");
    }

    private void inserirPedidoAjuste(RegistroInteracaoHumana registro) {
        jdbc.update("""
                INSERT INTO athena.pedidoajuste
                    (id, id_item, id_solicitante, descricao, justificativa, situacao, datasolicitacao,
                     responsaveltecnico, id_versao_resultante)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, registro.id(), registro.idItem(), registro.idUsuario(), registro.conteudo(),
                registro.justificativa(), registro.decisao(), registro.dataRegistro(), registro.conteudo(), null);
    }

    private void inserirConfirmacao(RegistroInteracaoHumana registro) {
        jdbc.update("""
                INSERT INTO athena.confirmacao (id, id_item, id_responsavel, decisao, justificativa, dataconfirmacao)
                VALUES (?, ?, ?, ?, ?, ?)
                """, registro.id(), registro.idItem(), registro.idUsuario(), registro.decisao(),
                registro.justificativa(), registro.dataRegistro());
        jdbc.update("""
                UPDATE athena.versaodocumento
                   SET confirmada = TRUE
                 WHERE id = (SELECT id_versao FROM athena.itemdocumental WHERE id = ?)
                """, registro.idItem());
    }

    private void vincularPedidoAjusteAVersaoResultante(
            DadosVersaoDocumentoPersistente versao,
            OffsetDateTime dataOcorrencia
    ) {
        if (versao.idPedidoAjusteOrigem() == null) {
            return;
        }
        int atualizados = jdbc.update("""
                UPDATE athena.pedidoajuste
                   SET id_versao_resultante = ?, dataconclusao = ?
                 WHERE id = ?
                """, versao.id(), dataOcorrencia, versao.idPedidoAjusteOrigem());
        if (atualizados != 1) {
            throw new IllegalArgumentException("pedido de ajuste origem deve existir para vincular nova versao.");
        }
    }

    private void inserirAuditoriaHistorico(RegistroHistoricoDocumental registro) {
        jdbc.update("""
                INSERT INTO athena.eventoauditoria
                    (id, id_projeto, id_documento, id_versao, id_usuario, tipo, dados, dataocorrencia, id_usuarioregistro)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, registro.eventoAuditoriaId(), registro.projeto().id(), registro.documento().id(),
                registro.versao().id(), registro.usuario().id(), "HISTORICO_DOCUMENTAL_REGISTRADO",
                "{}", registro.dataOcorrencia(), registro.usuario().id());
    }

    private void inserirAuditoriaInteracao(RegistroInteracaoHumana registro) {
        jdbc.update("""
                INSERT INTO athena.eventoauditoria
                    (id, id_item, id_usuario, tipo, decisao, justificativa, dados, dataocorrencia, id_usuarioregistro)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, registro.eventoAuditoriaId(), registro.idItem(), registro.idUsuario(),
                registro.tipo().name(), registro.decisao(), registro.justificativa(), "{}",
                registro.dataRegistro(), registro.idUsuario());
    }

    private DadosProjetoPersistente mapearProjeto(ResultSet rs, int linha) throws SQLException {
        return new DadosProjetoPersistente(uuid(rs, "id"), rs.getString("identificador"), rs.getString("nome"),
                rs.getString("descricao"), rs.getString("situacao"), horario(rs, "datacriacao"), uuid(rs, "id_usuariocriacao"));
    }

    private DadosUsuarioPersistente mapearUsuario(ResultSet rs, int linha) throws SQLException {
        return new DadosUsuarioPersistente(uuid(rs, "id"), rs.getString("identificador"), rs.getString("nome"),
                rs.getString("login"), rs.getString("unidade"));
    }

    private DadosDocumentoPersistente mapearDocumento(ResultSet rs, int linha) throws SQLException {
        return new DadosDocumentoPersistente(uuid(rs, "id"), rs.getString("identificador"), uuid(rs, "id_projeto"),
                rs.getString("titulo"), rs.getString("grupo"), rs.getString("categoria"),
                rs.getString("finalidade"), rs.getString("apresentacao"), horario(rs, "datacriacao"),
                uuid(rs, "id_usuariocriacao"));
    }

    private DadosVersaoDocumentoPersistente mapearVersao(ResultSet rs, int linha) throws SQLException {
        return new DadosVersaoDocumentoPersistente(uuid(rs, "id"), uuid(rs, "id_documento"), rs.getInt("numero"),
                rs.getString("formato"), rs.getString("conteudooriginal"), rs.getString("hashconteudo"),
                horario(rs, "datarecebimento"), uuid(rs, "id_usuariorecebimento"), rs.getString("origemexterna"),
                uuidOpcional(rs, "id_versao_anterior"), rs.getBoolean("confirmada"),
                uuidOpcional(rs, "id_pedido_ajuste_origem"));
    }

    private DadosPedidoAjustePersistente mapearPedidoAjuste(ResultSet rs, int linha) throws SQLException {
        return new DadosPedidoAjustePersistente(
                uuid(rs, "id"),
                uuid(rs, "id_item"),
                uuid(rs, "id_solicitante"),
                rs.getString("descricao"),
                rs.getString("justificativa"),
                rs.getString("situacao"),
                horario(rs, "datasolicitacao"),
                rs.getString("responsaveltecnico"),
                horarioOpcional(rs, "datatratamento"),
                rs.getString("decisaotratamento"),
                rs.getString("justificativatratamento"),
                uuidOpcional(rs, "id_versao_resultante")
        );
    }

    private DadosItemDocumentalPersistente mapearItem(ResultSet rs, int linha) throws SQLException {
        Array fontes = rs.getArray("fontesrastreabilidade");
        List<String> valores = fontes == null
                ? List.of()
                : Arrays.stream((Object[]) fontes.getArray()).map(Object::toString).toList();
        return new DadosItemDocumentalPersistente(uuid(rs, "id"), uuid(rs, "id_versao"),
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
        if (versao.numero() <= 1 || versao.idVersaoAnterior() == null) {
            throw new IllegalArgumentException("nova versao documental deve referenciar versao anterior existente.");
        }
        Boolean existe = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                      FROM athena.versaodocumento
                     WHERE id = ?
                       AND id_documento = ?
                       AND numero = ?
                       AND confirmada = TRUE
                )
                """, Boolean.class, versao.idVersaoAnterior(), versao.idDocumento(), versao.numero() - 1);
        if (!Boolean.TRUE.equals(existe)) {
            throw new IllegalArgumentException(
                    "versao anterior da nova versao documental deve existir confirmada no mesmo documento.");
        }
    }

    private static OffsetDateTime horario(ResultSet rs, String coluna) throws SQLException {
        Object valor = rs.getObject(coluna);
        if (valor instanceof OffsetDateTime horario) {
            return horario;
        }
        return rs.getTimestamp(coluna).toInstant().atOffset(OffsetDateTime.now().getOffset());
    }

    private static OffsetDateTime horarioOpcional(ResultSet rs, String coluna) throws SQLException {
        Object valor = rs.getObject(coluna);
        if (valor == null) {
            return null;
        }
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
