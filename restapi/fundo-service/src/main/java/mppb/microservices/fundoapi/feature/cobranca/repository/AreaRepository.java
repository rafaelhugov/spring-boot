package mppb.microservices.fundoapi.feature.cobranca.repository;

import mppb.microservices.fundoapi.feature.cobranca.dto.AreaDto;
import mppb.microservices.fundoapi.feature.cobranca.entity.Area;
import mppb.microservices.fundoapi.feature.cobranca.projection.AreaSelectProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AreaRepository extends JpaRepository<Area, Long> {
    Page<Area> findByNomeContainingIgnoreCase(String termo, Pageable pageable);

    @Query("SELECT new mppb.microservices.fundoapi.feature.cobranca.projection.AreaSelectProjection(a.id, a.nome) FROM Area a")
    List<AreaSelectProjection> findSelectAll();

    @Query("SELECT new mppb.microservices.fundoapi.feature.cobranca.dto.AreaDto(a.id, a.nome) FROM Area a")
    List<AreaDto> findSelectJpql();


    // Você pode adicionar métodos de consulta personalizados aqui, se necessário.
    // Por exemplo:
    // List<Area> findByNome(String nome);
    // Optional<Area> findByNomeIgnoreCase(String nome);
}
