package mppb.restapi.mobile.feature.mensagem.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import mppb.restapi.mobile.feature.mensagem.entity.MensagemAviso;
import mppb.restapi.mobile.feature.mensagem.enums.StatusPublicacao;

public interface MensagemAvisoRepository extends JpaRepository<MensagemAviso, Long> {

    List<MensagemAviso> findByStatusPublicacaoAndDataAgendamentoLessThanEqual(
            StatusPublicacao statusPublicacao,
            LocalDateTime agora);

    Page<MensagemAviso> findByArquivadaRemetenteFalseOrderByDataRegistroDesc(Pageable pageable);

    Page<MensagemAviso> findByArquivadaRemetenteOrderByDataRegistroDesc(boolean arquivadaRemetente, Pageable pageable);

    Page<MensagemAviso> findByRemetenteMatriculaAndArquivadaRemetenteOrderByDataRegistroDesc(
            String remetenteMatricula,
            boolean arquivadaRemetente,
            Pageable pageable);
}

