package mppb.restapi.mobile.feature.mensagem.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mppb.restapi.mobile.feature.mensagem.entity.GrupoMensagemMembro;

public interface GrupoMensagemMembroRepository extends JpaRepository<GrupoMensagemMembro, Long> {

    List<GrupoMensagemMembro> findByGrupoId(Long grupoId);

    void deleteByGrupoId(Long grupoId);
}

