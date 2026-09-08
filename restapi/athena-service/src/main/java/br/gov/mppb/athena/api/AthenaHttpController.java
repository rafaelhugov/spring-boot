package br.gov.mppb.athena.api;

import br.gov.mppb.athena.aplicacao.AvaliacaoRegistrada;
import br.gov.mppb.athena.aplicacao.ComandoConfirmarEntendimento;
import br.gov.mppb.athena.aplicacao.ComandoRegistrarComentario;
import br.gov.mppb.athena.aplicacao.ComandoRegistrarPedidoAjuste;
import br.gov.mppb.athena.aplicacao.ComandoTratarPedidoAjuste;
import br.gov.mppb.athena.aplicacao.ComentarioRegistrado;
import br.gov.mppb.athena.aplicacao.ConfirmacaoEntendimentoRegistrada;
import br.gov.mppb.athena.aplicacao.ComandoRegistrarAvaliacao;
import br.gov.mppb.athena.aplicacao.DecisaoAvaliacao;
import br.gov.mppb.athena.aplicacao.EstadoSolicitacaoAjuste;
import br.gov.mppb.athena.aplicacao.PedidoAjusteRegistrado;
import br.gov.mppb.athena.aplicacao.ReferenciaAvaliacaoDocumental;
import br.gov.mppb.athena.aplicacao.ServicoAvaliacaoConfirmacao;
import br.gov.mppb.athena.dominio.Documento;
import br.gov.mppb.athena.dominio.FormatoDocumento;
import br.gov.mppb.athena.dominio.IdentificadorProjeto;
import br.gov.mppb.athena.dominio.IdentificadorUsuario;
import br.gov.mppb.athena.dominio.ItemDocumental;
import br.gov.mppb.athena.dominio.MetadadosDocumento;
import br.gov.mppb.athena.dominio.PapelProjeto;
import br.gov.mppb.athena.dominio.ParticipanteProjeto;
import br.gov.mppb.athena.dominio.PermissaoDocumento;
import br.gov.mppb.athena.dominio.ProjetoInstitucional;
import br.gov.mppb.athena.dominio.RastreabilidadeDocumento;
import br.gov.mppb.athena.dominio.RelacaoDocumental;
import br.gov.mppb.athena.dominio.SituacaoProjeto;
import br.gov.mppb.athena.dominio.UsuarioInstitucional;
import br.gov.mppb.athena.dominio.VersaoDocumento;
import br.gov.mppb.athena.integracao.ConsultaParticipantesCorporativos;
import br.gov.mppb.athena.integracao.FonteCorporativaParticipantesInstitucional;
import br.gov.mppb.athena.integracao.ServicoConsultaParticipantesCorporativos;
import br.gov.mppb.athena.persistencia.DadosDocumentoPersistente;
import br.gov.mppb.athena.persistencia.DadosItemDocumentalPersistente;
import br.gov.mppb.athena.persistencia.DadosPedidoAjustePersistente;
import br.gov.mppb.athena.persistencia.DadosParticipanteProjetoPersistente;
import br.gov.mppb.athena.persistencia.DadosProjetoPersistente;
import br.gov.mppb.athena.persistencia.DadosUsuarioPersistente;
import br.gov.mppb.athena.persistencia.DadosVersaoDocumentoPersistente;
import br.gov.mppb.athena.persistencia.DadosVinculoProjetoPersistente;
import br.gov.mppb.athena.persistencia.PersistenciaTransacionalAthena;
import br.gov.mppb.athena.persistencia.RegistroInteracaoHumana;
import br.gov.mppb.athena.persistencia.TipoInteracaoHumana;
import br.gov.mppb.athena.seguranca.ContextoAutorizacaoInstitucional;
import br.gov.mppb.athena.seguranca.OperacaoProjeto;
import br.gov.mppb.athena.seguranca.PoliticaSegurancaAthena;
import br.gov.mppb.athena.seguranca.ResultadoAutorizacao;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projetos")
public final class AthenaHttpController {

    private final ServicoAvaliacaoConfirmacao avaliacoes = new ServicoAvaliacaoConfirmacao();
    private final ServicoConsultaParticipantesCorporativos consultaParticipantes;
    private final PersistenciaTransacionalAthena persistencia;
    private final UsuarioInstitucional responsavel = usuario("usuario-avaliador", "avaliador", "DITEC");
    private final UsuarioInstitucional participante = usuario("usuario-participante", "participante", "GABINETE");
    private final ProjetoInstitucional projeto = projeto();
    private final Documento documento = documento();

    public AthenaHttpController() {
        this(null, new ServicoConsultaParticipantesCorporativos(FonteCorporativaParticipantesInstitucional.cadastroGovernadoPadrao()));
    }

    @Autowired
    public AthenaHttpController(PersistenciaTransacionalAthena persistencia) {
        this(persistencia, new ServicoConsultaParticipantesCorporativos(FonteCorporativaParticipantesInstitucional.cadastroGovernadoPadrao()));
    }

    AthenaHttpController(ServicoConsultaParticipantesCorporativos consultaParticipantes) {
        this(null, consultaParticipantes);
    }

    AthenaHttpController(PersistenciaTransacionalAthena persistencia, ServicoConsultaParticipantesCorporativos consultaParticipantes) {
        if (consultaParticipantes == null) {
            throw new IllegalArgumentException("servico de consulta corporativa deve ser informado.");
        }
        this.persistencia = persistencia;
        this.consultaParticipantes = consultaParticipantes;
    }

