package mppb.restapi.cursos.feature.frequencia.service;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.error.BusinessRuleException; // Import adicionado
import mppb.restapi.common.core.service.AbstractService;
import mppb.restapi.cursos.feature.curso.entity.Curso; // Import adicionado
import mppb.restapi.cursos.feature.curso.enums.StatusCursoEnum;
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

        if (inscricao.getSituacao() != SituacaoInscricaoEnum.INSCRITO &&
            inscricao.getSituacao() != SituacaoInscricaoEnum.APROVADO &&
            inscricao.getSituacao() != SituacaoInscricaoEnum.REPROVADO) {
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
        frequenciaRepository.flush();
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
    public List<Frequencia> findAllByAulaId(Long aulaId) {
        return frequenciaRepository.findAllByChamadaAulaId(aulaId);
    }

    @Transactional
    public void remover(Long cursoId, Long aulaId, Long frequenciaId) {
        Frequencia frequencia = frequenciaRepository.findById(frequenciaId)
            .orElseThrow(() -> new mppb.restapi.common.core.error.NotFoundException("Registro de frequência não encontrado."));

        if (!frequencia.getChamada().getAula().getId().equals(aulaId)
                || !frequencia.getChamada().getAula().getCurso().getId().equals(cursoId)) {
            throw new BusinessRuleException("O registro de frequência não pertence a esta aula/curso.");
        }

        frequenciaRepository.delete(frequencia);
        frequenciaRepository.flush();
        consolidarFrequenciasPorCurso(cursoId);
    }

    @Transactional
    public void removerPorAula(Long aulaId) {
        frequenciaRepository.deleteByAulaId(aulaId);
        frequenciaRepository.flush();
    }

    @Transactional
    public void removerPorChamada(Long chamadaId) {
        frequenciaRepository.deleteByChamadaId(chamadaId);
        frequenciaRepository.flush();
    }

    @Transactional
    public void consolidarFrequenciasPorCurso(Long cursoId) {
        List<Inscricao> inscricoes = inscricaoRepository.findAllByCursoId(cursoId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        for (Inscricao inscricao : inscricoes) {
            consolidarFrequencias(inscricao.getId());
        }
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

        if (aulas.isEmpty()) {
            inscricao.setFrequencia(0);
            if (inscricao.getSituacao() == SituacaoInscricaoEnum.APROVADO || inscricao.getSituacao() == SituacaoInscricaoEnum.REPROVADO) {
                inscricao.setSituacao(SituacaoInscricaoEnum.INSCRITO);
            }
            inscricaoRepository.save(inscricao);
            return;
        }

        int aulasComPresencaIntegral = 0;
        int aulasJaRealizadas = 0;

        for (mppb.restapi.cursos.feature.frequencia.entity.Aula aula : aulas) {
            List<Chamada> chamadasDaAula = chamadaService.findAllByAulaId(aula.getId());
            if (chamadasDaAula.isEmpty()) {
                continue; // Aula sem chamada ainda
            }
            aulasJaRealizadas++;

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
        
        int minimoAprovacao = curso.getPercentualChAprovacao() != null ? curso.getPercentualChAprovacao() : 75;
        int aulasRestantes = aulas.size() - aulasJaRealizadas;
        int frequenciaMaximaPossivel = (int) (((double) (aulasComPresencaIntegral + aulasRestantes) / aulas.size()) * 100);
        boolean cursoFinalizado = isCursoFinalizado(curso);

        if (percentual >= minimoAprovacao) {
            inscricao.setSituacao(SituacaoInscricaoEnum.APROVADO);
        } else if (cursoFinalizado && frequenciaMaximaPossivel < minimoAprovacao) {
            inscricao.setSituacao(SituacaoInscricaoEnum.REPROVADO);
        } else if (inscricao.getSituacao() == SituacaoInscricaoEnum.APROVADO || inscricao.getSituacao() == SituacaoInscricaoEnum.REPROVADO) {
            inscricao.setSituacao(SituacaoInscricaoEnum.INSCRITO);
        }
        
        inscricaoRepository.save(inscricao);
    }

    private boolean isCursoFinalizado(Curso curso) {
        return curso.getStatus() == StatusCursoEnum.FINALIZADO
                || (curso.getRealizacaoDataFinal() != null && curso.getRealizacaoDataFinal().isBefore(LocalDateTime.now()));
    }

    @Transactional
    public void registrarPresencasManuais(Long cursoId, Long inscricaoId, mppb.restapi.cursos.feature.frequencia.dto.AtualizarFrequenciasManuaisDto dto) {
        Inscricao inscricao = inscricaoRepository.findById(inscricaoId)
            .orElseThrow(() -> new mppb.restapi.common.core.error.NotFoundException("Inscrição não encontrada."));

        List<mppb.restapi.cursos.feature.frequencia.entity.Aula> aulas = entityManager.createQuery(
            "SELECT a FROM Aula a WHERE a.curso.id = :cursoId", mppb.restapi.cursos.feature.frequencia.entity.Aula.class)
            .setParameter("cursoId", cursoId)
            .getResultList();

        for (mppb.restapi.cursos.feature.frequencia.entity.Aula aula : aulas) {
            Optional<mppb.restapi.cursos.feature.frequencia.dto.RegistroPresencaManualDto> registroOpt = dto.getPresencas().stream()
                .filter(p -> p.getAulaId().equals(aula.getId()))
                .findFirst();

            List<Chamada> chamadas = chamadaService.findAllByAulaId(aula.getId());

            if (registroOpt.isPresent()) {
                List<String> horarios = registroOpt.get().getHorarios();
                int quantidadeExigida = aula.getQuantidadeChamadas() != null ? aula.getQuantidadeChamadas() : 1;
                
                for (int i = 0; i < quantidadeExigida; i++) {
                    mppb.restapi.cursos.feature.frequencia.enums.TipoChamadaEnum tipo = (i == 0) ? mppb.restapi.cursos.feature.frequencia.enums.TipoChamadaEnum.INICIO : mppb.restapi.cursos.feature.frequencia.enums.TipoChamadaEnum.FIM;
                    String horarioStr = (horarios != null && horarios.size() > i) ? horarios.get(i) : null;

                    Chamada chamada = chamadas.stream()
                        .filter(c -> c.getTipo() == tipo)
                        .findFirst()
                        .orElseGet(() -> {
                            Chamada nova = new Chamada();
                            nova.setAula(aula);
                            nova.setTipo(tipo);
                            nova.setAberta(false);
                            return chamadaService.save(nova);
                        });

                    if (horarioStr != null && !horarioStr.isBlank()) {
                        java.time.LocalTime horario = java.time.LocalTime.parse(horarioStr);
                        if (horario.isBefore(aula.getHoraInicio()) || horario.isAfter(aula.getHoraFim())) {
                            throw new BusinessRuleException(String.format("O horário %s está fora do período da aula (%s às %s).", horarioStr, aula.getHoraInicio(), aula.getHoraFim()));
                        }
                        
                        Frequencia frequencia = frequenciaRepository.findByChamadaAndInscricao(chamada, inscricao)
                            .orElse(new Frequencia());
                        
                        frequencia.setChamada(chamada);
                        frequencia.setInscricao(inscricao);
                        frequencia.setDataHoraRegistro(LocalDateTime.of(aula.getData(), horario));
                        frequencia.setManual(true);
                        
                        frequenciaRepository.save(frequencia);
                    } else {
                        frequenciaRepository.findByChamadaAndInscricao(chamada, inscricao)
                            .ifPresent(frequenciaRepository::delete);
                    }
                }
            } else {
                for (Chamada chamada : chamadas) {
                    frequenciaRepository.findByChamadaAndInscricao(chamada, inscricao)
                        .ifPresent(frequenciaRepository::delete);
                }
            }
        }
        
        frequenciaRepository.flush();
        consolidarFrequencias(inscricaoId);
    }

    @Transactional(readOnly = true)
    public mppb.restapi.cursos.feature.frequencia.dto.AtualizarFrequenciasManuaisDto buscarPresencasPorInscricao(Long inscricaoId) {
        List<Frequencia> frequencias = frequenciaRepository.findAllByInscricaoId(inscricaoId);
        java.util.Map<Long, List<String>> mapAulas = new java.util.HashMap<>();
        
        for (Frequencia freq : frequencias) {
            Long aulaId = freq.getChamada().getAula().getId();
            mapAulas.putIfAbsent(aulaId, new java.util.ArrayList<>());
            int index = freq.getChamada().getTipo() == mppb.restapi.cursos.feature.frequencia.enums.TipoChamadaEnum.INICIO ? 0 : 1;
            List<String> list = mapAulas.get(aulaId);
            while (list.size() <= index) {
                list.add(null);
            }
            list.set(index, freq.getDataHoraRegistro().toLocalTime().toString());
        }
        
        List<mppb.restapi.cursos.feature.frequencia.dto.RegistroPresencaManualDto> presencas = new java.util.ArrayList<>();
        for (java.util.Map.Entry<Long, List<String>> entry : mapAulas.entrySet()) {
            mppb.restapi.cursos.feature.frequencia.dto.RegistroPresencaManualDto dto = new mppb.restapi.cursos.feature.frequencia.dto.RegistroPresencaManualDto();
            dto.setAulaId(entry.getKey());
            dto.setHorarios(entry.getValue());
            presencas.add(dto);
        }
        
        mppb.restapi.cursos.feature.frequencia.dto.AtualizarFrequenciasManuaisDto dto = new mppb.restapi.cursos.feature.frequencia.dto.AtualizarFrequenciasManuaisDto();
        dto.setPresencas(presencas);
        return dto;
    }
}
