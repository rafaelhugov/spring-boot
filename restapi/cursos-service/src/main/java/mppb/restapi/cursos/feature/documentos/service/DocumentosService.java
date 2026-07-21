package mppb.restapi.cursos.feature.documentos.service;

import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.curso.entity.CursoMinistrante;
import mppb.restapi.cursos.feature.curso.repository.CursoMinistranteRepository;
import mppb.restapi.cursos.feature.curso.repository.CursoRepository;
import mppb.restapi.cursos.feature.documentos.dto.GeracaoDocumentoRequest;
import mppb.restapi.cursos.feature.frequencia.entity.Aula;
import mppb.restapi.cursos.feature.frequencia.repository.AulaRepository;
import mppb.restapi.cursos.feature.inscricao.entity.Inscricao;
import mppb.restapi.cursos.feature.inscricao.repository.InscricaoRepository;
import mppb.restapi.cursos.feature.participante.entity.Participante;
import mppb.restapi.cursos.feature.participante.enums.TipoParticipanteEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Arrays;

@Slf4j
@Service
public class DocumentosService {

    private final CursoRepository cursoRepository;
    private final CursoMinistranteRepository cursoMinistranteRepository;
    private final AulaRepository aulaRepository;
    private final InscricaoRepository inscricaoRepository;
    private final PdfGeneratorService pdfGeneratorService;
    private final JdbcTemplate jdbcTemplate;

    public DocumentosService(CursoRepository cursoRepository,
                             CursoMinistranteRepository cursoMinistranteRepository,
                             AulaRepository aulaRepository,
                             InscricaoRepository inscricaoRepository,
                             PdfGeneratorService pdfGeneratorService,
                             JdbcTemplate jdbcTemplate) {
        this.cursoRepository = cursoRepository;
        this.cursoMinistranteRepository = cursoMinistranteRepository;
        this.aulaRepository = aulaRepository;
        this.inscricaoRepository = inscricaoRepository;
        this.pdfGeneratorService = pdfGeneratorService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public byte[] gerarDocumento(Long cursoId, GeracaoDocumentoRequest request) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new IllegalArgumentException("Curso não encontrado"));

        List<CursoMinistrante> ministrantes = cursoMinistranteRepository.findByCursoId(cursoId);
        List<Aula> aulas = aulaRepository.findAllByCursoId(cursoId);
        
        List<Inscricao> inscricoes = inscricaoRepository
                .findAllByCursoIdWithFilters(cursoId, null, request.getSituacaoInscricao(), Pageable.unpaged())
                .getContent();

        Map<String, Object> vars = new HashMap<>();
        
        // Dados do ofício
        vars.put("numero_oficio", request.getNumeroOficio());
        vars.put("ano_oficio", request.getAnoOficio());
        vars.put("unidade_emissora", "CEAF/MPPB");
        DadosAutoridades autoridades = resolverAutoridades(request);
        vars.put("destinatario_nome", autoridades.destinatarioNome());
        vars.put("destinatario_cargo", autoridades.destinatarioCargo());
        vars.put("orgao_destinatario", request.getOrgaoDestinatario());
        vars.put("assunto", request.getAssunto());
        vars.put("tratamento_destinatario", request.getTratamentoDestinatario());
        vars.put("pronome_tratamento", request.getPronomeTratamento());
        vars.put("finalidade_anotacao", request.getFinalidadeAnotacao());
        vars.put("assinante_nome", autoridades.assinanteNome());
        vars.put("assinante_cargo", autoridades.assinanteCargo());
        
        // Dados do curso
        vars.put("titulo_evento", (request.getNomeCurso() != null && !request.getNomeCurso().isEmpty()) ? request.getNomeCurso() : curso.getTitulo());
        vars.put("tipo_evento", curso.getTipoEvento() != null ? curso.getTipoEvento().getDescricao() : "Evento");
        vars.put("descricao_complementar_evento", request.getDescricaoComplementarEvento() != null ? " " + request.getDescricaoComplementarEvento() : "");
        vars.put("modalidade", curso.getModalidade() != null ? curso.getModalidade().getDescricao() : "");
        vars.put("carga_horaria", curso.getCargaHoraria() != null ? curso.getCargaHoraria() + " horas" : "");
        vars.put("publico_alvo", request.getTipoPublico());
        vars.put("forma_divulgacao", "sítio e e-mail institucionais");
        vars.put("orgao_promotor", request.getOrgaoPromotor());
        vars.put("brasao_src", getBrasaoDataUri());

