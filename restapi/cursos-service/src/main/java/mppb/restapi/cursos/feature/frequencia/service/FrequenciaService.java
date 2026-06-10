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
import mppb.restapi.cursos.feature.frequencia.converter.FrequenciaConverter;
import mppb.restapi.cursos.feature.frequencia.dto.FrequenciaDto;
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
     * Registra a presença de um participante usando o ID da aula e CPF.
     */
    public FrequenciaRegistroResponseDto registrarPresenca(Long aulaId, String cpf) {
        // 1. Obtém ou cria a chamada baseada na hora atual e configurações da aula
        Chamada chamada = chamadaService.obterChamadaAutomatica(aulaId);

        if (!chamada.isAberta()) {
            throw new BusinessRuleException("Esta chamada já foi encerrada.");
        }

        // 3. Identificar o Participante
        Participante participante = participanteService.identificarParticipantePorCpf(cpf);

        // 4. Validar a Inscrição
        Curso curso = chamada.getAula().getCurso();
        Optional<Inscricao> inscricaoOpt = inscricaoRepository.findByCursoAndParticipante(curso, participante);

        if (inscricaoOpt.isEmpty()) {
            throw new BusinessRuleException("Você não está inscrito neste curso.");
        }

        Inscricao inscricao = inscricaoOpt.get();

        if (inscricao.getSituacao() != SituacaoInscricaoEnum.INSCRITO) {
            throw new BusinessRuleException("A sua inscrição não está ativa.");
        }

        if (frequenciaRepository.existsByChamadaAndInscricao(chamada, inscricao)) {
            throw new BusinessRuleException("Você já registrou presença nesta chamada.");
        }

        // 6. Registra a Frequência
        Frequencia frequencia = new Frequencia();
        frequencia.setChamada(chamada);
        frequencia.setInscricao(inscricao);
        frequencia.setDataHoraRegistro(LocalDateTime.now());

        Frequencia frequenciaSalva = frequenciaRepository.save(frequencia);
        log.info("Presença registrada via QR Estático: Participante {} na Chamada {} (Aula {})", participante.getId(), chamada.getId(), chamada.getAula().getId());

        // Consolidar frequência (75% / regras do turno)
        consolidarFrequencias(inscricao.getId());

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

    @Transactional
    public void consolidarFrequencias(Long inscricaoId) {
        Inscricao inscricao = inscricaoRepository.findById(inscricaoId)
            .orElseThrow(() -> new mppb.restapi.common.core.error.NotFoundException("Inscrição não encontrada."));

        Curso curso = inscricao.getCurso();
        // Buscar aulas (podemos injetar AulaRepository ou buscar através de relacionamento se for EAGER/Fetch)
        // Como não temos a injeção do AulaRepository aqui, faremos uma query via EntityManager
        List<mppb.restapi.cursos.feature.frequencia.entity.Aula> aulas = entityManager.createQuery(
            "SELECT a FROM Aula a WHERE a.curso.id = :cursoId", mppb.restapi.cursos.feature.frequencia.entity.Aula.class)
            .setParameter("cursoId", curso.getId())
            .getResultList();

        if (aulas.isEmpty()) return;

        int aulasComPresencaIntegral = 0;

        for (mppb.restapi.cursos.feature.frequencia.entity.Aula aula : aulas) {
            List<Chamada> chamadasDaAula = chamadaService.findAllByAulaId(aula.getId());
            if (chamadasDaAula.isEmpty()) {
                continue; // Aula sem chamada ainda
            }

            int chamadasExigidas = aula.getQuantidadeChamadas() != null ? aula.getQuantidadeChamadas() : 1;

            int registros = 0;
            for (Chamada chamada : chamadasDaAula) {
                if (frequenciaRepository.existsByChamadaAndInscricao(chamada, inscricao)) {
                    registros++;
                }
            }

            // O participante só ganha a presença da aula se responder a todas as chamadas exigidas
            if (registros >= chamadasExigidas) {
                aulasComPresencaIntegral++;
            }
        }

        int percentual = (int) (((double) aulasComPresencaIntegral / aulas.size()) * 100);
        inscricao.setFrequencia(percentual);
        
        // Regra de aprovação/reprovação pode ser aplicada aqui (ex: < 75%) caso o curso tenha encerrado,
        // mas por hora mantemos a gravação do percentual dinamicamente.
        inscricaoRepository.save(inscricao);
    }
}