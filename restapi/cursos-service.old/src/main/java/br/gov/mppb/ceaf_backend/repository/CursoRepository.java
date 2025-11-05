package br.gov.mppb.ceaf_backend.repository;

import br.gov.mppb.ceaf_backend.domain.Curso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CursoRepository extends JpaRepository<Curso, Long> {
    Optional<Curso> findByHashInscricao(UUID hash);
}
