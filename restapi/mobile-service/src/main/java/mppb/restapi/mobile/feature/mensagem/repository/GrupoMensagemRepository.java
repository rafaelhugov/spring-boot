package mppb.restapi.mobile.feature.mensagem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mppb.restapi.mobile.feature.mensagem.entity.GrupoMensagem;

public interface GrupoMensagemRepository extends JpaRepository<GrupoMensagem, Long> {

    List<GrupoMensagem> findByAtivoTrueOrderByNomeAsc();

    Optional<GrupoMensagem> findByIdAndAtivoTrue(Long id);
}

