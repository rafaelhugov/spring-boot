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
import br.gov.mppb.athena.seguranca.ParticipanteVisivel;
import br.gov.mppb.athena.seguranca.ResultadoAutorizacao;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/projetos")
public final class AthenaHttpController {

    private static final int LIMITE_RECEBIMENTO_BYTES = 10 * 1024 * 1024;
    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    private final ServicoAvaliacaoConfirmacao avaliacoes = new ServicoAvaliacaoConfirmacao();
    private final ServicoConsultaParticipantesCorporativos consultaParticipantes;
    private final PersistenciaTransacionalAthena persistencia;
    private final UsuarioInstitucional responsavel = usuario("usuario-avaliador", "avaliador", "DITEC");
    private final UsuarioInstitucional participante = usuario("usuario-participante", "participante", "GABINETE");
    private final ProjetoInstitucional projeto = projeto();
    private final Documento documento = documento();

    public AthenaHttpController() {
        this(null, consultaParticipantesIndisponivel());
    }

    public AthenaHttpController(PersistenciaTransacionalAthena persistencia) {
        this(persistencia, consultaParticipantesIndisponivel());
    }

    @Autowired
    public AthenaHttpController(PersistenciaTransacionalAthena persistencia, JdbcTemplate jdbc) {
        this(persistencia, new ServicoConsultaParticipantesCorporativos(
                new FonteCorporativaParticipantesInstitucional(jdbc)));
    }

    AthenaHttpController(ServicoConsultaParticipantesCorporativos consultaParticipantes) {
        this(null, consultaParticipantes);
    }

    public AthenaHttpController(PersistenciaTransacionalAthena persistencia, ServicoConsultaParticipantesCorporativos consultaParticipantes) {
        if (consultaParticipantes == null) {
            throw new IllegalArgumentException("servico de consulta corporativa deve ser informado.");
        }
        this.persistencia = persistencia;
        this.consultaParticipantes = consultaParticipantes;
    }

