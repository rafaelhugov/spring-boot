package br.gov.mppb.athena.api;

import br.gov.mppb.athena.aplicacao.AvaliacaoRegistrada;
import br.gov.mppb.athena.aplicacao.ComandoRegistrarAvaliacao;
import br.gov.mppb.athena.aplicacao.DecisaoAvaliacao;
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
import br.gov.mppb.athena.seguranca.ContextoAutorizacaoInstitucional;
import br.gov.mppb.athena.seguranca.OperacaoProjeto;
import br.gov.mppb.athena.seguranca.PoliticaSegurancaAthena;
import br.gov.mppb.athena.seguranca.ResultadoAutorizacao;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
public class AthenaHttpController {

    private final ServicoAvaliacaoConfirmacao avaliacoes = new ServicoAvaliacaoConfirmacao();
    private final ServicoConsultaParticipantesCorporativos consultaParticipantes;
    private final UsuarioInstitucional responsavel = usuario("usuario-avaliador", "avaliador", "DITEC");
    private final ProjetoInstitucional projeto = projeto();
    private final Documento documento = documento();

    public AthenaHttpController() {
        this(new ServicoConsultaParticipantesCorporativos(FonteCorporativaParticipantesInstitucional.cadastroGovernadoPadrao()));
    }

    AthenaHttpController(ServicoConsultaParticipantesCorporativos consultaParticipantes) {
        if (consultaParticipantes == null) {
            throw new IllegalArgumentException("servico de consulta corporativa deve ser informado.");
        }
        this.consultaParticipantes = consultaParticipantes;
    }

    @GetMapping("/{projetoId}")
    public Map<String, Object> consultarProjeto(
            @PathVariable String projetoId,
            @RequestHeader Map<String, String> headers
    ) {
        autorizar(projetoId, headers, OperacaoProjeto.CONSULTAR);
        return respostaProjeto();
    }

    @GetMapping("/{projetoId}/documentos")
    public Map<String, Object> listarDocumentos(
            @PathVariable String projetoId,
            @RequestHeader Map<String, String> headers
    ) {
        autorizar(projetoId, headers, OperacaoProjeto.CONSULTAR);
        return Map.of("documentos", List.of(respostaDocumentoResumido()));
    }

    @GetMapping("/{projetoId}/documentos/{documentoId}")
    public Map<String, Object> consultarDocumento(
            @PathVariable String projetoId,
            @PathVariable String documentoId,
            @RequestHeader Map<String, String> headers
    ) {
        autorizar(projetoId, headers, OperacaoProjeto.CONSULTAR);
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
        autorizar(projetoId, headers, OperacaoProjeto.CONSULTAR);
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

    @PostMapping("/{projetoId}/documentos/{documentoId}/avaliacoes")
    public ResponseEntity<AvaliacaoRegistrada> registrarAvaliacao(
            @PathVariable String projetoId,
            @PathVariable String documentoId,
            @RequestHeader Map<String, String> headers,
            @RequestBody Map<String, Object> corpo
    ) {
        validarDocumento(documentoId);
        ContextoAutorizacaoInstitucional contexto = contexto(headers);
        AvaliacaoRegistrada avaliacao = avaliacoes.registrarAvaliacao(
                projetoValidado(projetoId),
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

    private void autorizar(String projetoId, Map<String, String> headers, OperacaoProjeto operacao) {
        ResultadoAutorizacao resultado = PoliticaSegurancaAthena.autorizar(projetoValidado(projetoId), contexto(headers), operacao);
        if (!resultado.permitido() && resultado.motivo().contains("identidade institucional")) {
            throw new RespostaApiException(HttpStatus.UNAUTHORIZED, "IDENTIDADE_INSTITUCIONAL_AUSENTE", resultado.motivo());
        }
        if (!resultado.permitido()) {
            throw new RespostaApiException(HttpStatus.FORBIDDEN, "OPERACAO_NAO_AUTORIZADA", resultado.motivo());
        }
    }

    private ProjetoInstitucional projetoValidado(String projetoId) {
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
