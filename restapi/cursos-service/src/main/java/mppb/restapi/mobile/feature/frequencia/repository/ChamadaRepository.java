package mppb.restapi.mobile.feature.frequencia.repository;

import mppb.restapi.common.core.repository.BaseRepository;
import mppb.restapi.mobile.feature.frequencia.entity.Chamada;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChamadaRepository extends BaseRepository<Chamada, Long> {

    // Busca uma chamada pelo seu token único (usado no registo público)
    Optional<Chamada> findByToken(String token);
}