package br.gov.mppb.ncap.feature.entidade;

import br.gov.mppb.ncap.feature.entidade.dto.EntidadeDto;
import jakarta.persistence.EntityManager;
import mppb.restapi.common.core.service.AbstractService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EntidadeService extends AbstractService<Entidade, Long> {

    private final EntidadeRepository repository;

    public EntidadeService(EntityManager entityManager, EntidadeRepository repository) {
        super(entityManager, repository);
        this.repository = repository;
    }

    public Page<Entidade> findAll(String search, Pageable pageable) {
        if (search != null && !search.trim().isEmpty()) {
            return repository.findByNomecnmpContainingIgnoreCaseOrMunicipioContainingIgnoreCase(search, search, pageable);
        }
        return repository.findAll(pageable);
    }

    public Entidade update(Long id, EntidadeDto dto) {
        Entidade entity = repository.findById(id)
                .orElseThrow(() -> new mppb.restapi.common.core.error.NotFoundException("Entidade não encontrada."));
        entity.setDescricaomppb(dto.getDescricaomppb());
        return repository.save(entity);
    }
}
