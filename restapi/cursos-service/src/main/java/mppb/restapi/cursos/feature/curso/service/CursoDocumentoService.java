package mppb.restapi.cursos.feature.curso.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.infrastructure.report.JasperService;
import mppb.restapi.common.core.error.NotFoundException;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.curso.repository.CursoRepository;
import mppb.restapi.cursos.feature.inscricao.entity.Inscricao;
import mppb.restapi.cursos.feature.inscricao.repository.InscricaoRepository;
import mppb.restapi.cursos.feature.participante.entity.Participante;
import mppb.restapi.cursos.feature.participante.enums.TipoParticipanteEnum;
import mppb.restapi.cursos.feature.curso.dto.ParticipanteDocumentoDto;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CursoDocumentoService {

    private final JasperService jasperService;
    private final CursoRepository cursoRepository;
    private final InscricaoRepository inscricaoRepository;
    private final JdbcTemplate jdbcTemplate;

    public byte[] gerarListaPresenca(Long cursoId) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new NotFoundException("Curso não encontrado."));

        List<ParticipanteDocumentoDto> participantes = buscarParticipantes(cursoId, false);

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("NOME_CURSO", curso.getTitulo());

        return jasperService.gerarPdfDatasource("lista_presenca.jrxml", parametros, new JRBeanCollectionDataSource(participantes));
    }

    public byte[] gerarOficioCorregedoria(Long cursoId) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new NotFoundException("Curso não encontrado."));

        String nomeCorregedor = buscarNomeAutoridade(64L, "Corregedor-Geral do Ministério Público");
        String nomeDiretorCeaf = buscarNomeAutoridade(65L, "Diretor do Centro de Estudos e Aperfeiçoamento Funcional");

        // Somente MEMBROS
        List<ParticipanteDocumentoDto> membros = buscarParticipantes(cursoId, true);

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("NOME_CURSO", curso.getTitulo());
        parametros.put("NOME_CORREGEDOR", nomeCorregedor);
        parametros.put("NOME_DIRETOR_CEAF", nomeDiretorCeaf);

        return jasperService.gerarPdfDatasource("oficio_corregedoria.jrxml", parametros, new JRBeanCollectionDataSource(membros));
    }

    public byte[] gerarOficioDrh(Long cursoId) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new NotFoundException("Curso não encontrado."));

        String nomeChefeDrh = buscarNomeAutoridade(15L, "Chefe do Departamento de Recursos Humanos");
        String nomeDiretorCeaf = buscarNomeAutoridade(65L, "Diretor do Centro de Estudos e Aperfeiçoamento Funcional");

        // TODOS (Membros, Servidores, Assessores)
        List<ParticipanteDocumentoDto> todos = buscarParticipantes(cursoId, false);

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("NOME_CURSO", curso.getTitulo());
        parametros.put("NOME_CHEFE_DRH", nomeChefeDrh);
        parametros.put("NOME_DIRETOR_CEAF", nomeDiretorCeaf);

        return jasperService.gerarPdfDatasource("oficio_drh.jrxml", parametros, new JRBeanCollectionDataSource(todos));
    }

    private List<ParticipanteDocumentoDto> buscarParticipantes(Long cursoId, boolean apenasMembros) {
        List<Inscricao> inscricoes = inscricaoRepository.findAllByCursoId(cursoId, Pageable.unpaged()).getContent();

        return inscricoes.stream()
                .map(Inscricao::getParticipante)
                .filter(p -> !apenasMembros || isMembro(p))
                .map(p -> ParticipanteDocumentoDto.builder()
                        .nome(p.getNome() != null ? p.getNome() : p.getNomeServidor())
                        .cargo(p.getCargoServidor() != null ? p.getCargoServidor() : "Não informado")
                        .matricula(p.getMatriculaServidor() != null ? p.getMatriculaServidor() : "Não informado")
                        .tipo(p.getTipo() != null ? p.getTipo().getDescricao() : "")
                        .build())
                .sorted(Comparator.comparing(ParticipanteDocumentoDto::getNome))
                .collect(Collectors.toList());
    }

    private boolean isMembro(Participante p) {
        // Assume-se que membro tem cargo de Promotor ou Procurador
        if (p.getCargoServidor() == null) {
            return false;
        }
        String cargo = p.getCargoServidor().toUpperCase();
        return cargo.contains("PROMOTOR") || cargo.contains("PROCURADOR");
    }

    private String buscarNomeAutoridade(Long orgaoExecucaoId, String fallbackRoleName) {
        String query = "SELECT servidor_nome FROM diariooficial.mapa_por_membro_vw WHERE orgao_execucao_id = ? AND ativo = 'A' LIMIT 1";
        try {
            List<String> nomes = jdbcTemplate.query(query, new Object[]{orgaoExecucaoId},
                    (rs, rowNum) -> rs.getString("servidor_nome"));
            if (!nomes.isEmpty() && nomes.get(0) != null && !nomes.get(0).isBlank()) {
                return nomes.get(0);
            }
        } catch (Exception e) {
            log.warn("Erro ao consultar view diariooficial.mapa_por_membro_vw para orgao_execucao_id {}. Usando fallback.", orgaoExecucaoId, e);
        }
        return fallbackRoleName;
    }
}