    @GetMapping
    public Map<String, Object> listarProjetosAutorizados(@RequestHeader Map<String, String> headers) {
        if (persistencia == null) {
            return Map.of("projetos", List.of(respostaProjeto()));
        }
        ContextoAutorizacaoInstitucional contexto = contextoAdministradorBootstrap();
        List<DadosProjetoPersistente> projetos = contexto.possuiAutorizacao(PoliticaSegurancaAthena.AUTORIZACAO_ADMINISTRATIVA)
                ? persistencia.listarProjetos()
                : persistencia.listarProjetosPorLogin(contexto.usuario().login());
        return Map.of("projetos", projetos.stream().map(this::respostaProjetoPersistente).toList());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> criarProjeto(
            @RequestHeader Map<String, String> headers,
            @RequestBody Map<String, Object> corpo
    ) {
        if (persistencia == null) {
            throw new RespostaApiException(HttpStatus.UNPROCESSABLE_ENTITY, "REGRA_FUNCIONAL_VIOLADA",
                    "criacao operacional de projeto exige persistencia transacional configurada.");
        }
        ContextoAutorizacaoInstitucional contexto = exigirAdministradorGlobal();
        OffsetDateTime agora = OffsetDateTime.now();
        String identificador = textoObrigatorio(corpo, "identificador").toUpperCase();
        persistencia.consultarProjetoPorIdentificador(identificador).ifPresent(projetoExistente -> {
            throw new RespostaApiException(HttpStatus.CONFLICT, "PROJETO_EXISTENTE",
                    "projeto ja existe no contexto institucional.");
        });
        DadosUsuarioPersistente usuario = persistencia.consultarUsuarioPorLogin(contexto.usuario().login())
                .orElseGet(() -> usuarioPersistente(contexto.usuario(), UUID.randomUUID()));
        DadosProjetoPersistente projetoCriado = new DadosProjetoPersistente(
                UUID.randomUUID(),
                identificador,
                textoObrigatorio(corpo, "nome"),
                textoObrigatorio(corpo, "descricao"),
                textoOpcional(corpo, "situacao") == null ? SituacaoProjeto.ATIVO.name() : textoOpcional(corpo, "situacao"),
                agora,
                usuario.id()
        );
        DadosVinculoProjetoPersistente vinculo = new DadosVinculoProjetoPersistente(
                UUID.randomUUID(),
                projetoCriado.id(),
                usuario.id(),
                PapelProjeto.RESPONSAVEL_TECNICO.name(),
                textoOpcional(corpo, "unidade") == null ? contexto.usuario().unidade() : textoOpcional(corpo, "unidade"),
                agora,
                usuario.id()
        );
        persistencia.criarProjetoComResponsavel(usuario, projetoCriado, vinculo, agora, UUID.randomUUID());
        return ResponseEntity.status(HttpStatus.CREATED).body(respostaProjetoPersistente(projetoCriado));
    }

    @PutMapping("/{idProjeto}")
    public Map<String, Object> atualizarProjeto(
            @PathVariable String idProjeto,
            @RequestHeader Map<String, String> headers,
            @RequestBody Map<String, Object> corpo
    ) {
        ProjetoInstitucional projetoAutorizado = autorizar(idProjeto, OperacaoProjeto.ADMINISTRAR);
        DadosParticipanteProjetoPersistente usuarioPersistente = usuarioPersistenteAtual(
                projetoAutorizado,
                contextoAutenticado(projetoAutorizado)
        );
        DadosProjetoPersistente atualizado = persistencia.atualizarProjeto(
                idProjeto,
                textoObrigatorio(corpo, "nome"),
                textoObrigatorio(corpo, "descricao"),
                textoOpcional(corpo, "situacao") == null ? projetoAutorizado.situacao().name() : textoOpcional(corpo, "situacao"),
                usuarioPersistente.id(),
                OffsetDateTime.now()
        );
        return respostaProjetoPersistente(atualizado);
    }

    @GetMapping("/{idProjeto}")
    public Map<String, Object> consultarProjeto(
            @PathVariable String idProjeto,
            @RequestHeader Map<String, String> headers
    ) {
        ProjetoInstitucional projetoAutorizado = autorizar(idProjeto, OperacaoProjeto.CONSULTAR);
        if (persistencia != null) {
            return respostaProjetoPersistente(projetoAutorizado);
        }
        return respostaProjeto();
    }

    @GetMapping("/{idProjeto}/documentos")
    public Map<String, Object> listarDocumentos(
            @PathVariable String idProjeto,
            @RequestHeader Map<String, String> headers
    ) {
        ProjetoInstitucional projetoAutorizado = autorizar(idProjeto, OperacaoProjeto.CONSULTAR);
        if (persistencia != null) {
            return Map.of("documentos", persistencia.listarDocumentosProjeto(uuidProjeto(projetoAutorizado))
                    .stream()
                    .map(this::respostaDocumentoResumidoPersistente)
                    .toList());
        }
        return Map.of("documentos", List.of(respostaDocumentoResumido()));
    }

    @PostMapping("/{idProjeto}/participantes")
    public ResponseEntity<Map<String, Object>> vincularParticipante(
            @PathVariable String idProjeto,
            @RequestHeader Map<String, String> headers,
            @RequestBody Map<String, Object> corpo
    ) {
        ProjetoInstitucional projetoAutorizado = autorizar(idProjeto, OperacaoProjeto.ADMINISTRAR);
        DadosParticipanteProjetoPersistente usuarioRegistro = usuarioPersistenteAtual(
                projetoAutorizado,
                contextoAutenticado(projetoAutorizado)
        );
        String loginParticipante = textoObrigatorio(corpo, "login");
        DadosUsuarioPersistente usuario = persistencia.consultarUsuarioPorLogin(loginParticipante)
                .orElseGet(() -> new DadosUsuarioPersistente(
                        UUID.randomUUID(),
                        textoObrigatorio(corpo, "identificador"),
                        textoObrigatorio(corpo, "nome"),
                        loginParticipante,
                        textoObrigatorio(corpo, "unidade")
                ));
        DadosParticipanteProjetoPersistente participante = persistencia.vincularParticipante(
                uuidProjeto(projetoAutorizado),
                usuario,
                textoObrigatorio(corpo, "papel"),
                textoObrigatorio(corpo, "unidade"),
                usuarioRegistro.id(),
                OffsetDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "nome", participante.nome(),
                "login", participante.login(),
                "unidade", participante.unidade(),
                "papel", participante.papel()
        ));
    }

    @GetMapping("/{idProjeto}/participantes")
    public Map<String, Object> listarParticipantes(
            @PathVariable String idProjeto,
            @RequestHeader Map<String, String> headers
    ) {
        ProjetoInstitucional projetoAutorizado = autorizar(idProjeto, OperacaoProjeto.ADMINISTRAR);
        return Map.of("participantes", participantesProjeto(projetoAutorizado));
    }

    @GetMapping("/{idProjeto}/documentos/{idDocumento}")
    public Map<String, Object> consultarDocumento(
            @PathVariable String idProjeto,
            @PathVariable String idDocumento,
            @RequestHeader Map<String, String> headers
    ) {
        ProjetoInstitucional projetoAutorizado = autorizar(idProjeto, OperacaoProjeto.CONSULTAR);
        if (persistencia != null) {
            DadosDocumentoPersistente documentoPersistente = documentoPersistente(uuidProjeto(projetoAutorizado), idDocumento);
            registrarLeitura(projetoAutorizado, contextoAutenticado(projetoAutorizado), documentoPersistente, null, "LEITURA_DOCUMENTO");
            return respostaDocumentoDetalhadoPersistente(documentoPersistente);
        }
        validarDocumento(idDocumento);
        return respostaDocumentoDetalhado();
    }