        // Datas / Locais
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String dataRealizacao = (request.getDataEvento() != null && !request.getDataEvento().isEmpty())
                ? request.getDataEvento() 
                : (!aulas.isEmpty() ?
                        aulas.stream()
                                .map(a -> a.getData().format(dtf))
                                .distinct()
                                .collect(Collectors.joining(", ")) : "Data não informada");
        
        vars.put("data_realizacao", dataRealizacao);
        vars.put("periodo_realizacao", dataRealizacao);
        vars.put("data", dataRealizacao);
        
        if (request.getLocalEvento() != null && !request.getLocalEvento().isEmpty()) {
            vars.put("plataforma_ou_local", request.getLocalEvento());
            vars.put("local", request.getLocalEvento());
        } else {
            String locais = aulas.stream()
                    .map(Aula::getDescricao)
                    .distinct()
                    .collect(Collectors.joining(", "));
            vars.put("plataforma_ou_local", locais);
            vars.put("local", locais);
        }

        // Horário
        String horarioEvento = (request.getHorarioEvento() != null && !request.getHorarioEvento().isEmpty())
                ? request.getHorarioEvento()
                : (!aulas.isEmpty() ? aulas.get(0).getHoraInicio().toString() + " às " + aulas.get(0).getHoraFim().toString() : "");
        vars.put("horario", horarioEvento);

        // Ministrantes
        List<String> nomesPalestrantes = new ArrayList<>();
        List<String> nomesMediadores = new ArrayList<>();
        List<String> nomesDebatedores = new ArrayList<>();
        
        for (CursoMinistrante cm : ministrantes) {
            String nome = cm.getMinistrante().getNome();
            String papel = cm.getPapel() != null ? cm.getPapel().getNome().toLowerCase() : "";
            if (papel.contains("palestrante") || papel.contains("instrutor")) {
                nomesPalestrantes.add(nome);
            } else if (papel.contains("mediador")) {
                nomesMediadores.add(nome);
            } else if (papel.contains("debatedor")) {
                nomesDebatedores.add(nome);
            } else {
                nomesPalestrantes.add(nome); // fallback
            }
        }
        
        vars.put("palestrantes", nomesPalestrantes);
        vars.put("mediadores", nomesMediadores);
        vars.put("debatedores", nomesDebatedores);

        // Participantes
        List<ParticipanteDTO> participantes = inscricoes.stream()
                .map(i -> new ParticipanteDTO(nomeParticipante(i.getParticipante()), i.getParticipante().getMatriculaServidor()))
                .collect(Collectors.toList());
        vars.put("participantes", participantes);
        
        // Para lista de presença
        if ("LISTA_PRESENCA".equals(request.getTipoDocumento())) {
            List<String> filtros = request.getTipoPublico() != null ? Arrays.asList(request.getTipoPublico().toUpperCase().split(",")) : new ArrayList<>();
            boolean todos = filtros.isEmpty() || filtros.contains("TODOS");

            Map<String, List<Map<String, Object>>> grupos = new HashMap<>();
            grupos.put("MEMBROS", new ArrayList<>());
            grupos.put("SERVIDORES", new ArrayList<>());
            grupos.put("ASSESSORES", new ArrayList<>());
            grupos.put("EXTERNOS", new ArrayList<>());

            for (Inscricao inscricao : inscricoes) {
                Participante p = inscricao.getParticipante();
                String categoria = classificarParticipante(p);

                // Aplica o filtro selecionado pelo usuário (se não for "TODOS")
                if (todos || filtros.contains(categoria)) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("nome", nomeParticipante(p));
                    grupos.get(categoria).add(map);
                }
            }

            List<Map<String, Object>> gruposParticipantes = new ArrayList<>();
            String[] ordemGrupos = {"MEMBROS", "SERVIDORES", "ASSESSORES", "EXTERNOS"};

            for (String cat : ordemGrupos) {
                List<Map<String, Object>> lista = grupos.get(cat);
                if (!lista.isEmpty()) {
                    // Ordenar alfabeticamente e atribuir numeração
                    lista.sort((m1, m2) -> {
                        String n1 = (String) m1.get("nome");
                        String n2 = (String) m2.get("nome");
                        n1 = n1 != null ? n1 : "";
                        n2 = n2 != null ? n2 : "";
                        return n1.compareToIgnoreCase(n2);
                    });
                    for (int i = 0; i < lista.size(); i++) {
                        lista.get(i).put("numero", i + 1);
                    }
                    Map<String, Object> grupoInfo = new HashMap<>();
                    grupoInfo.put("nomeGrupo", cat);
                    grupoInfo.put("participantes", lista);
                    gruposParticipantes.add(grupoInfo);
                }
            }

