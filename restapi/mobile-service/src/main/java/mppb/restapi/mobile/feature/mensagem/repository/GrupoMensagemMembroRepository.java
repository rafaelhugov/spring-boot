package mppb.restapi.mobile.feature.mensagem.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mppb.restapi.mobile.feature.mensagem.entity.GrupoMensagemMembro;

public interface GrupoMensagemMembroRepository extends JpaRepository<GrupoMensagemMembro, Long> {

    List<GrupoMensagemMembro> findByGrupoId(Long grupoId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from GrupoMensagemMembro m where m.grupoId = :grupoId")
    void deleteByGrupoId(@Param("grupoId") Long grupoId);
}

