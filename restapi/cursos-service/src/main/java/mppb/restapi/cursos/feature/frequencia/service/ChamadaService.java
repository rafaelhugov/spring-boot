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

    /**
     * Mapeia os dados essenciais da Chamada para o DTO de visualização pública.
     * Este método é EAGER por natureza, garantindo o carregamento do Curso aninhado.
     */
    @Transactional(readOnly = true)
    public ChamadaPublicaDto getDadosChamadaPublica(String token) {
        Chamada chamada = buscarPorToken(token);

        // Acesso direto, forçando o carregamento lazy do Aula e Curso
        // (Nota: Isso funciona se as entidades forem configuradas para acesso lazy no Service)
        final var aula = chamada.getAula();
        final var curso = aula.getCurso();

        return ChamadaPublicaDto.builder()
                .cursoTitulo(curso.getTitulo())
                .aulaDescricao(aula.getDescricao())
                .data(aula.getData())
                .horaInicio(aula.getHoraInicio())
                .horaFim(aula.getHoraFim())
                .tipoChamada(chamada.getTipo().getDescricao())
                .aberta(chamada.isAberta())
                .build();
    }
}