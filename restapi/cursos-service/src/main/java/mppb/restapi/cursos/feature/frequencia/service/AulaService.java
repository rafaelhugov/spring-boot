package mppb.restapi.cursos.feature.frequencia.service;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.error.NotFoundException;
import mppb.restapi.common.core.service.AbstractService;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.curso.enums.StatusCursoEnum;
import mppb.restapi.cursos.feature.curso.repository.CursoRepository;
import mppb.restapi.cursos.feature.frequencia.converter.AulaConverter;
import mppb.restapi.cursos.feature.frequencia.dto.AulaDto;
import mppb.restapi.cursos.feature.frequencia.entity.Aula;
import mppb.restapi.cursos.feature.frequencia.entity.Chamada;
import mppb.restapi.cursos.feature.frequencia.repository.AulaRepository;
import mppb.restapi.cursos.feature.frequencia.repository.ChamadaRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@Transactional
public class AulaService extends AbstractService<Aula, Long> {

    private final AulaRepository aulaRepository;
    private final ChamadaRepository chamadaRepository;
    private final CursoRepository cursoRepository;
    private final AulaConverter aulaConverter; // Injeção
    private final FrequenciaService frequenciaService;

    public AulaService(EntityManager entityManager, AulaRepository aulaRepository, ChamadaRepository chamadaRepository, CursoRepository cursoRepository, AulaConverter aulaConverter, @Lazy FrequenciaService frequenciaService) {
        super(entityManager, aulaRepository);
        this.aulaRepository = aulaRepository;
        this.chamadaRepository = chamadaRepository;
        this.cursoRepository = cursoRepository;
        this.aulaConverter = aulaConverter; // Atribuição
        this.frequenciaService = frequenciaService;
    }


