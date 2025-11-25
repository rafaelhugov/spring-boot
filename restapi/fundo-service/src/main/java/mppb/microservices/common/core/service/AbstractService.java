package mppb.microservices.common.core.service;

import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public class AbstractService<I, ID> {

    private final EntityManager entityManager;
    private final JpaRepository<I, ID> repository;

    public AbstractService(EntityManager entityManager, JpaRepository<I, ID> repository){
        this.entityManager = entityManager;
        this.repository = repository;
    }

    public List<I> findAll() {
        return repository.findAll();
    }

    public Page<I> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Optional<I> findById(ID id) {
        return repository.findById(id);
    }

    public I save(I entity) {
        return repository.save(entity);
    }

    public void deleteById(ID id) {
        repository.deleteById(id);
    }

    public boolean existsById(ID uuid) {
        return repository.existsById(uuid);
    }

}
