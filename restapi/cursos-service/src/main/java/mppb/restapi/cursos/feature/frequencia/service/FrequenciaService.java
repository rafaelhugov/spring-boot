package mppb.restapi.cursos.feature.frequencia.service;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.error.BusinessRuleException; // Import adicionado
import mppb.restapi.common.core.service.AbstractService;
import mppb.restapi.cursos.feature.curso.entity.Curso; // Import adicionado
import mppb.restapi.cursos.feature.frequencia.dto.FrequenciaRegistroResponseDto;
import mppb.restapi.cursos.feature.frequencia.entity.Chamada;
import mppb.restapi.cursos.feature.frequencia.entity.Frequencia;
import mppb.restapi.cursos.feature.frequencia.repository.FrequenciaRepository;
import mppb.restapi.cursos.feature.inscricao.entity.Inscricao;
import mppb.restapi.cursos.feature.inscricao.enums.SituacaoInscricaoEnum; // Import adicionado
import mppb.restapi.cursos.feature.inscricao.repository.InscricaoRepository;
import mppb.restapi.cursos.feature.participante.dto.ParticipanteDto;
import mppb.restapi.cursos.feature.participante.entity.Participante;
import mppb.restapi.cursos.feature.participante.service.ParticipanteService; // Import adicionado
import mppb.restapi.cursos.feature.frequencia.converter.FrequenciaConverter; // Import adicionado
import mppb.restapi.cursos.feature.frequencia.dto.FrequenciaDto; // Import adicionado

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@Transactional
public class FrequenciaService extends AbstractService<Frequencia, Long> { // Tipo alterado para Long

    private final FrequenciaRepository frequenciaRepository;
    private final ChamadaService chamadaService;
    private final InscricaoRepository inscricaoRepository;
    private final ParticipanteService participanteService;
    private final FrequenciaConverter frequenciaConverter;

    public FrequenciaService(EntityManager entityManager,
                             FrequenciaRepository frequenciaRepository,
                             ChamadaService chamadaService,
                             InscricaoRepository inscricaoRepository,
                             ParticipanteService participanteService,
                             FrequenciaConverter frequenciaConverter) {
        super(entityManager, frequenciaRepository);
        this.frequenciaRepository = frequenciaRepository;
        this.chamadaService = chamadaService;
        this.inscricaoRepository = inscricaoRepository;
        this.participanteService = participanteService;
        this.frequenciaConverter = frequenciaConverter;
    }

    /**
     * Registra a presença de um participante usando o token da chamada e o CPF.
     * @param token Token único gerado pela Chamada.
     * @param cpf O CPF do participante (recebido do DTO).
     * @return A entidade Frequencia registrada.
     */
    public FrequenciaRegistroResponseDto registrarPresenca(String token, String cpf) { // Assinatura simplificada para receber CPF
        // 1. Encontra a Chamada pelo Token
        Chamada chamada = chamadaService.buscarPorToken(token);

        // 2. Validação extra: A aula está ativa?
        //if (!chamada.getAula().isAtiva()) {
        //    throw new BusinessRuleException("Esta aula não está ativa para registro de presença.");
       // }

        // 3. Identificar o Participante (agora delegado ao serviço especialista)
        Participante participante = participanteService.identificarParticipantePorCpf(cpf);

        // 4. Validar a Inscrição (Está inscrito neste curso?)
        Curso curso = chamada.getAula().getCurso();
        Optional<Inscricao> inscricaoOpt = inscricaoRepository.findByCursoAndParticipante(curso, participante);

        if (inscricaoOpt.isEmpty()) {
            throw new BusinessRuleException("Você não está inscrito neste curso. Por favor, procure a equipe de apoio.");
        }

        Inscricao inscricao = inscricaoOpt.get();

        // Regra: Apenas inscritos confirmados podem registrar presença
        if (inscricao.getSituacao() != SituacaoInscricaoEnum.INSCRITO) {
            throw new BusinessRuleException("A sua inscrição não está ativa (Situação: " + inscricao.getSituacao().getDescricao() + ").");
        }

        // 5. Verificar Duplicidade (Já registrou presença nesta chamada?)
        if (frequenciaRepository.existsByChamadaAndInscricao(chamada, inscricao)) {
            throw new BusinessRuleException("Você já registrou presença nesta chamada.");
        }

        // 6. Registra a Frequência
        Frequencia frequencia = new Frequencia();
        frequencia.setChamada(chamada);
        frequencia.setInscricao(inscricao);
        frequencia.setDataHoraRegistro(LocalDateTime.now());

        Frequencia frequenciaSalva = frequenciaRepository.save(frequencia);
        log.info("Presença registrada: Participante {} na Chamada {} (Aula {})", participante.getId(), chamada.getId(), chamada.getAula().getId());

        // Retorna o DTO simplificado para a tela pública
        ParticipanteDto enrichedParticipante = participanteService.toEnrichedDto(participante);

        return FrequenciaRegistroResponseDto.builder()
                .nomeParticipante(enrichedParticipante.getNome())
                .dataHoraRegistro(frequenciaSalva.getDataHoraRegistro())
                .build();
    }

    /**
     * Converte a entidade Frequencia para DTO, enriquecendo com dados do Participante.
     */
    @Transactional(readOnly = true)
    public FrequenciaDto toDto(Frequencia frequencia) {
        FrequenciaDto dto = frequenciaConverter.toDto(frequencia);

        // Enriquecer o Participante antes de anexar ao DTO de Frequencia
        ParticipanteDto enrichedParticipanteDto = participanteService.toEnrichedDto(frequencia.getInscricao().getParticipante());
        dto.getInscricao().setParticipante(enrichedParticipanteDto);

        return dto;
    }

    @Transactional(readOnly = true)
    public List<Frequencia> findAllByAulaId(Integer aulaId) {
        return frequenciaRepository.findAllByChamadaAulaId(aulaId);
    }
}