    @GetMapping("/{idProjeto}/documentos/{idDocumento}/itens/{idItem}")
    public Map<String, Object> consultarItem(
            @PathVariable String idProjeto,
            @PathVariable String idDocumento,
            @PathVariable String idItem,
            @RequestHeader Map<String, String> headers
    ) {
        ProjetoInstitucional projetoAutorizado = autorizar(idProjeto, OperacaoProjeto.CONSULTAR);
        if (persistencia != null) {
            DadosDocumentoPersistente documentoPersistente = documentoPersistente(uuidProjeto(projetoAutorizado), idDocumento);
            DadosItemDocumentalPersistente itemPersistente = persistencia.consultarItem(documentoPersistente.id(), idItem)
                    .orElseThrow(() -> new RespostaApiException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO",
                            "item inexistente no contexto autorizado."));
            registrarLeitura(projetoAutorizado, contextoAutenticado(projetoAutorizado), documentoPersistente, itemPersistente.id(), "LEITURA_ITEM_DOCUMENTAL");
            return respostaItemPersistente(itemPersistente);
        }
        validarDocumento(idDocumento);
        ItemDocumental item = item(idItem);
        return Map.of(
                "identificador", item.identificador(),
                "titulo", item.titulo(),
                "conteudo", item.conteudo(),
                "rastreabilidade", item.rastreabilidade().fontes(),
                "relacoesRecebidas", item.relacoesRecebidas()
        );
    }

    @PostMapping("/{idProjeto}/documentos/ingestoes")
    public ResponseEntity<Map<String, Object>> ingerirDocumentoInterno(
            @PathVariable String idProjeto,
            @RequestHeader Map<String, String> headers,
            @RequestBody Map<String, Object> corpo
    ) {
        if (persistencia == null) {
            throw new RespostaApiException(HttpStatus.UNPROCESSABLE_ENTITY, "REGRA_FUNCIONAL_VIOLADA",
                    "ingestao operacional exige persistencia transacional configurada.");
        }
        ProjetoInstitucional projetoAutorizado = autorizar(idProjeto, OperacaoProjeto.ADMINISTRAR);
        ContextoAutorizacaoInstitucional contexto = contextoAutenticado(projetoAutorizado);
        UUID projetoPersistenteId = uuidProjeto(projetoAutorizado);
        DadosParticipanteProjetoPersistente usuarioPersistente = usuarioPersistenteAtual(projetoAutorizado, contexto);
        String identificadorDocumento = textoObrigatorio(corpo, "identificadorDocumento");
        String formato = FormatoDocumento.valueOf(textoObrigatorio(corpo, "formato")).name();
        OffsetDateTime dataRecebimento = OffsetDateTime.now();
        DadosDocumentoPersistente documentoPersistente = persistencia.consultarDocumento(projetoPersistenteId, identificadorDocumento)
                .orElseGet(() -> novoDocumentoPersistente(projetoPersistenteId, usuarioPersistente.id(), dataRecebimento, corpo));
        int numeroVersao = persistencia.listarVersoesDocumento(documentoPersistente.id()).stream()
                .mapToInt(DadosVersaoDocumentoPersistente::numero)
                .max()
                .orElse(0) + 1;
        DadosVersaoDocumentoPersistente versao = novaVersaoPersistente(
                documentoPersistente.id(),
                numeroVersao,
                formato,
                usuarioPersistente.id(),
                dataRecebimento,
                corpo
        );
        List<DadosItemDocumentalPersistente> itens = itensPersistentes(versao.id(), corpo);
        if (numeroVersao == 1) {
            persistencia.registrarDocumentoComPrimeiraVersao(
                    projetoPersistenteId,
                    usuarioPersistente.id(),
                    documentoPersistente,
                    versao,
                    itens,
                    dataRecebimento,
                    UUID.randomUUID()
            );
        } else {
            persistencia.registrarNovaVersaoDocumental(
                    projetoPersistenteId,
                    usuarioPersistente.id(),
                    versao,
                    itens,
                    dataRecebimento,
                    UUID.randomUUID()
            );
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "documento", documentoPersistente.identificador(),
                "versao", numeroVersao,
                "formato", formato,
                "itens", itens.stream().map(DadosItemDocumentalPersistente::identificador).toList(),
                "relacoesRecebidas", persistencia.listarRelacoesDocumento(documentoPersistente.id()),
                "dependenciaExternaCorporativa", "REGISTRADA_COMO_PENDENTE_NAO_SIMULADA"
        ));
    }

    @PostMapping("/{idProjeto}/documentos/{idDocumento}/avaliacoes")
    public ResponseEntity<Object> registrarAvaliacao(
            @PathVariable String idProjeto,
            @PathVariable String idDocumento,
            @RequestHeader Map<String, String> headers,
            @RequestBody Map<String, Object> corpo
    ) {
        ProjetoInstitucional projetoAutorizado = autorizar(idProjeto, OperacaoProjeto.AVALIAR);
        ContextoAutorizacaoInstitucional contexto = contextoAutenticado(projetoAutorizado);
        if (persistencia != null) {
            DadosDocumentoPersistente documentoPersistente = documentoPersistente(uuidProjeto(projetoAutorizado), idDocumento);
            DadosItemDocumentalPersistente itemPersistente =
                    itemPersistente(documentoPersistente, inteiroObrigatorio(corpo, "versao"), textoObrigatorio(corpo, "idItem"));
            DadosParticipanteProjetoPersistente usuarioPersistente = usuarioPersistenteAtual(projetoAutorizado, contexto);
            return registrarInteracaoPersistente(
                    projetoAutorizado,
                    documentoPersistente,
                    itemPersistente,
                    usuarioPersistente,
                    TipoInteracaoHumana.AVALIACAO,
                    textoObrigatorio(corpo, "decisao"),
                    null,
                    textoObrigatorio(corpo, "justificativa")
            );
        }
        validarDocumento(idDocumento);
        AvaliacaoRegistrada avaliacao = avaliacoes.registrarAvaliacao(
                projetoAutorizado,
                contexto,
                new ComandoRegistrarAvaliacao(
                        new ReferenciaAvaliacaoDocumental(
                                new IdentificadorProjeto(idProjeto),
                                idDocumento,
                                inteiroObrigatorio(corpo, "versao"),
                                textoObrigatorio(corpo, "idItem")
                        ),
                        contexto.usuario().identificador(),
                        OffsetDateTime.now(),
                        DecisaoAvaliacao.valueOf(textoObrigatorio(corpo, "decisao")),
                        textoObrigatorio(corpo, "justificativa"),
                        true
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(avaliacao);
    }

    @PostMapping("/{idProjeto}/documentos/{idDocumento}/comentarios")
    public ResponseEntity<Object> registrarComentario(
            @PathVariable String idProjeto,
            @PathVariable String idDocumento,
            @RequestHeader Map<String, String> headers,
            @RequestBody Map<String, Object> corpo
    ) {
        ProjetoInstitucional projetoAutorizado = autorizar(idProjeto, OperacaoProjeto.COMENTAR);
        ContextoAutorizacaoInstitucional contexto = contextoAutenticado(projetoAutorizado);
        if (persistencia != null) {
            DadosDocumentoPersistente documentoPersistente = documentoPersistente(uuidProjeto(projetoAutorizado), idDocumento);
            DadosItemDocumentalPersistente itemPersistente =
                    itemPersistente(documentoPersistente, inteiroObrigatorio(corpo, "versao"), textoObrigatorio(corpo, "idItem"));
            DadosParticipanteProjetoPersistente usuarioPersistente = usuarioPersistenteAtual(projetoAutorizado, contexto);
            return registrarInteracaoPersistente(
                    projetoAutorizado,
                    documentoPersistente,
                    itemPersistente,
                    usuarioPersistente,
                    TipoInteracaoHumana.COMENTARIO,
                    textoOpcional(corpo, "decisaoRelacionada"),
                    textoObrigatorio(corpo, "texto"),
                    null
            );
        }
        validarDocumento(idDocumento);
        ComentarioRegistrado comentario = avaliacoes.registrarComentario(
                projetoAutorizado,
                contexto,
                new ComandoRegistrarComentario(
                        referencia(idProjeto, idDocumento, corpo, "versao"),
                        contexto.usuario().identificador(),
                        OffsetDateTime.now(),
                        textoObrigatorio(corpo, "texto"),
                        decisaoOpcional(corpo, "decisaoRelacionada")
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(comentario);
    }

    @PostMapping("/{idProjeto}/documentos/{idDocumento}/ajustes")
    public ResponseEntity<Object> registrarPedidoAjuste(
            @PathVariable String idProjeto,
            @PathVariable String idDocumento,
            @RequestHeader Map<String, String> headers,
            @RequestBody Map<String, Object> corpo
    ) {
        ProjetoInstitucional projetoAutorizado = autorizar(idProjeto, OperacaoProjeto.AVALIAR);
        ContextoAutorizacaoInstitucional contexto = contextoAutenticado(projetoAutorizado);
        if (persistencia != null) {
            DadosDocumentoPersistente documentoPersistente = documentoPersistente(uuidProjeto(projetoAutorizado), idDocumento);
            DadosItemDocumentalPersistente itemPersistente =
                    itemPersistente(documentoPersistente, inteiroObrigatorio(corpo, "versao"), textoObrigatorio(corpo, "idItem"));
            DadosParticipanteProjetoPersistente usuarioPersistente = usuarioPersistenteAtual(projetoAutorizado, contexto);
            return registrarInteracaoPersistente(
                    projetoAutorizado,
                    documentoPersistente,
                    itemPersistente,
                    usuarioPersistente,
                    TipoInteracaoHumana.PEDIDO_AJUSTE,
                    "PENDENTE",
                    responsavelTecnico(projetoAutorizado).valor(),
                    textoObrigatorio(corpo, "justificativa")
            );
        }
        validarDocumento(idDocumento);
        PedidoAjusteRegistrado pedido = avaliacoes.registrarPedidoAjuste(
                projetoAutorizado,
                contexto,
                new ComandoRegistrarPedidoAjuste(
                        referencia(idProjeto, idDocumento, corpo, "versao"),
                        contexto.usuario().identificador(),
                        OffsetDateTime.now(),
                        textoObrigatorio(corpo, "justificativa")
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(pedido);
    }

    @PostMapping("/{idProjeto}/documentos/{idDocumento}/ajustes/{pedidoAjusteId}/tratamentos")
    public ResponseEntity<Object> tratarPedidoAjuste(
            @PathVariable String idProjeto,
            @PathVariable String idDocumento,
            @PathVariable UUID pedidoAjusteId,
            @RequestHeader Map<String, String> headers,
            @RequestBody Map<String, Object> corpo
    ) {
        ProjetoInstitucional projetoAutorizado = autorizar(idProjeto, OperacaoProjeto.AVALIAR);
        ContextoAutorizacaoInstitucional contexto = contextoAutenticado(projetoAutorizado);
        EstadoSolicitacaoAjuste decisao = EstadoSolicitacaoAjuste.valueOf(textoObrigatorio(corpo, "decisao"));
        String justificativa = textoOpcional(corpo, "justificativa");
        if (decisao == EstadoSolicitacaoAjuste.NAO_ACOLHIDA) {
            justificativa = textoObrigatorio(corpo, "justificativa");
        }
        if (persistencia != null) {
            DadosDocumentoPersistente documentoPersistente = documentoPersistente(uuidProjeto(projetoAutorizado), idDocumento);
            DadosPedidoAjustePersistente pedido = pedidoAjustePersistente(documentoPersistente, pedidoAjusteId);
            DadosParticipanteProjetoPersistente usuarioPersistente = usuarioPersistenteAtual(projetoAutorizado, contexto);
            if (!usuarioPersistente.identificador().equals(pedido.responsavelTecnico())
                    && !usuarioPersistente.login().equals(pedido.responsavelTecnico())) {
                throw new RespostaApiException(HttpStatus.FORBIDDEN, "OPERACAO_NAO_AUTORIZADA",
                        "tratamento deve ser realizado pelo responsavel tecnico da solicitacao.");
            }
            OffsetDateTime dataTratamento = OffsetDateTime.now();
            persistencia.tratarPedidoAjuste(pedidoAjusteId, decisao.name(), justificativa, dataTratamento,
                    usuarioPersistente.id(), UUID.randomUUID());
            DadosPedidoAjustePersistente tratado = persistencia.consultarPedidoAjuste(pedidoAjusteId)
                    .orElseThrow(() -> new RespostaApiException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO",
                            "pedido de ajuste inexistente no contexto autorizado."));
            return ResponseEntity.status(HttpStatus.CREATED).body(respostaPedidoAjuste(tratado, documentoPersistente));
        }
        validarDocumento(idDocumento);
        PedidoAjusteRegistrado pedido = avaliacoes.registrarPedidoAjuste(
                projetoAutorizado,
                contexto,
                new ComandoRegistrarPedidoAjuste(
                        referencia(idProjeto, idDocumento, corpo, "versao"),
                        contexto.usuario().identificador(),
                        OffsetDateTime.now(),
                        textoObrigatorio(corpo, "justificativaSolicitacao")
                )
        );
        PedidoAjusteRegistrado tratado = avaliacoes.tratarPedidoAjuste(
                projetoAutorizado,
                contexto,
                pedido,
                new ComandoTratarPedidoAjuste(contexto.usuario().identificador(), OffsetDateTime.now(), decisao, justificativa)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(tratado);
    }

    @PostMapping("/{idProjeto}/documentos/{idDocumento}/versoes")
    public ResponseEntity<Map<String, Object>> criarVersaoVinculada(
            @PathVariable String idProjeto,
            @PathVariable String idDocumento,
            @RequestHeader Map<String, String> headers,
            @RequestBody Map<String, Object> corpo
    ) {
        if (persistencia == null) {
            throw new RespostaApiException(HttpStatus.UNPROCESSABLE_ENTITY, "REGRA_FUNCIONAL_VIOLADA",
                    "criacao operacional de versao vinculada exige persistencia transacional configurada.");
        }
        ProjetoInstitucional projetoAutorizado = autorizar(idProjeto, OperacaoProjeto.ADMINISTRAR);
        DadosDocumentoPersistente documentoPersistente = documentoPersistente(uuidProjeto(projetoAutorizado), idDocumento);
        int numeroAnterior = inteiroObrigatorio(corpo, "versaoAnterior");
        DadosVersaoDocumentoPersistente versaoAnterior = persistencia.consultarVersaoDocumento(documentoPersistente.id(), numeroAnterior)
                .orElseThrow(() -> new RespostaApiException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO",
                        "versao anterior inexistente no contexto autorizado."));
        if (!versaoAnterior.confirmada()) {
            throw new RespostaApiException(HttpStatus.UNPROCESSABLE_ENTITY, "REGRA_FUNCIONAL_VIOLADA",
                    "nova versao vinculada exige versao anterior formalmente confirmada.");
        }
        UUID pedidoAjusteId = uuidOpcional(corpo, "pedidoAjusteId");
        if (pedidoAjusteId != null) {
            pedidoAjustePersistente(documentoPersistente, pedidoAjusteId);
        }
        DadosParticipanteProjetoPersistente usuarioPersistente = usuarioPersistenteAtual(projetoAutorizado, contextoAutenticado(projetoAutorizado));
        OffsetDateTime dataRecebimento = OffsetDateTime.now();
        int numeroVersao = persistencia.listarVersoesDocumento(documentoPersistente.id()).stream()
                .mapToInt(DadosVersaoDocumentoPersistente::numero)
                .max()
                .orElse(0) + 1;
        DadosVersaoDocumentoPersistente versao = novaVersaoPersistente(
                documentoPersistente.id(),
                numeroVersao,
                FormatoDocumento.valueOf(textoObrigatorio(corpo, "formato")).name(),
                usuarioPersistente.id(),
                dataRecebimento,
                corpo,
                versaoAnterior.id(),
                pedidoAjusteId
        );
        List<DadosItemDocumentalPersistente> itens = itensPersistentes(versao.id(), corpo);
        persistencia.registrarNovaVersaoDocumental(
                uuidProjeto(projetoAutorizado),
                usuarioPersistente.id(),
                versao,
                itens,
                dataRecebimento,
                UUID.randomUUID()
        );
        if (pedidoAjusteId != null) {
            persistencia.vincularVersaoResultanteAoPedidoAjuste(pedidoAjusteId, versao.id());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "documento", documentoPersistente.identificador(),
                "versao", numeroVersao,
                "versaoAnterior", numeroAnterior,
                "idVersaoAnterior", versaoAnterior.id(),
                "idPedidoAjusteOrigem", pedidoAjusteId == null ? "" : pedidoAjusteId,
                "itens", itens.stream().map(DadosItemDocumentalPersistente::identificador).toList(),
                "relacoesRecebidas", persistencia.listarRelacoesDocumento(documentoPersistente.id())
        ));
    }

    @PostMapping("/{idProjeto}/documentos/{idDocumento}/confirmacoes")
    public ResponseEntity<Object> confirmarEntendimento(
            @PathVariable String idProjeto,
            @PathVariable String idDocumento,
            @RequestHeader Map<String, String> headers,
            @RequestBody Map<String, Object> corpo
    ) {
        ProjetoInstitucional projetoAutorizado = autorizar(idProjeto, OperacaoProjeto.CONFIRMAR);
        ContextoAutorizacaoInstitucional contexto = contextoAutenticado(projetoAutorizado);
        int versaoConfirmada = inteiroObrigatorio(corpo, "versaoConfirmada");
        if (persistencia != null) {
            DadosDocumentoPersistente documentoPersistente = documentoPersistente(uuidProjeto(projetoAutorizado), idDocumento);
            DadosItemDocumentalPersistente itemPersistente = primeiroItemDaVersao(documentoPersistente, versaoConfirmada);
            DadosParticipanteProjetoPersistente usuarioPersistente = usuarioPersistenteAtual(projetoAutorizado, contexto);
            ResponseEntity<Object> resposta = registrarInteracaoPersistente(
                    projetoAutorizado,
                    documentoPersistente,
                    itemPersistente,
                    usuarioPersistente,
                    TipoInteracaoHumana.CONFIRMACAO,
                    "CONFIRMADO",
                    textoObrigatorio(corpo, "escopo"),
                    textoObrigatorio(corpo, "escopo")
            );
            persistencia.confirmarVersaoDocumental(
                    persistencia.consultarVersaoDocumento(documentoPersistente.id(), versaoConfirmada)
                            .orElseThrow(() -> new RespostaApiException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO",
                                    "versao inexistente no contexto autorizado."))
                            .id()
            );
            return resposta;
        }
        validarDocumento(idDocumento);
        ConfirmacaoEntendimentoRegistrada confirmacao = avaliacoes.confirmarEntendimento(
                projetoAutorizado,
                contexto,
                new ComandoConfirmarEntendimento(
                        new IdentificadorProjeto(idProjeto),
                        idDocumento,
                        versaoConfirmada,
                        contexto.usuario().identificador(),
                        OffsetDateTime.now(),
                        textoObrigatorio(corpo, "escopo")
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(confirmacao);
    }

    @GetMapping("/{idProjeto}/documentos/{idDocumento}/historico")
    public Map<String, Object> consultarHistorico(
            @PathVariable String idProjeto,
            @PathVariable String idDocumento,
            @RequestHeader Map<String, String> headers
    ) {
        ProjetoInstitucional projetoAutorizado = autorizar(idProjeto, OperacaoProjeto.CONSULTAR);
        if (persistencia != null) {
            DadosDocumentoPersistente documentoPersistente = documentoPersistente(uuidProjeto(projetoAutorizado), idDocumento);
            return Map.of("eventos", persistencia.listarEventosAuditoriaDocumento(documentoPersistente.id()));
        }
        validarDocumento(idDocumento);
        return Map.of("eventos", avaliacoes.historico());
    }

    @GetMapping("/{idProjeto}/auditoria")
    public Map<String, Object> consultarAuditoriaTecnica(
            @PathVariable String idProjeto,
            @RequestParam(required = false) String idDocumento,
            @RequestHeader Map<String, String> headers
    ) {
        if (persistencia == null) {
            throw new RespostaApiException(HttpStatus.UNPROCESSABLE_ENTITY, "REGRA_FUNCIONAL_VIOLADA",
                    "auditoria tecnica operacional exige persistencia transacional configurada.");
        }
        ProjetoInstitucional projetoAutorizado = autorizar(idProjeto, OperacaoProjeto.CONSULTAR);
        UUID projetoPersistenteId = uuidProjeto(projetoAutorizado);
        return Map.of("eventos", persistencia.listarEventosAuditoriaProjeto(projetoPersistenteId, idDocumento));
    }

    @GetMapping("/{idProjeto}/participantes/pesquisa")
    public Map<String, Object> pesquisarParticipantes(
            @PathVariable String idProjeto,
            @RequestParam(required = false) String termo,
            @RequestParam(required = false) String matricula,
            @RequestParam(required = false) String unidadeOuLotacao,
            @RequestHeader Map<String, String> headers
    ) {
        ProjetoInstitucional projetoAutorizado = autorizar(idProjeto, OperacaoProjeto.ADMINISTRAR);
        return Map.of("participantes", consultaParticipantes.pesquisarParaVinculacao(
                projetoAutorizado,
                contextoAutenticado(projetoAutorizado),
                new ConsultaParticipantesCorporativos(termo, matricula, unidadeOuLotacao)
        ));
    }

    @ExceptionHandler(RespostaApiException.class)
    ResponseEntity<Map<String, Object>> erroApi(RespostaApiException exc) {
        return ResponseEntity.status(exc.status).body(Map.of("codigo", exc.codigo, "mensagem", exc.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, Object>> erroEntrada(IllegalArgumentException exc) {
        return ResponseEntity.badRequest().body(Map.of("codigo", "REQUISICAO_INVALIDA", "mensagem", exc.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, Object>> erroFuncional(IllegalStateException exc) {
        if (exc.getMessage() != null && exc.getMessage().contains("identidade institucional")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("codigo", "IDENTIDADE_INSTITUCIONAL_AUSENTE", "mensagem", exc.getMessage()));
        }
        if (exc.getMessage() != null && exc.getMessage().contains("nao autorizada")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("codigo", "OPERACAO_NAO_AUTORIZADA", "mensagem", exc.getMessage()));
        }
        return ResponseEntity.unprocessableEntity()
                .body(Map.of("codigo", "REGRA_FUNCIONAL_VIOLADA", "mensagem", exc.getMessage()));
    }

    private ProjetoInstitucional autorizar(String idProjeto, OperacaoProjeto operacao) {
        ProjetoInstitucional projetoAutorizado = projetoValidado(idProjeto);
        ResultadoAutorizacao resultado = PoliticaSegurancaAthena.autorizar(
                projetoAutorizado,
                contextoAutenticado(projetoAutorizado),
                operacao
        );
        if (!resultado.permitido() && resultado.motivo().contains("identidade institucional")) {
            throw new RespostaApiException(HttpStatus.UNAUTHORIZED, "IDENTIDADE_INSTITUCIONAL_AUSENTE", resultado.motivo());
        }
        if (!resultado.permitido()) {
            throw new RespostaApiException(HttpStatus.FORBIDDEN, "OPERACAO_NAO_AUTORIZADA", resultado.motivo());
        }
        return projetoAutorizado;
    }

    private ProjetoInstitucional projetoValidado(String idProjeto) {
        if (persistencia != null) {
            DadosProjetoPersistente projetoPersistente = persistencia.consultarProjetoPorIdentificador(idProjeto)
                    .orElseThrow(() -> new RespostaApiException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO",
                            "projeto inexistente no contexto autorizado."));
            List<DadosParticipanteProjetoPersistente> participantesPersistentes =
                    persistencia.listarParticipantesProjeto(projetoPersistente.id());
            if (participantesPersistentes.isEmpty()) {
                throw new RespostaApiException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO",
                        "projeto sem vinculo operacional consultavel.");
            }
            UsuarioInstitucional criador = usuarioPersistente(participantesPersistentes.stream()
                    .filter(participante -> participante.id().equals(projetoPersistente.idUsuarioCriacao()))
                    .findFirst()
                    .orElse(participantesPersistentes.get(0)));
            return new ProjetoInstitucional(
                    new IdentificadorProjeto(projetoPersistente.identificador()),
                    projetoPersistente.nome(),
                    projetoPersistente.descricao(),
                    participantesPersistentes.stream().map(DadosParticipanteProjetoPersistente::unidade).collect(java.util.stream.Collectors.toSet()),
                    participantesPersistentes.stream()
                            .map(participante -> new ParticipanteProjeto(usuarioPersistente(participante), PapelProjeto.valueOf(participante.papel())))
                            .collect(java.util.stream.Collectors.toSet()),
                    SituacaoProjeto.valueOf(projetoPersistente.situacao()),
                    projetoPersistente.dataCriacao(),
                    criador
            );
        }
        if (!projeto.identificador().valor().equals(idProjeto)) {
            throw new RespostaApiException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO", "projeto inexistente no contexto autorizado.");
        }
        return projeto;
    }

    private void validarDocumento(String idDocumento) {
        if (!documento.identificador().equals(idDocumento)) {
            throw new RespostaApiException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO", "documento inexistente no contexto autorizado.");
        }
    }

    private DadosDocumentoPersistente documentoPersistente(UUID idProjeto, String idDocumento) {
        return persistencia.consultarDocumento(idProjeto, idDocumento)
                .orElseThrow(() -> new RespostaApiException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO",
                        "documento inexistente no contexto autorizado."));
    }

    private DadosItemDocumentalPersistente itemPersistente(
            DadosDocumentoPersistente documentoPersistente,
            int versao,
            String idItem
    ) {
        return persistencia.listarVersoesDocumento(documentoPersistente.id()).stream()
                .filter(versaoPersistente -> versaoPersistente.numero() == versao)
                .flatMap(versaoPersistente -> persistencia.listarItensDocumento(documentoPersistente.id()).stream()
                        .filter(item -> item.idVersao().equals(versaoPersistente.id()))
                        .filter(item -> item.identificador().equals(idItem)))
                .findFirst()
                .orElseThrow(() -> new RespostaApiException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO",
                        "item inexistente no contexto autorizado."));
    }

    private DadosItemDocumentalPersistente primeiroItemDaVersao(DadosDocumentoPersistente documentoPersistente, int versao) {
        return persistencia.listarVersoesDocumento(documentoPersistente.id()).stream()
                .filter(versaoPersistente -> versaoPersistente.numero() == versao)
                .flatMap(versaoPersistente -> persistencia.listarItensDocumento(documentoPersistente.id()).stream()
                        .filter(item -> item.idVersao().equals(versaoPersistente.id()))
                        .sorted(Comparator.comparingInt(DadosItemDocumentalPersistente::ordem)))
                .findFirst()
                .orElseThrow(() -> new RespostaApiException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO",
                        "versao sem item documental consultavel."));
    }

    private DadosPedidoAjustePersistente pedidoAjustePersistente(
            DadosDocumentoPersistente documentoPersistente,
            UUID pedidoAjusteId
    ) {
        DadosPedidoAjustePersistente pedido = persistencia.consultarPedidoAjuste(pedidoAjusteId)
                .orElseThrow(() -> new RespostaApiException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO",
                        "pedido de ajuste inexistente no contexto autorizado."));
        boolean pertenceAoDocumento = persistencia.listarItensDocumento(documentoPersistente.id()).stream()
                .anyMatch(item -> item.id().equals(pedido.idItem()));
        if (!pertenceAoDocumento) {
            throw new RespostaApiException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO",
                    "pedido de ajuste inexistente no contexto autorizado.");
        }
        return pedido;
    }

    private DadosParticipanteProjetoPersistente usuarioPersistenteAtual(
            ProjetoInstitucional projetoAutorizado,
            ContextoAutorizacaoInstitucional contexto
    ) {
        String login = contexto.usuario().login();
        return persistencia.listarParticipantesProjeto(uuidProjeto(projetoAutorizado)).stream()
                .filter(participante -> participante.login().equals(login))
                .findFirst()
                .orElseThrow(() -> new RespostaApiException(HttpStatus.FORBIDDEN, "OPERACAO_NAO_AUTORIZADA",
                        "usuario sem vinculo operacional persistente."));
    }

    private ResponseEntity<Object> registrarInteracaoPersistente(
            ProjetoInstitucional projetoAutorizado,
            DadosDocumentoPersistente documentoPersistente,
            DadosItemDocumentalPersistente itemPersistente,
            DadosParticipanteProjetoPersistente usuarioPersistente,
            TipoInteracaoHumana tipo,
            String decisao,
            String conteudo,
            String justificativa
    ) {
        OffsetDateTime dataRegistro = OffsetDateTime.now();
        RegistroInteracaoHumana registro = new RegistroInteracaoHumana(
                UUID.randomUUID(),
                itemPersistente.id(),
                usuarioPersistente.id(),
                tipo,
                decisao,
                conteudo,
                justificativa,
                dataRegistro,
                UUID.randomUUID()
        );
        persistencia.registrarInteracaoHumana(registro);
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("id", registro.id());
        resposta.put("projeto", projetoAutorizado.identificador().valor());
        resposta.put("documento", documentoPersistente.identificador());
        resposta.put("item", itemPersistente.identificador());
        resposta.put("tipo", tipo.name());
        resposta.put("decisao", decisao == null ? "" : decisao);
        resposta.put("estado", decisao == null ? "" : decisao);
        resposta.put("conteudo", conteudo == null ? "" : conteudo);
        resposta.put("justificativa", justificativa == null ? "" : justificativa);
        resposta.put("autor", usuarioPersistente.login());
        resposta.put("dataRegistro", dataRegistro);
        if (tipo == TipoInteracaoHumana.PEDIDO_AJUSTE) {
            resposta.put("responsavelTecnico", conteudo == null ? "" : conteudo);
            resposta.put("dataSolicitacao", dataRegistro);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    private void registrarLeitura(
            ProjetoInstitucional projetoAutorizado,
            ContextoAutorizacaoInstitucional contexto,
            DadosDocumentoPersistente documentoPersistente,
            UUID idItem,
            String tipo
    ) {
        DadosParticipanteProjetoPersistente usuarioPersistente = usuarioPersistenteAtual(projetoAutorizado, contexto);
        persistencia.registrarLeituraTecnica(
                uuidProjeto(projetoAutorizado),
                documentoPersistente.id(),
                idItem,
                usuarioPersistente.id(),
                tipo,
                OffsetDateTime.now()
        );
    }

    private ItemDocumental item(String idItem) {
        return documento.versoes().get(0).itens().stream()
                .filter(item -> item.identificador().equals(idItem))
                .findFirst()
                .orElseThrow(() -> new RespostaApiException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO", "item inexistente no contexto autorizado."));
    }

    private ContextoAutorizacaoInstitucional contextoAutenticado(ProjetoInstitucional projetoAutorizado) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            return new ContextoAutorizacaoInstitucional(null, Set.of());
        }
        String login = authentication.getName();
        Set<String> autorizacoes = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return new ContextoAutorizacaoInstitucional(
                usuarioAutenticado(login, projetoAutorizado),
                autorizacoes
        );
    }

    private ContextoAutorizacaoInstitucional contextoAdministradorBootstrap() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            throw new RespostaApiException(HttpStatus.UNAUTHORIZED, "IDENTIDADE_INSTITUCIONAL_AUSENTE",
                    "identidade institucional autenticada deve ser informada.");
        }
        Set<String> autorizacoes = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return new ContextoAutorizacaoInstitucional(
                usuario("usuario-" + authentication.getName(), authentication.getName(), "DITEC"),
                autorizacoes
        );
    }

    private ContextoAutorizacaoInstitucional exigirAdministradorGlobal() {
        ContextoAutorizacaoInstitucional contexto = contextoAdministradorBootstrap();
        ResultadoAutorizacao resultado = PoliticaSegurancaAthena.autorizar(projeto, contexto, OperacaoProjeto.ADMINISTRAR);
        if (!resultado.permitido()) {
            throw new RespostaApiException(HttpStatus.FORBIDDEN, "OPERACAO_NAO_AUTORIZADA", resultado.motivo());
        }
        return contexto;
    }

    private UsuarioInstitucional usuarioAutenticado(String login, ProjetoInstitucional projetoAutorizado) {
        return projetoAutorizado.participantes().stream()
                .map(ParticipanteProjeto::usuario)
                .filter(usuario -> usuario.login().equals(login))
                .findFirst()
                .orElseGet(() -> usuario("usuario-" + login, login, "NAO_INFORMADA"));
    }

    private DadosDocumentoPersistente novoDocumentoPersistente(
            UUID idProjeto,
            UUID idUsuario,
            OffsetDateTime dataCriacao,
            Map<String, Object> corpo
    ) {
        return new DadosDocumentoPersistente(
                UUID.randomUUID(),
                textoObrigatorio(corpo, "identificadorDocumento"),
                idProjeto,
                textoObrigatorio(corpo, "tituloDocumento"),
                textoObrigatorio(corpo, "grupo"),
                textoObrigatorio(corpo, "categoria"),
                textoObrigatorio(corpo, "finalidade"),
                textoObrigatorio(corpo, "apresentacao"),
                dataCriacao,
                idUsuario
        );
    }

    private DadosVersaoDocumentoPersistente novaVersaoPersistente(
            UUID idDocumento,
            int numeroVersao,
            String formato,
            UUID idUsuario,
            OffsetDateTime dataRecebimento,
            Map<String, Object> corpo
    ) {
        return novaVersaoPersistente(
                idDocumento,
                numeroVersao,
                formato,
                idUsuario,
                dataRecebimento,
                corpo,
                idVersaoAnterior(idDocumento, numeroVersao),
                null
        );
    }

    private DadosVersaoDocumentoPersistente novaVersaoPersistente(
            UUID idDocumento,
            int numeroVersao,
            String formato,
            UUID idUsuario,
            OffsetDateTime dataRecebimento,
            Map<String, Object> corpo,
            UUID idVersaoAnterior,
            UUID idPedidoAjusteOrigem
    ) {
        String conteudoOriginal = textoObrigatorio(corpo, "conteudoOriginal");
        return new DadosVersaoDocumentoPersistente(
                UUID.randomUUID(),
                idDocumento,
                numeroVersao,
                formato,
                conteudoOriginal,
                Integer.toHexString(conteudoOriginal.hashCode()),
                dataRecebimento,
                idUsuario,
                textoObrigatorio(corpo, "origem"),
                idVersaoAnterior,
                false,
                idPedidoAjusteOrigem
        );
    }

    private UUID idVersaoAnterior(UUID idDocumento, int numeroVersao) {
        if (numeroVersao == 1) {
            return null;
        }
        return persistencia.listarVersoesDocumento(idDocumento).stream()
                .filter(versao -> versao.numero() == numeroVersao - 1)
                .map(DadosVersaoDocumentoPersistente::id)
                .findFirst()
                .orElseThrow(() -> new RespostaApiException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "REGRA_FUNCIONAL_VIOLADA",
                        "nova versao documental exige versao anterior persistida."
                ));
    }

    private List<DadosItemDocumentalPersistente> itensPersistentes(UUID idVersao, Map<String, Object> corpo) {
        Object itens = corpo.get("representacaoInterna");
        if (!(itens instanceof Collection<?> colecao) || colecao.isEmpty()) {
            throw new IllegalArgumentException("representacaoInterna deve conter ao menos um item.");
        }
        int[] ordem = {1};
        return colecao.stream()
                .map(item -> itemPersistente(idVersao, item, ordem[0]++))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private DadosItemDocumentalPersistente itemPersistente(UUID idVersao, Object item, int ordem) {
        if (!(item instanceof Map<?, ?> mapaBruto)) {
            throw new IllegalArgumentException("item da representacaoInterna deve ser objeto.");
        }
        Map<String, Object> mapa = (Map<String, Object>) mapaBruto;
        return new DadosItemDocumentalPersistente(
                UUID.randomUUID(),
                idVersao,
                textoObrigatorio(mapa, "identificador"),
                textoObrigatorio(mapa, "titulo"),
                textoObrigatorio(mapa, "conteudo"),
                ordem,
                textosObrigatorios(mapa, "fontesRastreabilidade")
        );
    }

    private static List<String> textosObrigatorios(Map<String, Object> corpo, String campo) {
        Object valor = corpo.get(campo);
        if (!(valor instanceof Collection<?> colecao) || colecao.isEmpty()) {
            throw new IllegalArgumentException(campo + " deve conter ao menos um valor.");
        }
        return colecao.stream()
                .map(Object::toString)
                .filter(texto -> !texto.isBlank())
                .toList();
    }

    private Map<String, Object> respostaProjeto() {
        return Map.of(
                "identificador", projeto.identificador().valor(),
                "nome", projeto.nome(),
                "descricao", projeto.descricao(),
                "unidades", projeto.unidades(),
                "participantes", projeto.participantes().stream()
                        .map(participanteProjeto -> Map.of(
                                "nome", participanteProjeto.usuario().nome(),
                                "login", participanteProjeto.usuario().login(),
                                "unidade", participanteProjeto.usuario().unidade(),
                                "papel", participanteProjeto.papel().name()
                        ))
                        .toList(),
                "situacao", projeto.situacao().name(),
                "dataCriacao", projeto.dataCriacao(),
                "usuarioCriacao", responsavel.login()
        );
    }

    private Map<String, Object> respostaProjetoPersistente(ProjetoInstitucional projetoAutorizado) {
        UUID idProjeto = uuidProjeto(projetoAutorizado);
        Map<String, Long> indicadores = persistencia.calcularIndicadoresProjeto(idProjeto);
        return Map.of(
                "identificador", projetoAutorizado.identificador().valor(),
                "nome", projetoAutorizado.nome(),
                "descricao", projetoAutorizado.descricao(),
                "unidades", projetoAutorizado.unidades(),
                "participantes", participantesProjeto(projetoAutorizado),
                "situacao", projetoAutorizado.situacao().name(),
                "dataCriacao", projetoAutorizado.dataCriacao(),
                "usuarioCriacao", projetoAutorizado.usuarioCriacao().login(),
                "indicadores", indicadores
        );
    }

    private List<Map<String, String>> participantesProjeto(ProjetoInstitucional projetoAutorizado) {
        return projetoAutorizado.participantes().stream()
                .sorted(Comparator.comparing(participante -> participante.usuario().nome()))
                .map(participante -> Map.of(
                        "nome", participante.usuario().nome(),
                        "login", participante.usuario().login(),
                        "unidade", participante.usuario().unidade(),
                        "papel", participante.papel().name()
                ))
                .toList();
    }

    private Map<String, Object> respostaProjetoPersistente(DadosProjetoPersistente projetoPersistente) {
        List<DadosParticipanteProjetoPersistente> participantes = persistencia.listarParticipantesProjeto(projetoPersistente.id());
        Map<String, Long> indicadores = persistencia.calcularIndicadoresProjeto(projetoPersistente.id());
        String usuarioCriacao = participantes.stream()
                .filter(participante -> participante.id().equals(projetoPersistente.idUsuarioCriacao()))
                .map(DadosParticipanteProjetoPersistente::login)
                .findFirst()
                .orElse("");
        return Map.of(
                "identificador", projetoPersistente.identificador(),
                "nome", projetoPersistente.nome(),
                "descricao", projetoPersistente.descricao(),
                "unidades", participantes.stream().map(DadosParticipanteProjetoPersistente::unidade).distinct().toList(),
                "participantes", participantes.stream()
                        .map(participante -> Map.of(
                                "nome", participante.nome(),
                                "login", participante.login(),
                                "unidade", participante.unidade(),
                                "papel", participante.papel()
                        ))
                        .toList(),
                "situacao", projetoPersistente.situacao(),
                "dataCriacao", projetoPersistente.dataCriacao(),
                "usuarioCriacao", usuarioCriacao,
                "indicadores", indicadores
        );
    }

    private Map<String, Object> respostaDocumentoResumidoPersistente(DadosDocumentoPersistente documentoPersistente) {
        return Map.of(
                "identificador", documentoPersistente.identificador(),
                "titulo", documentoPersistente.titulo(),
                "grupo", documentoPersistente.grupo(),
                "categoria", documentoPersistente.categoria(),
                "finalidade", documentoPersistente.finalidade(),
                "apresentacao", documentoPersistente.apresentacao(),
                "relacoesRecebidas", persistencia.listarRelacoesDocumento(documentoPersistente.id())
        );
    }

    private Map<String, Object> respostaDocumentoDetalhadoPersistente(DadosDocumentoPersistente documentoPersistente) {
        return Map.of(
                "identificador", documentoPersistente.identificador(),
                "titulo", documentoPersistente.titulo(),
                "metadados", Map.of(
                        "grupo", documentoPersistente.grupo(),
                        "categoria", documentoPersistente.categoria(),
                        "finalidade", documentoPersistente.finalidade(),
                        "apresentacao", documentoPersistente.apresentacao()
                ),
                "versoes", persistencia.listarVersoesDocumento(documentoPersistente.id()).stream()
                        .map(this::respostaVersaoPersistente)
                        .toList(),
                "relacoesRecebidas", persistencia.listarRelacoesDocumento(documentoPersistente.id())
        );
    }

    private Map<String, Object> respostaVersaoPersistente(DadosVersaoDocumentoPersistente versao) {
        return Map.of(
                "id", versao.id(),
                "numero", versao.numero(),
                "formato", versao.formato(),
                "conteudoOriginal", versao.conteudoOriginal(),
                "dataRecebimento", versao.dataRecebimento(),
                "confirmada", versao.confirmada(),
                "idVersaoAnterior", uuidComoTexto(versao.idVersaoAnterior()),
                "idPedidoAjusteOrigem", uuidComoTexto(versao.idPedidoAjusteOrigem()),
                "itens", persistencia.listarItensDocumento(versao.idDocumento()).stream()
                        .filter(item -> item.idVersao().equals(versao.id()))
                        .sorted(Comparator.comparingInt(DadosItemDocumentalPersistente::ordem))
                        .map(this::respostaItemPersistente)
                        .toList()
        );
    }

    private Map<String, Object> respostaPedidoAjuste(
            DadosPedidoAjustePersistente pedido,
            DadosDocumentoPersistente documentoPersistente
    ) {
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("id", pedido.id());
        resposta.put("documento", documentoPersistente.identificador());
        resposta.put("idItem", pedido.idItem());
        resposta.put("idSolicitante", pedido.idSolicitante());
        resposta.put("justificativa", pedido.justificativa());
        resposta.put("estado", pedido.situacao());
        resposta.put("responsavelTecnico", pedido.responsavelTecnico());
        resposta.put("dataSolicitacao", pedido.dataSolicitacao());
        resposta.put("dataTratamento", pedido.dataTratamento() == null ? "" : pedido.dataTratamento());
        resposta.put("decisaoTratamento", pedido.decisaoTratamento() == null ? "" : pedido.decisaoTratamento());
        resposta.put("justificativaTratamento", pedido.justificativaTratamento() == null ? "" : pedido.justificativaTratamento());
        resposta.put("idVersaoResultante", pedido.idVersaoResultante() == null ? "" : pedido.idVersaoResultante());
        return resposta;
    }

    private static String uuidComoTexto(UUID valor) {
        return valor == null ? "" : valor.toString();
    }

    private Map<String, Object> respostaItemPersistente(DadosItemDocumentalPersistente item) {
        return Map.of(
                "identificador", item.identificador(),
                "titulo", item.titulo(),
                "conteudo", item.conteudo(),
                "rastreabilidade", item.fontesRastreabilidade(),
                "relacoesRecebidas", persistencia.listarRelacoesItem(item.id())
        );
    }

    private Map<String, Object> respostaDocumentoResumido() {
        return Map.of(
                "identificador", documento.identificador(),
                "titulo", documento.titulo(),
                "grupo", documento.metadados().grupo(),
                "categoria", documento.metadados().categoria(),
                "finalidade", documento.metadados().finalidade(),
                "apresentacao", documento.metadados().apresentacao(),
                "relacoesRecebidas", documento.metadados().relacoesRecebidas()
        );
    }

    private Map<String, Object> respostaDocumentoDetalhado() {
        return Map.of(
                "identificador", documento.identificador(),
                "titulo", documento.titulo(),
                "metadados", documento.metadados(),
                "versoes", documento.versoes(),
                "relacoesRecebidas", documento.metadados().relacoesRecebidas()
        );
    }

    private static String textoObrigatorio(Map<String, Object> corpo, String campo) {
        Object valor = corpo.get(campo);
        if (valor == null || valor.toString().isBlank()) {
            throw new IllegalArgumentException(campo + " deve ser informado.");
        }
        return valor.toString();
    }

    private static String textoOpcional(Map<String, Object> corpo, String campo) {
        Object valor = corpo.get(campo);
        if (valor == null || valor.toString().isBlank()) {
            return null;
        }
        return valor.toString();
    }

    private static IdentificadorUsuario responsavelTecnico(ProjetoInstitucional projeto) {
        return projeto.participantes().stream()
                .filter(participante -> participante.papel() == PapelProjeto.RESPONSAVEL_TECNICO)
                .map(participante -> participante.usuario().identificador())
                .findFirst()
                .orElseThrow(() -> new RespostaApiException(HttpStatus.UNPROCESSABLE_ENTITY, "REGRA_FUNCIONAL_VIOLADA",
                        "projeto deve possuir responsavel tecnico para tratar solicitacao de ajuste."));
    }

    private static DecisaoAvaliacao decisaoOpcional(Map<String, Object> corpo, String campo) {
        String valor = textoOpcional(corpo, campo);
        return valor == null ? null : DecisaoAvaliacao.valueOf(valor);
    }

    private static ReferenciaAvaliacaoDocumental referencia(
            String idProjeto,
            String idDocumento,
            Map<String, Object> corpo,
            String campoVersao
    ) {
        return new ReferenciaAvaliacaoDocumental(
                new IdentificadorProjeto(idProjeto),
                idDocumento,
                inteiroObrigatorio(corpo, campoVersao),
                textoObrigatorio(corpo, "idItem")
        );
    }

    private static int inteiroObrigatorio(Map<String, Object> corpo, String campo) {
        Object valor = corpo.get(campo);
        if (valor instanceof Number numero) {
            return numero.intValue();
        }
        return Integer.parseInt(textoObrigatorio(corpo, campo));
    }

    private static UUID uuidOpcional(Map<String, Object> corpo, String campo) {
        Object valor = corpo.get(campo);
        if (valor == null || valor.toString().isBlank()) {
            return null;
        }
        return UUID.fromString(valor.toString());
    }

    private ProjetoInstitucional projeto() {
        return new ProjetoInstitucional(
                new IdentificadorProjeto("ATHENA"),
                "Athena",
                "Plataforma institucional multiprojeto documental",
                Set.of("DITEC"),
                Set.of(
                        new ParticipanteProjeto(responsavel, PapelProjeto.RESPONSAVEL_TECNICO),
                        new ParticipanteProjeto(participante, PapelProjeto.PARTICIPANTE)
                ),
                SituacaoProjeto.ATIVO,
                OffsetDateTime.parse("2026-08-25T10:15:30-03:00"),
                responsavel
        );
    }

    private Documento documento() {
        return new Documento(
                "DOC-ENTENDIMENTO",
                new IdentificadorProjeto("ATHENA"),
                "Entendimento do Athena",
                new MetadadosDocumento(
                        "Especificacao",
                        "Entendimento",
                        "Consulta institucional",
                        "Arvore documental",
                        Set.of(PermissaoDocumento.CONSULTAR, PermissaoDocumento.AVALIAR),
                        Set.of(new RelacaoDocumental("rastreia", "BL.AC.002")),
                        new RastreabilidadeDocumento(Set.of("BL.AC.002", "SOL.API.001"))
                ),
                List.of(new VersaoDocumento(
                        1,
                        FormatoDocumento.MARKDOWN,
                        "conteudo original recebido",
                        OffsetDateTime.parse("2026-08-25T10:20:30-03:00"),
                        List.of(new ItemDocumental(
                                "ESP.REQ.006",
                                "Avaliacao humana",
                                "Item documental exposto pela API.",
                                Set.of(new RelacaoDocumental("rastreia", "BL.AC.002")),
                                new RastreabilidadeDocumento(Set.of("ESP.REQ.006"))
                        ))
                ))
        );
    }

    private static UsuarioInstitucional usuario(String id, String login, String unidade) {
        return new UsuarioInstitucional(new IdentificadorUsuario(id), "Usuario Institucional", login, unidade);
    }

    private static DadosUsuarioPersistente usuarioPersistente(UsuarioInstitucional usuario, UUID id) {
        return new DadosUsuarioPersistente(id, usuario.identificador().valor(), usuario.nome(), usuario.login(), usuario.unidade());
    }

    private static UsuarioInstitucional usuarioPersistente(DadosParticipanteProjetoPersistente participante) {
        return new UsuarioInstitucional(
                new IdentificadorUsuario(participante.identificador()),
                participante.nome(),
                participante.login(),
                participante.unidade()
        );
    }

    private UUID uuidProjeto(ProjetoInstitucional projetoAutorizado) {
        return persistencia.consultarProjetoPorIdentificador(projetoAutorizado.identificador().valor())
                .map(DadosProjetoPersistente::id)
                .orElseThrow(() -> new RespostaApiException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO",
                        "projeto inexistente no contexto autorizado."));
    }

    private static final class RespostaApiException extends RuntimeException {
        private final HttpStatus status;
        private final String codigo;

        private RespostaApiException(HttpStatus status, String codigo, String mensagem) {
            super(mensagem);
            this.status = status;
            this.codigo = codigo;
        }
    }
}