    /**
     * Busca todas as aulas do curso e as converte para DTO, garantindo que
     * o carregamento da Chamada (JOIN FETCH) e a conversão ocorram na transação.
     * @param cursoId ID do curso.
     * @return Lista de AulaDto.
     */
    @Transactional(readOnly = true)
    public List<AulaDto> findAllByCursoId(Long cursoId) {
        List<Aula> aulas = aulaRepository.findAllByCursoId(cursoId);
        // A conversão ocorre aqui, garantindo o acesso às chamadas antes de fechar a sessão.
        return aulaConverter.toDto(aulas);
    }
    public Aula createAula(Long cursoId, Aula aula) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new NotFoundException("Curso não encontrado: " + cursoId));

        validarAula(aula, curso, null);
        aula.setCurso(curso);
        // O @PrePersist na entidade Chamada garante que tokens sejam gerados se adicionarmos chamadas aqui
        Aula savedAula = aulaRepository.save(aula);
        recalcularDadosCurso(curso);
        frequenciaService.consolidarFrequenciasPorCurso(cursoId);
        return savedAula;
    }

    public Aula updateAula(Long cursoId, Long aulaId, Aula aulaAtualizada) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new NotFoundException("Curso não encontrado: " + cursoId));

        Aula aulaExistente = aulaRepository.findById(aulaId)
                .orElseThrow(() -> new NotFoundException("Aula não encontrada: " + aulaId));

        if (!aulaExistente.getCurso().getId().equals(cursoId)) {
            throw new mppb.restapi.common.core.error.BusinessRuleException("A aula não pertence a este curso.");
        }

        validarAula(aulaAtualizada, curso, aulaId);
        
        aulaExistente.setDescricao(aulaAtualizada.getDescricao());
        aulaExistente.setData(aulaAtualizada.getData());
        aulaExistente.setHoraInicio(aulaAtualizada.getHoraInicio());
        aulaExistente.setHoraFim(aulaAtualizada.getHoraFim());
        if (aulaAtualizada.getQuantidadeChamadas() != null) {
            aulaExistente.setQuantidadeChamadas(aulaAtualizada.getQuantidadeChamadas());
        }

        Aula savedAula = aulaRepository.save(aulaExistente);
        // Força o flush para que o JPA escreva as mudanças antes da query JPQL em recalcularDadosCurso
        aulaRepository.flush();
        recalcularDadosCurso(curso);
        frequenciaService.consolidarFrequenciasPorCurso(cursoId);
        return savedAula;
    }

    public void deleteAula(Long cursoId, Long aulaId) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new NotFoundException("Curso não encontrado: " + cursoId));

        Aula aula = aulaRepository.findById(aulaId)
                .orElseThrow(() -> new NotFoundException("Aula não encontrada: " + aulaId));

        if (!aula.getCurso().getId().equals(cursoId)) {
            throw new mppb.restapi.common.core.error.BusinessRuleException("A aula não pertence a este curso.");
        }

        frequenciaService.removerPorAula(aulaId);
        aulaRepository.delete(aula);
        aulaRepository.flush(); // Garante que a deleção seja persistida antes do recalculo
        recalcularDadosCurso(curso);
        frequenciaService.consolidarFrequenciasPorCurso(cursoId);
    }

    private void validarAula(Aula aula, Curso curso, Long aulaIdIgnorada) {
        if (aula.getHoraInicio() == null || aula.getHoraFim() == null) {
            throw new mppb.restapi.common.core.error.BusinessRuleException("Hora de início e fim são obrigatórias.");
        }
        if (!aula.getHoraFim().isAfter(aula.getHoraInicio())) {
            throw new mppb.restapi.common.core.error.BusinessRuleException("Hora fim deve ser maior que a hora início.");
        }
    }

    public void recalcularDadosCurso(Curso curso) {
        DadosCursoRecalculados dados = calcularDadosCurso(curso);

        aplicarDadosCurso(curso, dados);
        cursoRepository.atualizarDadosRecalculados(
                curso.getId(),
                dados.dataInicial(),
                dados.dataFinal(),
                dados.cargaHoraria(),
                dados.status());
    }

    public void sincronizarDadosCursoSeNecessario(Curso curso) {
        DadosCursoRecalculados dados = calcularDadosCurso(curso);
        if (dadosIguais(curso, dados)) {
            return;
        }

        aplicarDadosCurso(curso, dados);
        cursoRepository.atualizarDadosRecalculados(
                curso.getId(),
                dados.dataInicial(),
                dados.dataFinal(),
                dados.cargaHoraria(),
                dados.status());
    }

    private DadosCursoRecalculados calcularDadosCurso(Curso curso) {
        List<Aula> aulas = aulaRepository.findAllByCursoId(curso.getId());
        long totalMinutos = 0;
        LocalDateTime dataInicial = null;
        LocalDateTime dataFinal = null;

        for (Aula aula : aulas) {
            totalMinutos += aula.getDuracaoMinutos();
            LocalDateTime inicioAula = aula.getData().atTime(aula.getHoraInicio());
            LocalDateTime fimAula = aula.getData().atTime(aula.getHoraFim());

            if (dataInicial == null || inicioAula.isBefore(dataInicial)) {
                dataInicial = inicioAula;
            }
            if (dataFinal == null || fimAula.isAfter(dataFinal)) {
                dataFinal = fimAula;
            }
        }

        long hours = totalMinutos / 60;
        long minutes = totalMinutos % 60;
        String chCalculada = String.format("%02d:%02d:00", hours, minutes);

        StatusCursoEnum status = totalMinutos > 0
                ? StatusCursoEnum.PUBLICADO
                : StatusCursoEnum.EM_CONSTRUCAO;

        return new DadosCursoRecalculados(dataInicial, dataFinal, chCalculada, status);
    }

    private void aplicarDadosCurso(Curso curso, DadosCursoRecalculados dados) {
        curso.setCargaHoraria(dados.cargaHoraria());
        curso.setRealizacaoDataInicial(dados.dataInicial());
        curso.setRealizacaoDataFinal(dados.dataFinal());
        curso.setStatus(dados.status());
    }

    private boolean dadosIguais(Curso curso, DadosCursoRecalculados dados) {
        return Objects.equals(curso.getRealizacaoDataInicial(), dados.dataInicial())
                && Objects.equals(curso.getRealizacaoDataFinal(), dados.dataFinal())
                && Objects.equals(curso.getCargaHoraria(), dados.cargaHoraria())
                && curso.getStatus() == dados.status();
    }

    private record DadosCursoRecalculados(
            LocalDateTime dataInicial,
            LocalDateTime dataFinal,
            String cargaHoraria,
            StatusCursoEnum status) {
    }

    public Chamada adicionarChamada(Long aulaId, String tipoStr) {
        // Obter o enum a partir da string
        mppb.restapi.cursos.feature.frequencia.enums.TipoChamadaEnum tipo = mppb.restapi.cursos.feature.frequencia.enums.TipoChamadaEnum.valueOf(tipoStr.toUpperCase());
        
        Aula aula = findById(aulaId)
                .orElseThrow(() -> new NotFoundException("Aula não encontrada: " + aulaId));

        if (aula.getCurso().getStatus() != mppb.restapi.cursos.feature.curso.enums.StatusCursoEnum.PUBLICADO) {
            throw new mppb.restapi.common.core.error.BusinessRuleException("Não é possível gerar chamadas pois a carga horária de aulas está divergente ou o curso está em construção.");
        }

        // Validação movida do ChamadaService para manter simplicidade aqui
        boolean jaExiste = aula.getChamadas().stream().anyMatch(c -> c.getTipo() == tipo);
        if (jaExiste) {
            throw new mppb.restapi.common.core.error.BusinessRuleException("Já existe uma chamada de " + tipo.getDescricao() + " para esta aula.");
        }

        Chamada novaChamada = new Chamada();
        novaChamada.setAula(aula);
        novaChamada.setDataGeracao(LocalDateTime.now());
        novaChamada.setTipo(tipo);
        novaChamada.setAberta(true);
        // O token será gerado automaticamente pelo @PrePersist da entidade Chamada

        aula.getChamadas().add(novaChamada);
        aulaRepository.save(aula);
        frequenciaService.consolidarFrequenciasPorCurso(aula.getCurso().getId());

        return novaChamada;
    }

    public void removerChamada(Long cursoId, Long aulaId, Long chamadaId) {
        Chamada chamada = chamadaRepository.findById(chamadaId)
                .orElseThrow(() -> new NotFoundException("Chamada não encontrada: " + chamadaId));

        Aula aula = chamada.getAula();
        if (!aula.getId().equals(aulaId) || !aula.getCurso().getId().equals(cursoId)) {
            throw new mppb.restapi.common.core.error.BusinessRuleException("A chamada não pertence a esta aula/curso.");
        }

        frequenciaService.removerPorChamada(chamadaId);
        chamadaRepository.delete(chamada);
        chamadaRepository.flush();
        frequenciaService.consolidarFrequenciasPorCurso(cursoId);
    }
}
