package mppb.restapi.common.core.service;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.error.NotFoundException;
import mppb.restapi.common.core.repository.BaseRepository;
import mppb.restapi.common.core.repository.SoftDeleteRepository;
import mppb.restapi.common.feature.autorizacao.entities.Usuario;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
public class AbstractService<I, ID> {

    protected final EntityManager entityManager;
    protected final BaseRepository<I, ID> repository;

    @Autowired
    private AuditorAware<Usuario> auditorAware;

    public AbstractService(EntityManager entityManager, BaseRepository<I, ID> repository){
        this.entityManager = entityManager;
        this.repository = repository;
    }

    public List<I> findAll() {
        return repository.findAll();
    }

    public Page<I> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<I> findAll(Specification specification, Pageable pageable) {
        return repository.findAll(specification, pageable);
    }

    public Optional<I> findById(ID id) {
        return repository.findById(id);
    }

    public I save(I entity) {
        return repository.save(entity);
    }

    public void deleteById(ID id) {
        if (!this.repository.existsById(id)) {
            throw new NotFoundException("Não existe entidade encontrado com id: " + id);
        }

        Usuario usuario = auditorAware.getCurrentAuditor()
                .orElseThrow(() -> new IllegalStateException("Não foi possível determinar o usuário logado para a operação de exclusão."));

        if(repository instanceof SoftDeleteRepository){
            SoftDeleteRepository softDeleteRepository = (SoftDeleteRepository) repository;
            softDeleteRepository.softDelete(id, usuario, LocalDateTime.now());
        } else {
            this.repository.deleteById(id);
        }
    }

    public boolean existsById(ID uuid) {
        return repository.existsById(uuid);
    }

}
