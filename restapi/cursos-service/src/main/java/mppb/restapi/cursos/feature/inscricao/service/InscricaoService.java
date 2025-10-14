package mppb.restapi.cursos.feature.inscricao.service;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.error.BusinessRuleException;
import mppb.restapi.common.core.error.NotFoundException;
import mppb.restapi.common.core.service.AbstractService;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.curso.repository.CursoRepository;
import mppb.restapi.cursos.feature.inscricao.converter.InscricaoConverter;
import mppb.restapi.cursos.feature.inscricao.dto.InscricaoDto;
import mppb.restapi.cursos.feature.inscricao.entity.Inscricao;
import mppb.restapi.cursos.feature.inscricao.enums.SituacaoInscricaoEnum;
import mppb.restapi.cursos.feature.inscricao.repository.InscricaoRepository;
import mppb.restapi.cursos.feature.participante.dto.CriarInscricaoDto;
import mppb.restapi.cursos.feature.participante.dto.ParticipanteDto;
import mppb.restapi.cursos.feature.participante.entity.Participante;
import mppb.restapi.cursos.feature.participante.service.ParticipanteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@Transactional
public class InscricaoService extends AbstractService<Inscricao, Long> {

    private final InscricaoRepository inscricaoRepository;
    private final CursoRepository cursoRepository;
    private final ParticipanteService participanteService;
    private final InscricaoConverter inscricaoConverter;

    public InscricaoService(EntityManager entityManager, InscricaoRepository inscricaoRepository, CursoRepository cursoRepository, ParticipanteService participanteService, InscricaoConverter inscricaoConverter) {
        super(entityManager, inscricaoRepository);
        this.inscricaoRepository = inscricaoRepository;
        this.cursoRepository = cursoRepository;
        this.participanteService = participanteService;
        this.inscricaoConverter = inscricaoConverter;
    }

    /**
     * Busca uma página de inscrições e já a converte para uma página de DTOs,
     * garantindo que os dados do participante sejam enriquecidos.
     */
    @Transactional(readOnly = true)
    public Page<Inscricao> findAllByCursoId(Long cursoId, Pageable pageable) {
        if (!cursoRepository.existsById(cursoId)) {
            throw new NotFoundException("Curso não encontrado: " + cursoId);
        }
        return inscricaoRepository.findAllByCursoId(cursoId, pageable);
    }

    /**
     * Converte uma entidade Inscricao para um DTO enriquecido.
     */
    @Transactional(readOnly = true)
    public InscricaoDto toDto(Inscricao inscricao) {
        InscricaoDto dto = inscricaoConverter.toDto(inscricao);
        // Enriquecimento dos dados do participante
        ParticipanteDto enrichedParticipanteDto = participanteService.toEnrichedDto(inscricao.getParticipante());
        dto.setParticipante(enrichedParticipanteDto);
        return dto;
    }

    public InscricaoDto inscrever(Long cursoId, CriarInscricaoDto dto) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new NotFoundException("Curso não encontrado: " + cursoId));

        Participante participante = participanteService.findOrCreateParticipante(dto);

        if (inscricaoRepository.existsByCursoAndParticipante(curso, participante)) {
            throw new BusinessRuleException("Este participante já está inscrito neste curso.");
        }

        Inscricao novaInscricao = new Inscricao();
        novaInscricao.setCurso(curso);
        novaInscricao.setParticipante(participante);
        novaInscricao.setDataInscricao(LocalDateTime.now());
        novaInscricao.setSituacao(SituacaoInscricaoEnum.INSCRITO);

        Inscricao inscricaoSalva = inscricaoRepository.save(novaInscricao);
        return toDto(inscricaoSalva);
    }


    /**
     * Atualiza a situação de uma inscrição.
     */
    public InscricaoDto atualizarSituacao(Long cursoId, Long inscricaoId, String novaSituacaoCodigoStr) {
        Inscricao inscricao = inscricaoRepository.findByIdAndCursoId(inscricaoId, cursoId)
                .orElseThrow(() -> new NotFoundException("Inscrição não encontrada para este curso."));

        try {
            // CORREÇÃO: Converte a String de um caractere para Character e usa o método de busca por código.
            Character codigo = novaSituacaoCodigoStr.charAt(0);
            SituacaoInscricaoEnum novaSituacao = SituacaoInscricaoEnum.fromCodigo(codigo);
            inscricao.setSituacao(novaSituacao);
        } catch (Exception e) { // Captura IndexOutOfBoundsException ou IllegalArgumentException
            throw new BusinessRuleException("Código de situação inválido: " + novaSituacaoCodigoStr);
        }

        return toDto(inscricao);
    }

    public void remover(Long cursoId, Long inscricaoId) {
        if (!inscricaoRepository.existsByIdAndCursoId(inscricaoId, cursoId)) {
            throw new NotFoundException("Inscrição não encontrada para este curso.");
        }
        inscricaoRepository.deleteById(inscricaoId);
    }
}

