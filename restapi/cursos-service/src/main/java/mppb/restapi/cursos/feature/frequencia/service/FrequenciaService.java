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

    private final FrequenciaOtpTokenRepository frequenciaOtpTokenRepository;
    private final mppb.restapi.cursos.feature.common.service.EmailService emailService;

    public FrequenciaService(EntityManager entityManager,
                             FrequenciaRepository frequenciaRepository,
                             ChamadaService chamadaService,
                             InscricaoRepository inscricaoRepository,
                             ParticipanteService participanteService,
                             FrequenciaConverter frequenciaConverter,
                             FrequenciaOtpTokenRepository frequenciaOtpTokenRepository,
                             mppb.restapi.cursos.feature.common.service.EmailService emailService) {
        super(entityManager, frequenciaRepository);
        this.frequenciaRepository = frequenciaRepository;
        this.chamadaService = chamadaService;
        this.inscricaoRepository = inscricaoRepository;
        this.participanteService = participanteService;
        this.frequenciaConverter = frequenciaConverter;
        this.frequenciaOtpTokenRepository = frequenciaOtpTokenRepository;
        this.emailService = emailService;
    }

    /**
     * Solicita o envio de um código OTP para o e-mail do participante.
     */
    public mppb.restapi.cursos.feature.frequencia.dto.SolicitacaoOtpResponseDto solicitarCodigoOtp(String token, String cpf) {
        Chamada chamada = chamadaService.buscarPorToken(token);

        if (!chamada.isAberta()) {
            throw new BusinessRuleException("Esta chamada já foi encerrada.");
        }

        Participante participante = participanteService.identificarParticipantePorCpf(cpf);
        Curso curso = chamada.getAula().getCurso();
        Optional<Inscricao> inscricaoOpt = inscricaoRepository.findByCursoAndParticipante(curso, participante);

        if (inscricaoOpt.isEmpty()) {
            throw new BusinessRuleException("Você não está inscrito neste curso. Por favor, procure a equipe de apoio.");
        }

        Inscricao inscricao = inscricaoOpt.get();

        if (inscricao.getSituacao() != SituacaoInscricaoEnum.INSCRITO) {
            throw new BusinessRuleException("A sua inscrição não está ativa (Situação: " + inscricao.getSituacao().getDescricao() + ").");
        }

        if (frequenciaRepository.existsByChamadaAndInscricao(chamada, inscricao)) {
            throw new BusinessRuleException("Você já registrou presença nesta chamada.");
        }

        // Gera código de 6 dígitos
        String codigoOtp = String.format("%06d", new java.util.Random().nextInt(999999));

        // Salva o token (validade de 5 minutos)
        mppb.restapi.cursos.feature.frequencia.entity.FrequenciaOtpToken otpToken = new mppb.restapi.cursos.feature.frequencia.entity.FrequenciaOtpToken(chamada, inscricao, codigoOtp, 5);
        frequenciaOtpTokenRepository.save(otpToken);

        // Envia E-mail
        String email = participante.getEmail();
        emailService.enviarEmailOtpFrequencia(email, participante.getNome(), codigoOtp, curso.getTitulo());

        String emailMascarado = mascararEmail(email);
        return new mppb.restapi.cursos.feature.frequencia.dto.SolicitacaoOtpResponseDto("E-mail enviado para " + emailMascarado);
    }

    private String mascararEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@");
        String name = parts[0];
        String domain = parts[1];
        if (name.length() <= 2) {
            return name.charAt(0) + "***@" + domain;
        }
        return name.substring(0, 2) + "***@" + domain;
    }

    /**
     * Registra a presença de um participante usando o token da chamada, CPF e o código OTP recebido por e-mail.
     */
    public FrequenciaRegistroResponseDto registrarPresenca(String token, String cpf, String codigoOtp) {
        // 1. Encontra a Chamada pelo Token
        Chamada chamada = chamadaService.buscarPorToken(token);

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

        // Validar OTP
        mppb.restapi.cursos.feature.frequencia.entity.FrequenciaOtpToken otpToken = frequenciaOtpTokenRepository
                .findFirstByChamadaAndInscricaoOrderByDataCriacaoDesc(chamada, inscricao)
                .orElseThrow(() -> new BusinessRuleException("Nenhum código solicitado para este participante. Por favor, solicite o envio do código."));

        if (otpToken.getDataExpiracao().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("O código expirou. Por favor, solicite um novo código.");
        }

        if (!otpToken.getCodigo().equals(codigoOtp)) {
            throw new BusinessRuleException("Código incorreto. Verifique o código enviado no seu e-mail.");
        }

        // Deleta o token após uso para não ser reusado
        frequenciaOtpTokenRepository.delete(otpToken);

        // 6. Registra a Frequência
        Frequencia frequencia = new Frequencia();
        frequencia.setChamada(chamada);
        frequencia.setInscricao(inscricao);
        frequencia.setDataHoraRegistro(LocalDateTime.now());

        Frequencia frequenciaSalva = frequenciaRepository.save(frequencia);
        log.info("Presença registrada via OTP: Participante {} na Chamada {} (Aula {})", participante.getId(), chamada.getId(), chamada.getAula().getId());

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

            // A regra diz: perder UMA das chamadas perde o turno inteiro
            boolean temFalta = false;
            for (Chamada chamada : chamadasDaAula) {
                boolean temRegistro = frequenciaRepository.existsByChamadaAndInscricao(chamada, inscricao);
                if (!temRegistro) {
                    temFalta = true;
                    break;
                }
            }

            if (!temFalta) {
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