    private static ServicoConsultaParticipantesCorporativos consultaParticipantesIndisponivel() {
        return new ServicoConsultaParticipantesCorporativos(consulta -> {
            throw new IllegalStateException("fonte corporativa de participantes indisponivel.");
        });
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
        Map<String, Object> responsavelInformado = mapaObrigatorio(corpo, "responsavelTecnico");
        String loginResponsavel = textoObrigatorio(responsavelInformado, "login");
        ParticipanteVisivel responsavelCorporativo = consultaParticipantes.pesquisarParaAdministracao(
                        contexto,
                        new ConsultaParticipantesCorporativos(loginResponsavel, null, null))
                .stream()
                .filter(candidato -> loginResponsavel.equalsIgnoreCase(candidato.login()))
                .findFirst()
                .orElseThrow(() -> new RespostaApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "RESPONSAVEL_INSTITUCIONAL_INVALIDO",
                        "responsavel tecnico deve ser selecionado na pesquisa corporativa."));
        DadosUsuarioPersistente responsavel = usuario.login().equalsIgnoreCase(responsavelCorporativo.login())
                ? new DadosUsuarioPersistente(
                        usuario.id(), usuario.identificador(), responsavelCorporativo.nome(), usuario.login(),
                        responsavelCorporativo.unidadeOuLotacao())
                : persistencia.consultarUsuarioPorLogin(responsavelCorporativo.login())
                        .orElseGet(() -> new DadosUsuarioPersistente(
                                UUID.randomUUID(),
                                "usuario-" + responsavelCorporativo.login(),
                                responsavelCorporativo.nome(),
                                responsavelCorporativo.login(),
                                responsavelCorporativo.unidadeOuLotacao()
                        ));
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
                responsavel.id(),
                PapelProjeto.RESPONSAVEL_TECNICO.name(),
                responsavel.unidade(),
                agora,
                usuario.id()
        );
        persistencia.criarProjetoComResponsavel(usuario, responsavel, projetoCriado, vinculo, agora, UUID.randomUUID());
        return ResponseEntity.status(HttpStatus.CREATED).body(respostaProjetoPersistente(projetoCriado));
    }

    @GetMapping("/participantes/pesquisa")
    public Map<String, Object> pesquisarParticipantesAntesDoProjeto(
            @RequestParam(required = false) String termo,
            @RequestParam(required = false) String matricula,
            @RequestParam(required = false) String unidadeOuLotacao,
            @RequestHeader Map<String, String> headers
    ) {
        ContextoAutorizacaoInstitucional contexto = exigirAdministradorGlobal();
        return Map.of("participantes", consultaParticipantes.pesquisarParaAdministracao(
                contexto, new ConsultaParticipantesCorporativos(termo, matricula, unidadeOuLotacao)));
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

    @PostMapping(
            value = "/{idProjeto}/documentos/recebimentos",
            consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Map<String, Object>> receberDocumento(
            @PathVariable String idProjeto,
            @RequestHeader Map<String, String> headers,
            @RequestPart("arquivo") MultipartFile arquivo,
            @RequestPart("metadados") String metadadosJson
    ) throws IOException {
        if (persistencia == null) {
            throw new RespostaApiException(HttpStatus.UNPROCESSABLE_ENTITY, "REGRA_FUNCIONAL_VIOLADA",
                    "recebimento documental exige persistencia transacional configurada.");
        }
        ProjetoInstitucional projetoAutorizado = autorizar(idProjeto, OperacaoProjeto.ADMINISTRAR);
        ContextoAutorizacaoInstitucional contexto = contextoAutenticado(projetoAutorizado);
        DadosParticipanteProjetoPersistente usuarioPersistente = usuarioPersistenteAtual(projetoAutorizado, contexto);
        byte[] original = bytesOriginaisValidos(arquivo);
        String conteudoOriginal = textoUtf8(original);
        Map<String, Object> metadados = metadadosRecebimento(metadadosJson);
        FormatoDocumento formato = FormatoDocumento.valueOf(textoObrigatorio(metadados, "formato"));
        List<DadosItemDocumentalPersistente> itensSemVersao = itensRecebidos(formato, conteudoOriginal, metadados);
        String hashOriginal = sha256(original);
        byte[] metadadosCanonicos = metadadosCanonicos(metadados);
        String hashMetadados = sha256(metadadosCanonicos);
        String identificadorDocumento = textoObrigatorio(metadados, "identificadorDocumento");
        int numeroVersao = inteiroObrigatorio(metadados, "versaoDocumento");
        if (numeroVersao <= 0) {
            throw new RespostaApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CONTRATO_INVALIDO",
                    "versaoDocumento deve ser inteiro positivo.");
        }
        String chaveIdempotencia = sha256((idProjeto + "\u001f" + identificadorDocumento + "\u001f"
                + numeroVersao + "\u001f" + hashOriginal + "\u001f" + hashMetadados)
                .getBytes(StandardCharsets.UTF_8));
        UUID idProjetoPersistente = uuidProjeto(projetoAutorizado);
        var versaoIdempotente = persistencia.consultarVersaoPorChaveIdempotencia(chaveIdempotencia);
        if (versaoIdempotente.isPresent()) {
            DadosDocumentoPersistente existente = documentoPersistentePorId(versaoIdempotente.get().idDocumento());
            return ResponseEntity.ok(respostaRecebimento(existente, versaoIdempotente.get(), false, itensDaVersao(versaoIdempotente.get())));
        }
        Optional<DadosDocumentoPersistente> documentoExistente =
                persistencia.consultarDocumento(idProjetoPersistente, identificadorDocumento);
        boolean documentoNovo = documentoExistente.isEmpty();
        DadosDocumentoPersistente documentoPersistente = documentoExistente
                .orElseGet(() -> novoDocumentoRecebido(idProjetoPersistente, usuarioPersistente.id(), OffsetDateTime.now(), metadados));
        persistencia.consultarVersaoDocumento(documentoPersistente.id(), numeroVersao).ifPresent(versao -> {
            throw new RespostaApiException(HttpStatus.CONFLICT, "CONFLITO_VERSAO_DOCUMENTAL",
                    "versao documental ja existe com conteudo ou metadados divergentes.");
        });
        OffsetDateTime dataRecebimento = OffsetDateTime.now();
        UUID idVersao = UUID.randomUUID();
        DadosVersaoDocumentoPersistente versao = new DadosVersaoDocumentoPersistente(
                idVersao,
                documentoPersistente.id(),
                numeroVersao,
                formato.name(),
                conteudoOriginal,
                hashOriginal,
                dataRecebimento,
                usuarioPersistente.id(),
                nomeFisicoArquivo(arquivo),
                idVersaoAnterior(documentoPersistente.id(), numeroVersao),
                false,
                null,
                original,
                (long) original.length,
                tipoMidia(arquivo),
                "UTF-8",
                hashMetadados,
                chaveIdempotencia
        );
        List<DadosItemDocumentalPersistente> itens = itensSemVersao.stream()
                .map(item -> new DadosItemDocumentalPersistente(
                        item.id(), idVersao, item.identificador(), item.titulo(), item.conteudo(),
                        item.ordem(), item.fontesRastreabilidade()))
                .toList();
        persistencia.registrarRecebimentoDocumental(
                idProjetoPersistente,
                usuarioPersistente.id(),
                documentoPersistente,
                documentoNovo,
                versao,
                itens,
                dataRecebimento,
                UUID.randomUUID()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(respostaRecebimento(documentoPersistente, versao, true, itens));
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

    @GetMapping("/{idProjeto}/documentos/{idDocumento}/ajustes")
    public Map<String, Object> listarPedidosAjuste(
            @PathVariable String idProjeto,
            @PathVariable String idDocumento,
            @RequestHeader Map<String, String> headers
    ) {
        ProjetoInstitucional projetoAutorizado = autorizar(idProjeto, OperacaoProjeto.CONSULTAR);
        if (persistencia == null) {
            return Map.of("pedidos", List.of());
        }
        DadosDocumentoPersistente documentoPersistente = documentoPersistente(uuidProjeto(projetoAutorizado), idDocumento);
        return Map.of("pedidos", persistencia.listarPedidosAjusteDocumento(documentoPersistente.id()).stream()
                .map(pedido -> respostaPedidoAjuste(pedido, documentoPersistente))
                .toList());
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
            UUID idVersaoAlvo = persistencia.consultarVersaoDocumento(documentoPersistente.id(), versaoConfirmada)
                    .orElseThrow(() -> new RespostaApiException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO",
                            "versao inexistente no contexto autorizado."))
                    .id();
            UUID idVersaoAtual = persistencia.consultarReferenciaVersaoAtual(documentoPersistente.id()).idVersaoAtual();
            if (idVersaoAtual == null || !idVersaoAtual.equals(idVersaoAlvo)) {
                throw new RespostaApiException(HttpStatus.UNPROCESSABLE_ENTITY, "REGRA_FUNCIONAL_VIOLADA",
                        "confirmacao formal exige a versao atual do documento.");
            }
            boolean possuiAjustePendente = persistencia.listarPedidosAjusteDocumento(documentoPersistente.id()).stream()
                    .anyMatch(pedido -> "PENDENTE".equals(pedido.situacao()));
            if (possuiAjustePendente) {
                throw new RespostaApiException(HttpStatus.UNPROCESSABLE_ENTITY, "REGRA_FUNCIONAL_VIOLADA",
                        "confirmacao formal exige ausencia de pedidos de ajuste pendentes.");
            }
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

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, Object>> erroIntegridade(DataIntegrityViolationException exc) {
        return ResponseEntity.badRequest().body(Map.of(
                "codigo", "INTEGRIDADE_PERSISTENCIA",
                "mensagem", "recebimento rejeitado por violar integridade transacional."));
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

    private DadosDocumentoPersistente documentoPersistentePorId(UUID idDocumento) {
        return persistencia.consultarHistoricoDocumento(idDocumento).documento();
    }

    private DadosDocumentoPersistente novoDocumentoRecebido(
            UUID idProjeto,
            UUID idUsuario,
            OffsetDateTime dataCriacao,
            Map<String, Object> metadados
    ) {
        return new DadosDocumentoPersistente(
                UUID.randomUUID(),
                textoObrigatorio(metadados, "identificadorDocumento"),
                idProjeto,
                textoObrigatorio(metadados, "tituloDocumento"),
                textoObrigatorio(metadados, "grupo"),
                textoObrigatorio(metadados, "categoria"),
                textoObrigatorio(metadados, "finalidade"),
                textoObrigatorio(metadados, "apresentacao"),
                dataCriacao,
                idUsuario,
                null
        );
    }

    private static byte[] bytesOriginaisValidos(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RespostaApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CONTRATO_INVALIDO",
                    "arquivo deve ser informado com conteudo.");
        }
        if (arquivo.getSize() > LIMITE_RECEBIMENTO_BYTES) {
            throw new RespostaApiException(HttpStatus.PAYLOAD_TOO_LARGE, "ARQUIVO_ACIMA_DO_LIMITE",
                    "arquivo excede o limite de 10 MB.");
        }
        try {
            return arquivo.getBytes();
        } catch (IOException exc) {
            throw new RespostaApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CONTRATO_INVALIDO",
                    "arquivo nao pode ser lido.");
        }
    }

    private static String textoUtf8(byte[] original) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(original))
                    .toString();
        } catch (CharacterCodingException exc) {
            throw new RespostaApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CONTEUDO_INVALIDO",
                    "arquivo deve estar em UTF-8 estrito.");
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> metadadosRecebimento(String metadadosJson) {
        try {
            Object valor = JSON.readValue(metadadosJson, Object.class);
            if (!(valor instanceof Map<?, ?> mapa)) {
                throw new RespostaApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CONTRATO_INVALIDO",
                        "metadados deve ser um objeto JSON.");
            }
            return (Map<String, Object>) mapa;
        } catch (JsonProcessingException exc) {
            throw new RespostaApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CONTRATO_INVALIDO",
                    "metadados deve ser JSON UTF-8 valido.");
        }
    }

    private List<DadosItemDocumentalPersistente> itensRecebidos(
            FormatoDocumento formato,
            String conteudoOriginal,
            Map<String, Object> metadados
    ) {
        List<Map<String, Object>> itens = switch (formato) {
            case JSON -> itensJson(conteudoOriginal);
            case YAML -> itensYaml(conteudoOriginal);
            case MARKDOWN -> itensMetadados(metadados);
        };
        if (itens.isEmpty()) {
            throw new RespostaApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CONTEUDO_INVALIDO",
                    "representacao interna deve conter ao menos um item.");
        }
        int[] ordem = {1};
        return itens.stream()
                .map(item -> itemRecebido(item, ordem[0]++))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> itensJson(String conteudoOriginal) {
        try {
            Object valor = JSON.readValue(conteudoOriginal, Object.class);
            Object itens = valor instanceof Map<?, ?> mapa ? mapa.get("itens") : null;
            if (!(itens instanceof Collection<?> colecao)) {
                return List.of();
            }
            return colecao.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> (Map<String, Object>) item)
                    .toList();
        } catch (JsonProcessingException exc) {
            throw new RespostaApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CONTEUDO_INVALIDO",
                    "arquivo JSON deve ser estruturado e valido.");
        }
    }

    private static List<Map<String, Object>> itensYaml(String conteudoOriginal) {
        List<Map<String, Object>> itens = new ArrayList<>();
        Map<String, Object> atual = null;
        String chaveMultilinha = null;
        for (String linha : conteudoOriginal.split("\\R")) {
            if (linha.startsWith("- id_item:")) {
                if (atual != null) {
                    itens.add(atual);
                }
                atual = new LinkedHashMap<>();
                atual.put("id_item", valorYaml(linha.substring("- id_item:".length())));
                chaveMultilinha = null;
                continue;
            }
            if (atual == null) {
                continue;
            }
            if (linha.startsWith("  - ") && "origem_upstream".equals(chaveMultilinha)) {
                @SuppressWarnings("unchecked")
                List<String> fontes = (List<String>) atual.computeIfAbsent("origem_upstream", chave -> new ArrayList<String>());
                fontes.add(valorYaml(linha.substring(4)));
                continue;
            }
            if (chaveMultilinha != null && linha.startsWith("    ")) {
                atual.compute(chaveMultilinha, (chave, valor) ->
                        (valor == null ? "" : valor + " ") + linha.strip());
                continue;
            }
            if (!linha.startsWith("  ") || !linha.contains(":")) {
                chaveMultilinha = null;
                continue;
            }
            int separador = linha.indexOf(':');
            String chave = linha.substring(2, separador);
            String valor = valorYaml(linha.substring(separador + 1));
            if ("origem_upstream".equals(chave)) {
                atual.put(chave, new ArrayList<String>());
                chaveMultilinha = chave;
            } else {
                atual.put(chave, valor);
                chaveMultilinha = valor.isBlank() || "descricao".equals(chave) ? chave : null;
            }
        }
        if (atual != null) {
            itens.add(atual);
        }
        return itens;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> itensMetadados(Map<String, Object> metadados) {
        Object itens = metadados.get("itens");
        if (!(itens instanceof Collection<?> colecao)) {
            return List.of();
        }
        return colecao.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .toList();
    }

    private static DadosItemDocumentalPersistente itemRecebido(Map<String, Object> item, int ordem) {
        String identificador = textoObrigatorioAlternativo(item, "identificador", "id_item");
        String titulo = textoObrigatorio(item, "titulo");
        String conteudo = textoOpcional(item, "conteudo");
        if (conteudo == null) {
            conteudo = textoObrigatorioAlternativo(item, "descricao", "texto");
        }
        return new DadosItemDocumentalPersistente(
                UUID.randomUUID(),
                UUID.randomUUID(),
                identificador,
                titulo,
                conteudo,
                ordem,
                fontesItem(item)
        );
    }

    private static List<String> fontesItem(Map<String, Object> item) {
        Set<String> fontes = new LinkedHashSet<>();
        adicionarFonte(fontes, item.get("etapa"));
        adicionarFonte(fontes, item.get("categoria"));
        adicionarFonte(fontes, item.get("versao_item"));
        adicionarFontes(fontes, item.get("fontesRastreabilidade"));
        adicionarFontes(fontes, item.get("rastreabilidade"));
        adicionarFontes(fontes, item.get("origem_upstream"));
        if (fontes.isEmpty()) {
            fontes.add("RECEBIMENTO_DOCUMENTAL");
        }
        return fontes.stream().toList();
    }

    private static void adicionarFontes(Set<String> fontes, Object valor) {
        if (valor instanceof Collection<?> colecao) {
            colecao.forEach(item -> adicionarFonte(fontes, item));
            return;
        }
        adicionarFonte(fontes, valor);
    }

    private static void adicionarFonte(Set<String> fontes, Object valor) {
        if (valor != null && !valor.toString().isBlank()) {
            fontes.add(valor.toString());
        }
    }

    private static String textoObrigatorioAlternativo(Map<String, Object> corpo, String primeiro, String segundo) {
        String valor = textoOpcional(corpo, primeiro);
        return valor == null ? textoObrigatorio(corpo, segundo) : valor;
    }

    private static String valorYaml(String valor) {
        String limpo = valor == null ? "" : valor.strip();
        if ((limpo.startsWith("'") && limpo.endsWith("'")) || (limpo.startsWith("\"") && limpo.endsWith("\""))) {
            return limpo.substring(1, limpo.length() - 1);
        }
        return limpo;
    }

    private static byte[] metadadosCanonicos(Map<String, Object> metadados) {
        try {
            return JSON.writeValueAsBytes(canonico(metadados));
        } catch (JsonProcessingException exc) {
            throw new RespostaApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CONTRATO_INVALIDO",
                    "metadados nao podem ser canonizados.");
        }
    }

    private static Object canonico(Object valor) {
        if (valor instanceof Map<?, ?> mapa) {
            Map<String, Object> ordenado = new TreeMap<>();
            mapa.forEach((chave, item) -> ordenado.put(chave.toString(), canonico(item)));
            return ordenado;
        }
        if (valor instanceof Collection<?> colecao) {
            return colecao.stream().map(AthenaHttpController::canonico).toList();
        }
        return valor;
    }

    private static String sha256(byte[] bytes) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder hexadecimal = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hexadecimal.append(String.format("%02x", b));
            }
            return hexadecimal.toString();
        } catch (NoSuchAlgorithmException exc) {
            throw new IllegalStateException("SHA-256 indisponivel.", exc);
        }
    }

    private List<DadosItemDocumentalPersistente> itensDaVersao(DadosVersaoDocumentoPersistente versao) {
        return persistencia.listarItensDocumento(versao.idDocumento()).stream()
                .filter(item -> item.idVersao().equals(versao.id()))
                .sorted(Comparator.comparingInt(DadosItemDocumentalPersistente::ordem))
                .toList();
    }

    private static String nomeFisicoArquivo(MultipartFile arquivo) {
        String nome = arquivo.getOriginalFilename();
        return nome == null || nome.isBlank() ? "arquivo-recebido" : nome;
    }

    private static String tipoMidia(MultipartFile arquivo) {
        String tipo = arquivo.getContentType();
        return tipo == null || tipo.isBlank() ? "application/octet-stream" : tipo;
    }

    private Map<String, Object> respostaRecebimento(
            DadosDocumentoPersistente documento,
            DadosVersaoDocumentoPersistente versao,
            boolean criado,
            List<DadosItemDocumentalPersistente> itens
    ) {
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("documento", documento.identificador());
        resposta.put("versao", versao.numero());
        resposta.put("formato", versao.formato());
        resposta.put("criado", criado);
        resposta.put("hashConteudo", versao.hashConteudo());
        resposta.put("hashMetadados", versao.hashMetadados());
        resposta.put("chaveIdempotencia", versao.chaveIdempotencia());
        resposta.put("originalTamanho", versao.originalTamanho());
        resposta.put("originalCodificacao", versao.originalCodificacao());
        resposta.put("idVersaoAtual", versao.id().toString());
        resposta.put("itens", itens.stream().map(DadosItemDocumentalPersistente::identificador).toList());
        return resposta;
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
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("identificador", documentoPersistente.identificador());
        resposta.put("titulo", documentoPersistente.titulo());
        UUID idVersaoAtual = persistencia.consultarReferenciaVersaoAtual(documentoPersistente.id()).idVersaoAtual();
        resposta.put("idVersaoAtual", idVersaoAtual == null ? null : idVersaoAtual.toString());
        resposta.put("metadados", Map.of(
                        "grupo", documentoPersistente.grupo(),
                        "categoria", documentoPersistente.categoria(),
                        "finalidade", documentoPersistente.finalidade(),
                        "apresentacao", documentoPersistente.apresentacao()
                ));
        resposta.put("versoes", persistencia.listarVersoesDocumento(documentoPersistente.id()).stream()
                        .map(this::respostaVersaoPersistente)
                        .toList());
        resposta.put("relacoesRecebidas", persistencia.listarRelacoesDocumento(documentoPersistente.id()));
        return resposta;
    }

    private Map<String, Object> respostaVersaoPersistente(DadosVersaoDocumentoPersistente versao) {
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("id", versao.id());
        resposta.put("numero", versao.numero());
        resposta.put("formato", versao.formato());
        resposta.put("conteudoOriginal", versao.conteudoOriginal());
        resposta.put("dataRecebimento", versao.dataRecebimento());
        resposta.put("confirmada", versao.confirmada());
        resposta.put("idVersaoAnterior", uuidComoTexto(versao.idVersaoAnterior()));
        resposta.put("idPedidoAjusteOrigem", uuidComoTexto(versao.idPedidoAjusteOrigem()));
        resposta.put("hashConteudo", versao.hashConteudo());
        resposta.put("hashMetadados", versao.hashMetadados() == null ? "" : versao.hashMetadados());
        resposta.put("chaveIdempotencia", versao.chaveIdempotencia() == null ? "" : versao.chaveIdempotencia());
        resposta.put("originalTamanho", versao.originalTamanho() == null ? 0 : versao.originalTamanho());
        resposta.put("originalCodificacao", versao.originalCodificacao() == null ? "" : versao.originalCodificacao());
        resposta.put("itens", persistencia.listarItensDocumento(versao.idDocumento()).stream()
                .filter(item -> item.idVersao().equals(versao.id()))
                .sorted(Comparator.comparingInt(DadosItemDocumentalPersistente::ordem))
                .map(this::respostaItemPersistente)
                .toList());
        return resposta;
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
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("identificador", documento.identificador());
        resposta.put("titulo", documento.titulo());
        resposta.put("idVersaoAtual", null);
        resposta.put("metadados", documento.metadados());
        resposta.put("versoes", documento.versoes());
        resposta.put("relacoesRecebidas", documento.metadados().relacoesRecebidas());
        return resposta;
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

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapaObrigatorio(Map<String, Object> corpo, String campo) {
        Object valor = corpo.get(campo);
        if (!(valor instanceof Map<?, ?> mapa) || mapa.isEmpty()) {
            throw new IllegalArgumentException(campo + " deve ser informado.");
        }
        return (Map<String, Object>) mapa;
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