            vars.put("grupos_participantes", gruposParticipantes);

            int adic = request.getQuantidadeLinhasAdicionais() != null ? request.getQuantidadeLinhasAdicionais() : 0;
            List<Map<String, Object>> linhasAdicionais = new ArrayList<>();
            for (int i = 0; i < adic; i++) {
                Map<String, Object> map = new HashMap<>();
                map.put("numero", i + 1);
                linhasAdicionais.add(map);
            }
            vars.put("linhas_adicionais", linhasAdicionais);
            
            vars.put("tipo_publico", formatarTipoPublico(request.getTipoPublico()));
            vars.put("titulo_documento", "APROVADO".equals(request.getSituacaoInscricao() != null ? request.getSituacaoInscricao().name() : null) ? "LISTA DE PRESENÇA" : "LISTA DE INSCRITOS");
        }

        // Determinar template
        String templateName = "";
        switch (request.getTipoDocumento()) {
            case "OFICIO_DRH":
                templateName = "oficio_drh.html";
                break;
            case "OFICIO_CORREGEDORIA":
                templateName = "oficio_corregedoria.html";
                break;
            case "OFICIO_CONSOLIDADO":
                templateName = "oficio_consolidado.html";
                break;
            case "LISTA_PRESENCA":
                templateName = "lista_presenca.html";
                break;
            default:
                templateName = "oficio_drh.html";
        }

        return pdfGeneratorService.generatePdf(templateName, vars);
    }

    private String classificarParticipante(Participante p) {
        if (p.getTipo() == TipoParticipanteEnum.EXTERNO) {
            return "EXTERNOS";
        }
        String cargo = p.getCargoServidor() != null ? p.getCargoServidor().toUpperCase() : "";
        if (cargo.contains("PROMOTOR") || cargo.contains("PROCURADOR")) {
            return "MEMBROS";
        } else if (cargo.contains("ASSESSOR")) {
            return "ASSESSORES";
        } else {
            return "SERVIDORES";
        }
    }

    private String nomeParticipante(Participante p) {
        if (p == null) {
            return "";
        }
        if (p.getNome() != null && !p.getNome().isBlank()) {
            return p.getNome();
        }
        if (p.getNomeServidor() != null && !p.getNomeServidor().isBlank()) {
            return p.getNomeServidor();
        }
        return "";
    }

    private DadosAutoridades resolverAutoridades(GeracaoDocumentoRequest request) {
        String tipoDocumento = request.getTipoDocumento();
        String destinatarioNome = request.getDestinatarioNome();
        String destinatarioCargo = request.getDestinatarioCargo();
        String assinanteNome = request.getAssinanteNome();
        String assinanteCargo = textoOuPadrao(request.getAssinanteCargo(), "Diretor do CEAF");

        if (isBlank(assinanteNome) || "Diretor do Centro de Estudos e Aperfeiçoamento Funcional".equalsIgnoreCase(assinanteNome)) {
            assinanteNome = buscarNomeAutoridade(
                    65L,
                    "",
                    new CriterioAutoridade("diretor", "ceaf"),
                    new CriterioAutoridade("diretor", "centro", "estudos", "aperfeicoamento", "funcional"));
        }

        if ("OFICIO_DRH".equals(tipoDocumento)) {
            destinatarioNome = textoOuPadrao(destinatarioNome, buscarNomeAutoridade(
                    15L,
                    "Chefe do Departamento de Recursos Humanos",
                    new CriterioAutoridade("chefe", "departamento", "recursos", "humanos"),
                    new CriterioAutoridade("chefe", "drh")));
            destinatarioCargo = textoOuPadrao(destinatarioCargo, "Chefe do Departamento de Recursos Humanos");
        } else if ("OFICIO_CORREGEDORIA".equals(tipoDocumento)) {
            destinatarioNome = textoOuPadrao(destinatarioNome, buscarNomeAutoridade(
                    64L,
                    "Corregedor-Geral do Ministério Público",
                    new CriterioAutoridade("corregedor", "geral"),
                    new CriterioAutoridade("corregedoria", "geral")));
            destinatarioCargo = textoOuPadrao(destinatarioCargo, "Corregedor-Geral");
        } else if ("OFICIO_CONSOLIDADO".equals(tipoDocumento)) {
            if (isBlank(destinatarioNome) && contem(request.getTratamentoDestinatario(), "corregedor")) {
                destinatarioNome = buscarNomeAutoridade(
                        64L,
                        "Corregedor-Geral do Ministério Público",
                        new CriterioAutoridade("corregedor", "geral"),
                        new CriterioAutoridade("corregedoria", "geral"));
            } else if (isBlank(destinatarioNome) && contem(request.getTratamentoDestinatario(), "chefe")) {
                destinatarioNome = buscarNomeAutoridade(
                        15L,
                        "Chefe do Departamento de Recursos Humanos",
                        new CriterioAutoridade("chefe", "departamento", "recursos", "humanos"),
                        new CriterioAutoridade("chefe", "drh"));
            }
        }

        return new DadosAutoridades(destinatarioNome, destinatarioCargo, assinanteNome, assinanteCargo);
    }

    private String buscarNomeAutoridade(Long orgaoExecucaoId, String fallback, CriterioAutoridade... criterios) {
        String textoOrgaoExecucao = "lower(funcaosemacentos(orgao_execucao_nome))";
        StringBuilder query = new StringBuilder("""
                SELECT servidor_nome
                  FROM diariooficial.mapa_por_membro_vw
                 WHERE ativo = 'A'
                   AND (
                        orgao_execucao_id = ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(orgaoExecucaoId);

        for (CriterioAutoridade criterio : criterios) {
            query.append(" OR (");
            for (int i = 0; i < criterio.termos().length; i++) {
                if (i > 0) {
                    query.append(" AND ");
                }
                query.append(textoOrgaoExecucao).append(" LIKE lower(funcaosemacentos(?))");
                params.add("%" + criterio.termos()[i] + "%");
            }
            query.append(")");
        }

        query.append("""
                       )
                 ORDER BY
                       CASE
                """);
        for (CriterioAutoridade criterio : criterios) {
            query.append(" WHEN ");
            for (int i = 0; i < criterio.termos().length; i++) {
                if (i > 0) {
                    query.append(" AND ");
                }
                query.append(textoOrgaoExecucao).append(" LIKE lower(funcaosemacentos(?))");
                params.add("%" + criterio.termos()[i] + "%");
            }
            query.append(" THEN 0");
        }
        query.append("""
                         WHEN orgao_execucao_id = ? THEN 1
                         ELSE 2
                       END,
                       orgao_execucao_id
                 LIMIT 1
                """);
        params.add(orgaoExecucaoId);

        try {
            List<String> nomes = jdbcTemplate.query(query.toString(), params.toArray(),
                    (rs, rowNum) -> rs.getString("servidor_nome"));
            if (!nomes.isEmpty() && !isBlank(nomes.get(0))) {
                return nomes.get(0);
            }
        } catch (Exception e) {
            log.warn("Erro ao consultar autoridade na view diariooficial.mapa_por_membro_vw. Usando fallback.", e);
        }
        return fallback;
    }

    private boolean contem(String texto, String termo) {
        return texto != null && texto.toLowerCase().contains(termo);
    }

    private String textoOuPadrao(String texto, String padrao) {
        return isBlank(texto) ? padrao : texto;
    }

    private boolean isBlank(String texto) {
        return texto == null || texto.isBlank();
    }

    private String getBrasaoDataUri() {
        try (InputStream is = getClass().getResourceAsStream("/reports/brasao_mppb_sem_fundo.png")) {
            if (is == null) {
                return "";
            }
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(is.readAllBytes());
        } catch (IOException e) {
            return "";
        }
    }

    private String formatarTipoPublico(String tipoPublico) {
        if (tipoPublico == null || tipoPublico.isBlank()) {
            return "TODOS";
        }
        List<String> tipos = Arrays.stream(tipoPublico.toUpperCase().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        if (tipos.isEmpty() || tipos.contains("TODOS")) {
            return "TODOS";
        }
        return String.join(", ", tipos);
    }

    private record DadosAutoridades(
            String destinatarioNome,
            String destinatarioCargo,
            String assinanteNome,
            String assinanteCargo) {
    }

    private record CriterioAutoridade(String... termos) {
    }
    
    public static class ParticipanteDTO {
        private String nome;
        private String matricula;
        
        public ParticipanteDTO(String nome, String matricula) {
            this.nome = nome;
            this.matricula = matricula;
        }
        
        public String getNome() { return nome; }
        public String getMatricula() { return matricula; }
    }
}
