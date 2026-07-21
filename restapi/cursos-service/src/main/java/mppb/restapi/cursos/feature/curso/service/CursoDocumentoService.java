package mppb.restapi.cursos.feature.curso.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.infrastructure.report.JasperService;
import mppb.restapi.common.core.error.NotFoundException;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.curso.repository.CursoRepository;
import mppb.restapi.cursos.feature.curso.dto.ListaPresencaRequestDto;
import mppb.restapi.cursos.feature.inscricao.entity.Inscricao;
import mppb.restapi.cursos.feature.inscricao.repository.InscricaoRepository;
import mppb.restapi.cursos.feature.participante.entity.Participante;
import mppb.restapi.cursos.feature.participante.enums.TipoParticipanteEnum;
import mppb.restapi.cursos.feature.curso.dto.ParticipanteDocumentoDto;
import mppb.restapi.cursos.feature.frequencia.entity.Aula;
import mppb.restapi.cursos.feature.frequencia.repository.AulaRepository;
import java.time.format.DateTimeFormatter;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import java.io.InputStream;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class CursoDocumentoService {

    private final JasperService jasperService;
    private final CursoRepository cursoRepository;
    private final InscricaoRepository inscricaoRepository;
    private final AulaRepository aulaRepository;
    private final JdbcTemplate jdbcTemplate;

    public byte[] gerarListaPresenca(Long cursoId, ListaPresencaRequestDto request) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new NotFoundException("Curso não encontrado."));

        String tipoPublico = request != null && request.getTipoPublico() != null ? request.getTipoPublico().toUpperCase() : "TODOS";

        List<ParticipanteDocumentoDto> participantes = buscarParticipantesPorTipo(cursoId, tipoPublico, true);

        String tituloLista;
        if (tipoPublico.contains(",")) {
            tituloLista = String.join(", ", tipoPublico.split(","));
        } else {
            tituloLista = switch (tipoPublico) {
                case "MEMBROS" -> "MEMBROS";
                case "SERVIDORES" -> "SERVIDORES";
                case "ASSESSORES" -> "ASSESSORES";
                case "EXTERNOS" -> "EXTERNOS";
                default -> "TODOS OS PARTICIPANTES";
            };
        }

        String tipoEvento = curso.getTipoEvento() != null ? curso.getTipoEvento().getDescricao() : "EVENTO";
        String localEvento = (request != null && request.getLocalEvento() != null && !request.getLocalEvento().isBlank())
                ? request.getLocalEvento() : (curso.getMunicipio() != null ? curso.getMunicipio().getNome() : "Local não informado");
        
        List<Aula> aulas = aulaRepository.findAllByCursoId(cursoId);
        DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        String dataEvento = (request != null && request.getDataEvento() != null && !request.getDataEvento().isBlank())
                ? request.getDataEvento() : (!aulas.isEmpty() ? aulas.stream().map(a -> a.getData().format(dtFormatter)).distinct().collect(Collectors.joining(", ")) : "Data não informada");
        
        String horarioEvento = (request != null && request.getHorarioEvento() != null && !request.getHorarioEvento().isBlank())
                ? request.getHorarioEvento() : (!aulas.isEmpty() ? aulas.stream().map(a -> "das " + a.getHoraInicio().format(timeFormatter) + " às " + a.getHoraFim().format(timeFormatter)).distinct().collect(Collectors.joining(" e ")) : "Horário não informado");
        String nomeCurso = (request != null && request.getNomeCurso() != null && !request.getNomeCurso().isBlank())
                ? request.getNomeCurso() : curso.getTitulo();

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("NOME_CURSO", nomeCurso);
        parametros.put("TIPO_EVENTO", tipoEvento);
        parametros.put("LOCAL_EVENTO", localEvento);
        parametros.put("DATA_EVENTO", dataEvento);
        parametros.put("HORARIO_EVENTO", horarioEvento);
        parametros.put("TITULO_LISTA", tituloLista);
        InputStream logo = getClass().getResourceAsStream("/reports/brasao_mppb_sem_fundo.png");
        parametros.put("LOGO_BRASAO", logo);

        return jasperService.gerarPdfDatasource("lista_presenca.jrxml", parametros, new JRBeanCollectionDataSource(participantes));
    }

    public byte[] gerarOficioCorregedoria(Long cursoId, mppb.restapi.cursos.feature.curso.dto.OficioCorregedoriaRequestDto request) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new NotFoundException("Curso não encontrado."));

        String nomeCorregedor = buscarNomeAutoridade(
                64L,
                "Corregedor-Geral do Ministério Público",
                new CriterioAutoridade("corregedor", "geral"),
                new CriterioAutoridade("corregedoria", "geral"));
        String nomeDiretorCeaf = buscarNomeAutoridade(
                65L,
                "Diretor do Centro de Estudos e Aperfeiçoamento Funcional",
                new CriterioAutoridade("diretor", "ceaf"),
                new CriterioAutoridade("diretor", "centro", "estudos", "aperfeicoamento", "funcional"));

        // Somente MEMBROS
        List<ParticipanteDocumentoDto> membros = buscarParticipantes(cursoId, true);

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("NOME_CURSO", curso.getTitulo());
        parametros.put("NOME_CORREGEDOR", nomeCorregedor);
        parametros.put("NOME_DIRETOR_CEAF", nomeDiretorCeaf);
        parametros.put("NUMERO_OFICIO", request.getNumeroOficio());
        parametros.put("ANO_OFICIO", request.getAnoOficio());
        parametros.put("ASSUNTO", request.getAssunto());
        parametros.put("TEXTO_CORPO", request.getTexto());
        InputStream logo = getClass().getResourceAsStream("/reports/brasao_mppb_sem_fundo.png");
        parametros.put("LOGO_BRASAO", logo);

        return jasperService.gerarPdfDatasource("oficio_corregedoria.jrxml", parametros, new JRBeanCollectionDataSource(membros));
    }

    public byte[] gerarOficioDrh(Long cursoId, mppb.restapi.cursos.feature.curso.dto.OficioDrhRequestDto request) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new NotFoundException("Curso não encontrado."));

        String nomeChefeDrh = buscarNomeAutoridade(
                15L,
                "Chefe do Departamento de Recursos Humanos",
                new CriterioAutoridade("chefe", "departamento", "recursos", "humanos"),
                new CriterioAutoridade("chefe", "drh"));
        String nomeDiretorCeaf = buscarNomeAutoridade(
                65L,
                "Diretor do Centro de Estudos e Aperfeiçoamento Funcional",
                new CriterioAutoridade("diretor", "ceaf"),
                new CriterioAutoridade("diretor", "centro", "estudos", "aperfeicoamento", "funcional"));
        // Tenta detectar sexo pelo próprio nome (heurística simples)
        boolean chefeFeminino = nomeChefeDrh.matches(".*\\bANA\\b.*|.*\\bMARIA\\b.*|.*\\bLUCIA\\b.*|.*\\bFERNANDA\\b.*|.*\\bKEYLA\\b.*|.*\\bPATRICIA\\b.*|.*\\bANDREA\\b.*|.*\\bSILVIA\\b.*|.*\\bJULIANA\\b.*|.*\\bRAQUEL\\b.*|.*\\bCLAUDIA\\b.*|.*\\bCARLA\\b.*|.*\\bSABRINA\\b.*|.*\\bRENATA\\b.*|.*\\bTATIANA\\b.*|.*\\bROSANE\\b.*|.*\\bCRISTINA\\b.*|.*\\bCINTIA\\b.*|.*\\bKAREN\\b.*|.*\\bMONICA\\b.*|.*\\bDENISE\\b.*|.*\\bANGELA\\b.*|.*\\bSORAYA\\b.*|.*\\bGERALDINE\\b.*");
        String pronomeVocativo = chefeFeminino ? "À Senhora" : "Ao Senhor";
        String funcaoChefe = "Chefe do Departamento de Recursos Humanos";
        String saudacao = chefeFeminino ? "Senhora Chefe," : "Senhor Chefe,";

        // TODOS (Membros, Servidores, Assessores)
        List<ParticipanteDocumentoDto> todos = buscarParticipantes(cursoId, false);

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("NOME_CURSO", curso.getTitulo());
        parametros.put("NOME_CHEFE_DRH", nomeChefeDrh);
        parametros.put("NOME_DIRETOR_CEAF", nomeDiretorCeaf);
        parametros.put("PRONOME_VOCATIVO", pronomeVocativo);
        parametros.put("FUNCAO_CHEFE", funcaoChefe);
        parametros.put("SAUDACAO", saudacao);
        parametros.put("NUMERO_OFICIO", request != null ? request.getNumeroOficio() : "");
        parametros.put("ANO_OFICIO", request != null ? request.getAnoOficio() : String.valueOf(java.time.Year.now().getValue()));
        parametros.put("ASSUNTO", request != null ? request.getAssunto() : "");
        parametros.put("TEXTO_CORPO", request != null ? request.getTexto() : "");
        InputStream logo = getClass().getResourceAsStream("/reports/brasao_mppb_sem_fundo.png");
        parametros.put("LOGO_BRASAO", logo);

        return jasperService.gerarPdfDatasource("oficio_drh.jrxml", parametros, new JRBeanCollectionDataSource(todos));
    }

    private List<ParticipanteDocumentoDto> buscarParticipantesPorTipo(Long cursoId, String tipoPublico, boolean apenasAprovados) {
        List<Inscricao> inscricoes = inscricaoRepository.findAllByCursoId(cursoId, Pageable.unpaged()).getContent();

        return inscricoes.stream()
                .filter(i -> !apenasAprovados || i.getSituacao() == mppb.restapi.cursos.feature.inscricao.enums.SituacaoInscricaoEnum.APROVADO)
                .map(Inscricao::getParticipante)
                .filter(p -> {
                    if (tipoPublico == null || tipoPublico.isEmpty() || tipoPublico.contains("TODOS")) return true;
                    if ("DRH".equals(tipoPublico)) return isMembro(p) || isServidor(p) || isAssessor(p);
                    
                    boolean match = false;
                    List<String> tipos = Arrays.asList(tipoPublico.split(","));
                    if (tipos.contains("MEMBROS") && isMembro(p)) match = true;
                    if (tipos.contains("SERVIDORES") && isServidor(p)) match = true;
                    if (tipos.contains("ASSESSORES") && isAssessor(p)) match = true;
                    if (tipos.contains("EXTERNOS") && !isMembro(p) && !isServidor(p) && !isAssessor(p)) match = true;
                    return match;
                })
                .map(p -> {
                    String grupo = "Externos";
                    int ordemGrupo = 4;
                    if (isMembro(p)) { grupo = "Membros"; ordemGrupo = 1; }
                    else if (isServidor(p)) { grupo = "Servidores"; ordemGrupo = 2; }
                    else if (isAssessor(p)) { grupo = "Assessores"; ordemGrupo = 3; }

                    return ParticipanteDocumentoDto.builder()
                        .nome(p.getNome() != null ? p.getNome() : p.getNomeServidor())
                        .cargo(p.getCargoServidor() != null ? p.getCargoServidor() : "Não informado")
                        .matricula(p.getMatriculaServidor() != null ? p.getMatriculaServidor() : "Não informado")
                        .tipo(p.getTipo() != null ? p.getTipo().getDescricao() : "")
                        .grupo(grupo)
                        .ordemGrupo(ordemGrupo)
                        .build();
                })
                .sorted(Comparator.comparing(ParticipanteDocumentoDto::getOrdemGrupo)
                                  .thenComparing(ParticipanteDocumentoDto::getNome))
                .collect(Collectors.toList());
    }

    private List<ParticipanteDocumentoDto> buscarParticipantes(Long cursoId, boolean apenasMembros) {
        return buscarParticipantesPorTipo(cursoId, apenasMembros ? "MEMBROS" : "DRH", true); // Ofícios só enviam APROVADOS
    }

    private boolean isMembro(Participante p) {
        if (p.getCargoServidor() == null) return false;
        String cargo = p.getCargoServidor().toUpperCase();
        return cargo.contains("PROMOTOR") || cargo.contains("PROCURADOR");
    }

    private boolean isServidor(Participante p) {
        if (p.getCargoServidor() == null) return false;
        String cargo = p.getCargoServidor().toUpperCase();
        return !isMembro(p) && !isAssessor(p)
               && (p.getTipo() == null || !p.getTipo().name().toUpperCase().contains("EXTERNO"));
    }

    private boolean isAssessor(Participante p) {
        if (p.getTipo() == null) return false;
        String tipo = p.getTipo().name().toUpperCase();
        return tipo.contains("ASSESSOR");
    }

    private String buscarNomeAutoridade(Long orgaoExecucaoId, String fallbackRoleName, CriterioAutoridade... criterios) {
        String queryPorId = """
                SELECT servidor_nome
                  FROM diariooficial.mapa_por_membro_vw
                 WHERE ativo = 'A'
                   AND orgao_execucao_id = ?
                   AND servidor_nome IS NOT NULL
                   AND trim(servidor_nome) <> ''
                 ORDER BY orgao_execucao_id
                 LIMIT 1
                """;
        Optional<String> nomePorId = consultarNomeAutoridade(queryPorId, orgaoExecucaoId);
        if (nomePorId.isPresent()) {
            return nomePorId.get();
        }

        StringBuilder queryPorNomeOrgao = new StringBuilder("""
                SELECT servidor_nome
                  FROM diariooficial.mapa_por_membro_vw
                 WHERE ativo = 'A'
                   AND servidor_nome IS NOT NULL
                   AND trim(servidor_nome) <> ''
                   AND (
                """);
        List<Object> params = new ArrayList<>();

        for (int c = 0; c < criterios.length; c++) {
            if (c > 0) {
                queryPorNomeOrgao.append(" OR ");
            }
            CriterioAutoridade criterio = criterios[c];
            queryPorNomeOrgao.append("(");
            for (int i = 0; i < criterio.termos().length; i++) {
                if (i > 0) {
                    queryPorNomeOrgao.append(" AND ");
                }
                queryPorNomeOrgao.append("UPPER(orgao_execucao_nome) LIKE ?");
                params.add("%" + criterio.termos()[i].toUpperCase() + "%");
            }
            queryPorNomeOrgao.append(")");
        }

        queryPorNomeOrgao.append("""
                       )
                 ORDER BY
                       CASE
                """);
        for (CriterioAutoridade criterio : criterios) {
            queryPorNomeOrgao.append(" WHEN ");
            for (int i = 0; i < criterio.termos().length; i++) {
                if (i > 0) {
                    queryPorNomeOrgao.append(" AND ");
                }
                queryPorNomeOrgao.append("UPPER(orgao_execucao_nome) LIKE ?");
                params.add("%" + criterio.termos()[i].toUpperCase() + "%");
            }
            queryPorNomeOrgao.append(" THEN 0");
        }
        queryPorNomeOrgao.append("""
                         ELSE 1
                       END,
                       orgao_execucao_id
                 LIMIT 1
                """);

        Optional<String> nomePorOrgao = consultarNomeAutoridade(queryPorNomeOrgao.toString(), params.toArray());
        if (nomePorOrgao.isPresent()) {
            return nomePorOrgao.get();
        }

        log.warn("Autoridade '{}' não encontrada na view diariooficial.mapa_por_membro_vw. Usando fallback.", fallbackRoleName);
        return fallbackRoleName;
    }

    private Optional<String> consultarNomeAutoridade(String query, Object... params) {
        try {
            List<String> nomes = jdbcTemplate.query(query, params,
                    (rs, rowNum) -> rs.getString("servidor_nome"));
            if (!nomes.isEmpty() && nomes.get(0) != null && !nomes.get(0).isBlank()) {
                return Optional.of(nomes.get(0));
            }
        } catch (Exception e) {
            log.warn("Erro ao consultar autoridade na view diariooficial.mapa_por_membro_vw.", e);
        }
        return Optional.empty();
    }

    private record CriterioAutoridade(String... termos) {
    }
}
