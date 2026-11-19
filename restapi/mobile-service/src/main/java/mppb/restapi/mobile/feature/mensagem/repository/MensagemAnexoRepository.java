package mppb.restapi.mobile.feature.mensagem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mppb.restapi.mobile.feature.mensagem.entity.MensagemAnexo;

public interface MensagemAnexoRepository extends JpaRepository<MensagemAnexo, Long> {

    List<MensagemAnexo> findByMensagemIdOrderByIdAsc(Long mensagemId);

    List<MensagemAnexo> findByIdIn(List<Long> ids);

    Optional<MensagemAnexo> findByIdAndMensagemId(Long id, Long mensagemId);
}

