package br.gov.mppb.athena.api;

import br.gov.mppb.athena.aplicacao.AvaliacaoRegistrada;
import br.gov.mppb.athena.aplicacao.ComandoConfirmarEntendimento;
import br.gov.mppb.athena.aplicacao.ComandoRegistrarComentario;
import br.gov.mppb.athena.aplicacao.ComandoRegistrarPedidoAjuste;
import br.gov.mppb.athena.aplicacao.ComentarioRegistrado;
import br.gov.mppb.athena.aplicacao.ConfirmacaoEntendimentoRegistrada;
import br.gov.mppb.athena.aplicacao.ComandoRegistrarAvaliacao;
import br.gov.mppb.athena.aplicacao.DecisaoAvaliacao;
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
import br.gov.mppb.athena.persistencia.DadosParticipanteProjetoPersistente;
import br.gov.mppb.athena.persistencia.DadosProjetoPersistente;
import br.gov.mppb.athena.persistencia.DadosVersaoDocumentoPersistente;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @GetMapping("/{projetoId}")
    public Map<String, Object> consultarProjeto(
            @PathVariable String projetoId,
            @RequestHeader Map<String, String> headers
    ) {
        ProjetoInstitucional projetoAutorizado = autorizar(projetoId, headers, OperacaoProjeto.CONSULTAR);
        if (persistencia != null) {
            return respostaProjetoPersistente(projetoAutorizado);
        }
        return respostaProjeto();
    }

    @GetMapping("/{projetoId}/documentos")
    public Map<String, Object> listarDocumentos(
            @PathVariable String projetoId,
            @RequestHeader Map<String, String> headers
    ) {
        ProjetoInstitucional projetoAutorizado = autorizar(projetoId, headers, OperacaoProjeto.CONSULTAR);
        if (persistencia != null) {
            return Map.of("documentos", persistencia.listarDocumentosProjeto(uuidProjeto(projetoAutorizado))
                    .stream()
                    .map(this::respostaDocumentoResumidoPersistente)
                    .toList());
        }
        return Map.of("documentos", List.of(respostaDocumentoResumido()));
    }

    @GetMapping("/{projetoId}/documentos/{documentoId}")
    public Map<String, Object> consultarDocumento(
            @PathVariable String projetoId,
            @PathVariable String documentoId,
            @RequestHeader Map<String, String> headers
    ) {
        ProjetoInstitucional projetoAutorizado = autorizar(projetoId, headers, OperacaoProjeto.CONSULTAR);
        if (persistencia != null) {
            DadosDocumentoPersistente documentoPersistente = documentoPersistente(uuidProjeto(projetoAutorizado), documentoId);
            registrarLeitura(projetoAutorizado, contexto(headers), documentoPersistente, null, "LEITURA_DOCUMENTO");
            return respostaDocumentoDetalhadoPersistente(documentoPersistente);
        }
        validarDocumento(documentoId);
        return respostaDocumentoDetalhado();
    }

    @GetMapping("/{projetoId}/documentos/{documentoId}/itens/{itemId}")
    public Map<String, Object> consultarItem(
            @PathVariable String projetoId,
            @PathVariable String documentoId,
            @PathVariable String itemId,
            @RequestHeader Map<String, String> headers
    ) {
        ProjetoInstitucional projetoAutorizado = autorizar(projetoId, headers, OperacaoProjeto.CONSULTAR);
        if (persistencia != null) {
            DadosDocumentoPersistente documentoPersistente = documentoPersistente(uuidProjeto(projetoAutorizado), documentoId);
            DadosItemDocumentalPersistente itemPersistente = persistencia.consultarItem(documentoPersistente.id(), itemId)
                    .orElseThrow(() -> new RespostaApiException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO",
                            "item inexistente no contexto autorizado."));
            registrarLeitura(projetoAutorizado, contexto(headers), documentoPersistente, itemPersistente.id(), "LEITURA_ITEM_DOCUMENTAL");
            return respostaItemPersistente(itemPersistente);
        }
        validarDocumento(documentoId);
        ItemDocumental item = item(itemId);
        return Map.of(
                "identificador", item.identificador(),
                "titulo", item.titulo(),
                "conteudo", item.conteudo(),
                "rastreabilidade", item.rastreabilidade().fontes(),
                "relacoesRecebidas", item.relacoesRecebidas()
        );
    }

    @PostMapping("/{projetoId}/documentos/ingestoes")
    public ResponseEntity<Map<String, Object>> ingerirDocumentoInterno(
            @PathVariable String projetoId,
            @RequestHeader Map<String, String> headers,
            @RequestBody Map<String, Object> corpo
    ) {
        if (persistencia == null) {
            throw new RespostaApiException(HttpStatus.UNPROCESSABLE_ENTITY, "REGRA_FUNCIONAL_VIOLADA",
                    "ingestao operacional exige persistencia transacional configurada.");
        }
        ProjetoInstitucional projetoAutorizado = autorizar(projetoId, headers, OperacaoProjeto.ADMINISTRAR);
        ContextoAutorizacaoInstitucional contexto = contexto(headers);
        UUID projetoPersistenteId = uuidProjeto(projetoAutorizado);
        DadosParticipanteProjetoPersistente usuarioPersistente = usuarioPersistenteAtual(projetoAutorizado, contexto);
        String identificadorDocumento = textoObrigatorio(corpo, "identificadorDocumento");
        String formato = FormatoDocumento.valueOf(textoObrigatorio(corpo, "formato")).name();
        OffsetDateTime recebidaEm = OffsetDateTime.now();
        DadosDocumentoPersistente documentoPersistente = persistencia.consultarDocumento(projetoPersistenteId, identificadorDocumento)
                .orElseGet(() -> novoDocumentoPersistente(projetoPersistenteId, usuarioPersistente.id(), recebidaEm, corpo));
        int numeroVersao = persistencia.listarVersoesDocumento(documentoPersistente.id()).stream()
                .mapToInt(DadosVersaoDocumentoPersistente::numero)
                .max()
                .orElse(0) + 1;
        DadosVersaoDocumentoPersistente versao = novaVersaoPersistente(
                documentoPersistente.id(),
                numeroVersao,
                formato,
                usuarioPersistente.id(),
                recebidaEm,
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
                    recebidaEm,
                    UUID.randomUUID()
            );
        } else {
            persistencia.registrarNovaVersaoDocumental(
                    projetoPersistenteId,
                    usuarioPersistente.id(),
                    versao,
                    itens,
                    recebidaEm,
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

    @PostMapping("/{projetoId}/documentos/{documentoId}/avaliacoes")
    public ResponseEntity<Object> registrarAvaliacao(
            @PathVariable String projetoId,
            @PathVariable String documentoId,
            @RequestHeader Map<String, String> headers,
            @RequestBody Map<String, Object> corpo
    ) {
        ProjetoInstitucional projetoAutorizado = autorizar(projetoId, headers, OperacaoProjeto.AVALIAR);
        ContextoAutorizacaoInstitucional contexto = contexto(headers);
        if (persistencia != null) {
            DadosDocumentoPersistente documentoPersistente = documentoPersistente(uuidProjeto(projetoAutorizado), documentoId);
            DadosItemDocumentalPersistente itemPersistente =
                    itemPersistente(documentoPersistente, inteiroObrigatorio(corpo, "versao"), textoObrigatorio(corpo, "itemId"));
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
        validarDocumento(documentoId);
        AvaliacaoRegistrada avaliacao = avaliacoes.registrarAvaliacao(
                projetoAutorizado,
                contexto,
                new ComandoRegistrarAvaliacao(
                        new ReferenciaAvaliacaoDocumental(
                                new IdentificadorProjeto(projetoId),
                                documentoId,
                                inteiroObrigatorio(corpo, "versao"),
                                textoObrigatorio(corpo, "itemId")
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

    @PostMapping("/{projetoId}/documentos/{documentoId}/comentarios")
    public ResponseEntity<Object> registrarComentario(
            @PathVariable String projetoId,
            @PathVariable String documentoId,
            @RequestHeader Map<String, String> headers,
            @RequestBody Map<String, Object> corpo
    ) {
        ProjetoInstitucional projetoAutorizado = autorizar(projetoId, headers, OperacaoProjeto.COMENTAR);
        ContextoAutorizacaoInstitucional contexto = contexto(headers);
        if (persistencia != null) {
            DadosDocumentoPersistente documentoPersistente = documentoPersistente(uuidProjeto(projetoAutorizado), documentoId);
            DadosItemDocumentalPersistente itemPersistente =
                    itemPersistente(documentoPersistente, inteiroObrigatorio(corpo, "versao"), textoObrigatorio(corpo, "itemId"));
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
        validarDocumento(documentoId);
        ComentarioRegistrado comentario = avaliacoes.registrarComentario(
                projetoAutorizado,
                contexto,
                new ComandoRegistrarComentario(
                        referencia(projetoId, documentoId, corpo, "versao"),
                        contexto.usuario().identificador(),
                        OffsetDateTime.now(),
                        textoObrigatorio(corpo, "texto"),
                        decisaoOpcional(corpo, "decisaoRelacionada")
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(comentario);
    }

    @PostMapping("/{projetoId}/documentos/{documentoId}/ajustes")
    public ResponseEntity<Object> registrarPedidoAjuste(
            @PathVariable String projetoId,
            @PathVariable String documentoId,
            @RequestHeader Map<String, String> headers,
            @RequestBody Map<String, Object> corpo
    ) {
        ProjetoInstitucional projetoAutorizado = autorizar(projetoId, headers, OperacaoProjeto.AVALIAR);
        ContextoAutorizacaoInstitucional contexto = contexto(headers);
        if (persistencia != null) {
            DadosDocumentoPersistente documentoPersistente = documentoPersistente(uuidProjeto(projetoAutorizado), documentoId);
            DadosItemDocumentalPersistente itemPersistente =
                    itemPersistente(documentoPersistente, inteiroObrigatorio(corpo, "versao"), textoObrigatorio(corpo, "itemId"));
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
        validarDocumento(documentoId);
        PedidoAjusteRegistrado pedido = avaliacoes.registrarPedidoAjuste(
                projetoAutorizado,
                contexto,
                new ComandoRegistrarPedidoAjuste(
                        referencia(projetoId, documentoId, corpo, "versao"),
                        contexto.usuario().identificador(),
                        OffsetDateTime.now(),
                        textoObrigatorio(corpo, "justificativa")
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(pedido);
    }

    @PostMapping("/{projetoId}/documentos/{documentoId}/confirmacoes")
    public ResponseEntity<Object> confirmarEntendimento(
            @PathVariable String projetoId,
            @PathVariable String documentoId,
            @RequestHeader Map<String, String> headers,
            @RequestBody Map<String, Object> corpo
    ) {
        ProjetoInstitucional projetoAutorizado = autorizar(projetoId, headers, OperacaoProjeto.CONFIRMAR);
        ContextoAutorizacaoInstitucional contexto = contexto(headers);
        int versaoConfirmada = inteiroObrigatorio(corpo, "versaoConfirmada");
        if (persistencia != null) {
            DadosDocumentoPersistente documentoPersistente = documentoPersistente(uuidProjeto(projetoAutorizado), documentoId);
            DadosItemDocumentalPersistente itemPersistente = primeiroItemDaVersao(documentoPersistente, versaoConfirmada);
            DadosParticipanteProjetoPersistente usuarioPersistente = usuarioPersistenteAtual(projetoAutorizado, contexto);
            return registrarInteracaoPersistente(
                    projetoAutorizado,
                    documentoPersistente,
                    itemPersistente,
                    usuarioPersistente,
                    TipoInteracaoHumana.CONFIRMACAO,
                    "CONFIRMADO",
                    textoObrigatorio(corpo, "escopo"),
                    textoObrigatorio(corpo, "escopo")
            );
        }
        validarDocumento(documentoId);
        ConfirmacaoEntendimentoRegistrada confirmacao = avaliacoes.confirmarEntendimento(
                projetoAutorizado,
                contexto,
                new ComandoConfirmarEntendimento(
                        new IdentificadorProjeto(projetoId),
                        documentoId,
                        versaoConfirmada,
                        contexto.usuario().identificador(),
                        OffsetDateTime.now(),
                        textoObrigatorio(corpo, "escopo")
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(confirmacao);
    }

    @GetMapping("/{projetoId}/documentos/{documentoId}/historico")
    public Map<String, Object> consultarHistorico(
            @PathVariable String projetoId,
            @PathVariable String documentoId,
            @RequestHeader Map<String, String> headers
    ) {
        ProjetoInstitucional projetoAutorizado = autorizar(projetoId, headers, OperacaoProjeto.CONSULTAR);
        if (persistencia != null) {
            DadosDocumentoPersistente documentoPersistente = documentoPersistente(uuidProjeto(projetoAutorizado), documentoId);
            return Map.of("eventos", persistencia.listarEventosAuditoriaDocumento(documentoPersistente.id()));
        }
        validarDocumento(documentoId);
        return Map.of("eventos", avaliacoes.historico());
    }

    @GetMapping("/{projetoId}/auditoria")
    public Map<String, Object> consultarAuditoriaTecnica(
            @PathVariable String projetoId,
            @RequestParam(required = false) String documentoId,
            @RequestHeader Map<String, String> headers
    ) {
        if (persistencia == null) {
            throw new RespostaApiException(HttpStatus.UNPROCESSABLE_ENTITY, "REGRA_FUNCIONAL_VIOLADA",
                    "auditoria tecnica operacional exige persistencia transacional configurada.");
        }
        ProjetoInstitucional projetoAutorizado = autorizar(projetoId, headers, OperacaoProjeto.CONSULTAR);
        UUID projetoPersistenteId = uuidProjeto(projetoAutorizado);
        return Map.of("eventos", persistencia.listarEventosAuditoriaProjeto(projetoPersistenteId, documentoId));
    }

    @GetMapping("/{projetoId}/participantes/pesquisa")
    public Map<String, Object> pesquisarParticipantes(
            @PathVariable String projetoId,
            @RequestParam(required = false) String termo,
            @RequestParam(required = false) String matricula,
            @RequestParam(required = false) String unidadeOuLotacao,
            @RequestHeader Map<String, String> headers
    ) {
        return Map.of("participantes", consultaParticipantes.pesquisarParaVinculacao(
                projetoValidado(projetoId),
                contexto(headers),
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

    private ProjetoInstitucional autorizar(String projetoId, Map<String, String> headers, OperacaoProjeto operacao) {
        ProjetoInstitucional projetoAutorizado = projetoValidado(projetoId);
        ResultadoAutorizacao resultado = PoliticaSegurancaAthena.autorizar(projetoAutorizado, contexto(headers), operacao);
        if (!resultado.permitido() && resultado.motivo().contains("identidade institucional")) {
            throw new RespostaApiException(HttpStatus.UNAUTHORIZED, "IDENTIDADE_INSTITUCIONAL_AUSENTE", resultado.motivo());
        }
        if (!resultado.permitido()) {
            throw new RespostaApiException(HttpStatus.FORBIDDEN, "OPERACAO_NAO_AUTORIZADA", resultado.motivo());
        }
        return projetoAutorizado;
    }

    private ProjetoInstitucional projetoValidado(String projetoId) {
        if (persistencia != null) {
            DadosProjetoPersistente projetoPersistente = persistencia.consultarProjetoPorIdentificador(projetoId)
                    .orElseThrow(() -> new RespostaApiException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO",
                            "projeto inexistente no contexto autorizado."));
            List<DadosParticipanteProjetoPersistente> participantesPersistentes =
                    persistencia.listarParticipantesProjeto(projetoPersistente.id());
            if (participantesPersistentes.isEmpty()) {
                throw new RespostaApiException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO",
                        "projeto sem vinculo operacional consultavel.");
            }
            UsuarioInstitucional criador = usuarioPersistente(participantesPersistentes.stream()
                    .filter(participante -> participante.id().equals(projetoPersistente.criadoPorId()))
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
                    projetoPersistente.criadoEm(),
                    criador
            );
        }
        if (!projeto.identificador().valor().equals(projetoId)) {
            throw new RespostaApiException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO", "projeto inexistente no contexto autorizado.");
        }
        return projeto;
    }

    private void validarDocumento(String documentoId) {
        if (!documento.identificador().equals(documentoId)) {
            throw new RespostaApiException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO", "documento inexistente no contexto autorizado.");
        }
    }

    private DadosDocumentoPersistente documentoPersistente(UUID projetoId, String documentoId) {
        return persistencia.consultarDocumento(projetoId, documentoId)
                .orElseThrow(() -> new RespostaApiException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO",
                        "documento inexistente no contexto autorizado."));
    }

    private DadosItemDocumentalPersistente itemPersistente(
            DadosDocumentoPersistente documentoPersistente,
            int versao,
            String itemId
    ) {
        return persistencia.listarVersoesDocumento(documentoPersistente.id()).stream()
                .filter(versaoPersistente -> versaoPersistente.numero() == versao)
                .flatMap(versaoPersistente -> persistencia.listarItensDocumento(documentoPersistente.id()).stream()
                        .filter(item -> item.versaoId().equals(versaoPersistente.id()))
                        .filter(item -> item.identificador().equals(itemId)))
                .findFirst()
                .orElseThrow(() -> new RespostaApiException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO",
                        "item inexistente no contexto autorizado."));
    }

    private DadosItemDocumentalPersistente primeiroItemDaVersao(DadosDocumentoPersistente documentoPersistente, int versao) {
        return persistencia.listarVersoesDocumento(documentoPersistente.id()).stream()
                .filter(versaoPersistente -> versaoPersistente.numero() == versao)
                .flatMap(versaoPersistente -> persistencia.listarItensDocumento(documentoPersistente.id()).stream()
                        .filter(item -> item.versaoId().equals(versaoPersistente.id()))
                        .sorted(Comparator.comparingInt(DadosItemDocumentalPersistente::ordem)))
                .findFirst()
                .orElseThrow(() -> new RespostaApiException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO",
                        "versao sem item documental consultavel."));
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
        OffsetDateTime registradaEm = OffsetDateTime.now();
        RegistroInteracaoHumana registro = new RegistroInteracaoHumana(
                UUID.randomUUID(),
                itemPersistente.id(),
                usuarioPersistente.id(),
                tipo,
                decisao,
                conteudo,
                justificativa,
                registradaEm,
                UUID.randomUUID()
        );
        persistencia.registrarInteracaoHumana(registro);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "projeto", projetoAutorizado.identificador().valor(),
                "documento", documentoPersistente.identificador(),
                "item", itemPersistente.identificador(),
                "tipo", tipo.name(),
                "decisao", decisao == null ? "" : decisao,
                "conteudo", conteudo == null ? "" : conteudo,
                "justificativa", justificativa == null ? "" : justificativa,
                "autor", usuarioPersistente.login(),
                "registradaEm", registradaEm
        ));
    }

    private void registrarLeitura(
            ProjetoInstitucional projetoAutorizado,
            ContextoAutorizacaoInstitucional contexto,
            DadosDocumentoPersistente documentoPersistente,
            UUID itemId,
            String tipo
    ) {
        DadosParticipanteProjetoPersistente usuarioPersistente = usuarioPersistenteAtual(projetoAutorizado, contexto);
        persistencia.registrarLeituraTecnica(
                uuidProjeto(projetoAutorizado),
                documentoPersistente.id(),
                itemId,
                usuarioPersistente.id(),
                tipo,
                OffsetDateTime.now()
        );
    }

    private ItemDocumental item(String itemId) {
        return documento.versoes().get(0).itens().stream()
                .filter(item -> item.identificador().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RespostaApiException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO", "item inexistente no contexto autorizado."));
    }

    private ContextoAutorizacaoInstitucional contexto(Map<String, String> headers) {
        String login = header(headers, "x-athena-usuario");
        if (login == null || login.isBlank()) {
            return new ContextoAutorizacaoInstitucional(null, Set.of());
        }
        String unidade = header(headers, "x-athena-unidade");
        String autorizacoes = header(headers, "x-athena-autorizacoes");
        return new ContextoAutorizacaoInstitucional(
                usuario("usuario-" + login, login, unidade == null || unidade.isBlank() ? "GABINETE" : unidade),
                autorizacoes == null || autorizacoes.isBlank() ? Set.of() : Set.of(autorizacoes.split(","))
        );
    }

    private DadosDocumentoPersistente novoDocumentoPersistente(
            UUID projetoId,
            UUID usuarioId,
            OffsetDateTime criadoEm,
            Map<String, Object> corpo
    ) {
        return new DadosDocumentoPersistente(
                UUID.randomUUID(),
                textoObrigatorio(corpo, "identificadorDocumento"),
                projetoId,
                textoObrigatorio(corpo, "tituloDocumento"),
                textoObrigatorio(corpo, "grupo"),
                textoObrigatorio(corpo, "categoria"),
                textoObrigatorio(corpo, "finalidade"),
                textoObrigatorio(corpo, "apresentacao"),
                criadoEm,
                usuarioId
        );
    }

    private DadosVersaoDocumentoPersistente novaVersaoPersistente(
            UUID documentoId,
            int numeroVersao,
            String formato,
            UUID usuarioId,
            OffsetDateTime recebidaEm,
            Map<String, Object> corpo
    ) {
        String conteudoOriginal = textoObrigatorio(corpo, "conteudoOriginal");
        return new DadosVersaoDocumentoPersistente(
                UUID.randomUUID(),
                documentoId,
                numeroVersao,
                formato,
                conteudoOriginal,
                Integer.toHexString(conteudoOriginal.hashCode()),
                recebidaEm,
                usuarioId,
                textoObrigatorio(corpo, "origem"),
                versaoAnteriorId(documentoId, numeroVersao),
                false,
                null
        );
    }

    private UUID versaoAnteriorId(UUID documentoId, int numeroVersao) {
        if (numeroVersao == 1) {
            return null;
        }
        return persistencia.listarVersoesDocumento(documentoId).stream()
                .filter(versao -> versao.numero() == numeroVersao - 1)
                .map(DadosVersaoDocumentoPersistente::id)
                .findFirst()
                .orElseThrow(() -> new RespostaApiException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "REGRA_FUNCIONAL_VIOLADA",
                        "nova versao documental exige versao anterior persistida."
                ));
    }

    private List<DadosItemDocumentalPersistente> itensPersistentes(UUID versaoId, Map<String, Object> corpo) {
        Object itens = corpo.get("representacaoInterna");
        if (!(itens instanceof Collection<?> colecao) || colecao.isEmpty()) {
            throw new IllegalArgumentException("representacaoInterna deve conter ao menos um item.");
        }
        int[] ordem = {1};
        return colecao.stream()
                .map(item -> itemPersistente(versaoId, item, ordem[0]++))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private DadosItemDocumentalPersistente itemPersistente(UUID versaoId, Object item, int ordem) {
        if (!(item instanceof Map<?, ?> mapaBruto)) {
            throw new IllegalArgumentException("item da representacaoInterna deve ser objeto.");
        }
        Map<String, Object> mapa = (Map<String, Object>) mapaBruto;
        return new DadosItemDocumentalPersistente(
                UUID.randomUUID(),
                versaoId,
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

    private String header(Map<String, String> headers, String nome) {
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(nome))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private Map<String, Object> respostaProjeto() {
        return Map.of(
                "identificador", projeto.identificador().valor(),
                "nome", projeto.nome(),
                "descricao", projeto.descricao(),
                "unidades", projeto.unidades(),
                "participantes", List.of(Map.of(
                        "nome", responsavel.nome(),
                        "login", responsavel.login(),
                        "unidade", responsavel.unidade(),
                        "papel", PapelProjeto.RESPONSAVEL_TECNICO.name()
                )),
                "situacao", projeto.situacao().name(),
                "criadoEm", projeto.criadoEm(),
                "criadoPor", responsavel.login()
        );
    }

    private Map<String, Object> respostaProjetoPersistente(ProjetoInstitucional projetoAutorizado) {
        UUID projetoId = uuidProjeto(projetoAutorizado);
        Map<String, Long> indicadores = persistencia.calcularIndicadoresProjeto(projetoId);
        return Map.of(
                "identificador", projetoAutorizado.identificador().valor(),
                "nome", projetoAutorizado.nome(),
                "descricao", projetoAutorizado.descricao(),
                "unidades", projetoAutorizado.unidades(),
                "participantes", projetoAutorizado.participantes().stream()
                        .map(participante -> Map.of(
                                "nome", participante.usuario().nome(),
                                "login", participante.usuario().login(),
                                "unidade", participante.usuario().unidade(),
                                "papel", participante.papel().name()
                        ))
                        .toList(),
                "situacao", projetoAutorizado.situacao().name(),
                "criadoEm", projetoAutorizado.criadoEm(),
                "criadoPor", projetoAutorizado.criadoPor().login(),
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
                "numero", versao.numero(),
                "formato", versao.formato(),
                "conteudoOriginal", versao.conteudoOriginal(),
                "recebidaEm", versao.recebidaEm(),
                "confirmada", versao.confirmada(),
                "versaoAnteriorId", uuidComoTexto(versao.versaoAnteriorId()),
                "pedidoAjusteOrigemId", uuidComoTexto(versao.pedidoAjusteOrigemId()),
                "itens", persistencia.listarItensDocumento(versao.documentoId()).stream()
                        .filter(item -> item.versaoId().equals(versao.id()))
                        .sorted(Comparator.comparingInt(DadosItemDocumentalPersistente::ordem))
                        .map(this::respostaItemPersistente)
                        .toList()
        );
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
            String projetoId,
            String documentoId,
            Map<String, Object> corpo,
            String campoVersao
    ) {
        return new ReferenciaAvaliacaoDocumental(
                new IdentificadorProjeto(projetoId),
                documentoId,
                inteiroObrigatorio(corpo, campoVersao),
                textoObrigatorio(corpo, "itemId")
        );
    }

    private static int inteiroObrigatorio(Map<String, Object> corpo, String campo) {
        Object valor = corpo.get(campo);
        if (valor instanceof Number numero) {
            return numero.intValue();
        }
        return Integer.parseInt(textoObrigatorio(corpo, campo));
    }

    private ProjetoInstitucional projeto() {
        return new ProjetoInstitucional(
                new IdentificadorProjeto("ATHENA"),
                "Athena",
                "Plataforma institucional multiprojeto documental",
                Set.of("DITEC"),
                Set.of(new ParticipanteProjeto(responsavel, PapelProjeto.RESPONSAVEL_TECNICO)),
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
