package mppb.restapi.cursos.feature.frequencia.service;

import jakarta.persistence.EntityManager;
import mppb.restapi.common.core.error.NotFoundException;
import mppb.restapi.common.core.service.AbstractService;
import mppb.restapi.cursos.feature.frequencia.dto.ChamadaPublicaDto;
import mppb.restapi.cursos.feature.frequencia.entity.Chamada;
import mppb.restapi.cursos.feature.frequencia.repository.ChamadaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ChamadaService extends AbstractService<Chamada, Long> {

    private final ChamadaRepository chamadaRepository;

    public ChamadaService(EntityManager entityManager, ChamadaRepository chamadaRepository) {
        super(entityManager, chamadaRepository);
        this.chamadaRepository = chamadaRepository;
    }

    @Transactional(readOnly = true)
    public Chamada buscarPorToken(String token) {
        return chamadaRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Chamada não encontrada ou token inválido."));
    }

    @Transactional(readOnly = true)
    public java.util.List<Chamada> findAllByAulaId(Long aulaId) {
        return chamadaRepository.findAllByAulaId(aulaId);
    }

    @Transactional
    public Chamada abrirChamada(Long aulaId, mppb.restapi.cursos.feature.frequencia.enums.TipoChamadaEnum tipo) {
        // Encontra a aula
        mppb.restapi.cursos.feature.frequencia.entity.Aula aula = entityManager.find(mppb.restapi.cursos.feature.frequencia.entity.Aula.class, aulaId);
        if (aula == null) {
            throw new NotFoundException("Aula não encontrada.");
        }
        
        // Verifica se já existe uma chamada aberta ou já criada para este tipo nesta aula
        boolean jaExiste = chamadaRepository.findAllByAulaId(aulaId).stream()
            .anyMatch(c -> c.getTipo() == tipo);
        if (jaExiste) {
            throw new mppb.restapi.common.core.error.BusinessRuleException("Já existe uma chamada de " + tipo.getDescricao() + " para esta aula.");
        }

        Chamada chamada = new Chamada();
        chamada.setAula(aula);
        chamada.setTipo(tipo);
        chamada.setAberta(true);
        return chamadaRepository.save(chamada);
    }

    @Transactional
    public Chamada encerrarChamada(Long chamadaId) {
        Chamada chamada = chamadaRepository.findById(chamadaId)
            .orElseThrow(() -> new NotFoundException("Chamada não encontrada."));
        chamada.setAberta(false);
        return chamadaRepository.save(chamada);
    }

    @Transactional
    public Chamada obterChamadaAutomatica(Long aulaId) {
        mppb.restapi.cursos.feature.frequencia.entity.Aula aula = entityManager.find(mppb.restapi.cursos.feature.frequencia.entity.Aula.class, aulaId);
        if (aula == null) {
            throw new NotFoundException("Aula não encontrada.");
        }

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalTime now = java.time.LocalTime.now();

        if (!today.equals(aula.getData()) || now.isBefore(aula.getHoraInicio()) || now.isAfter(aula.getHoraFim())) {
            throw new mppb.restapi.common.core.error.BusinessRuleException("O registro de frequência só é permitido durante o horário da aula (" + 
                aula.getData().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " " + aula.getHoraInicio() + " às " + aula.getHoraFim() + ").");
        }

        mppb.restapi.cursos.feature.frequencia.enums.TipoChamadaEnum tipoTemp = mppb.restapi.cursos.feature.frequencia.enums.TipoChamadaEnum.INICIO;
        
        if (aula.getQuantidadeChamadas() != null && aula.getQuantidadeChamadas() > 1) {
            long totalDuration = java.time.Duration.between(aula.getHoraInicio(), aula.getHoraFim()).toMinutes();
            long elapsed = java.time.Duration.between(aula.getHoraInicio(), now).toMinutes();
            if (elapsed >= totalDuration / 2) {
                tipoTemp = mppb.restapi.cursos.feature.frequencia.enums.TipoChamadaEnum.FIM;
            }
        }

        final mppb.restapi.cursos.feature.frequencia.enums.TipoChamadaEnum tipo = tipoTemp;

        return chamadaRepository.findByAulaAndTipo(aula, tipo).orElseGet(() -> {
            Chamada nova = new Chamada();
            nova.setAula(aula);
            nova.setTipo(tipo);
            nova.setAberta(true);
            return chamadaRepository.save(nova);
        });
    }

    /**
     * Mapeia os dados essenciais da Aula para o DTO de visualização pública.
     */
    @Transactional(readOnly = true)
    public ChamadaPublicaDto getDadosChamadaPublica(Long aulaId) {
        mppb.restapi.cursos.feature.frequencia.entity.Aula aula = entityManager.find(mppb.restapi.cursos.feature.frequencia.entity.Aula.class, aulaId);
        if (aula == null) {
            throw new NotFoundException("Aula não encontrada.");
        }

        final var curso = aula.getCurso();

        return ChamadaPublicaDto.builder()
                .cursoTitulo(curso.getTitulo())
                .aulaDescricao(aula.getDescricao())
                .data(aula.getData())
                .horaInicio(aula.getHoraInicio())
                .horaFim(aula.getHoraFim())
                .tipoChamada("Registro de Frequência")
                .aberta(true) // O backend validará a hora no momento do envio do CPF
                .build();
    }
}