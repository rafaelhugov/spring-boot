package mppb.restapi.mobile.feature.frequencia.service;

import jakarta.persistence.EntityManager;
import mppb.restapi.common.core.error.NotFoundException;
import mppb.restapi.common.core.service.AbstractService;
import mppb.restapi.mobile.feature.frequencia.dto.ChamadaPublicaDto;
import mppb.restapi.mobile.feature.frequencia.entity.Chamada;
import mppb.restapi.mobile.feature.frequencia.repository.ChamadaRepository;
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
                .build();
    }
}