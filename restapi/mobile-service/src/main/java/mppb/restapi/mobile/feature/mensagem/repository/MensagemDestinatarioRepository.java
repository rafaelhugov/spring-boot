package mppb.restapi.mobile.feature.mensagem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mppb.restapi.mobile.feature.mensagem.entity.MensagemDestinatario;
import mppb.restapi.mobile.feature.mensagem.enums.StatusDestinatario;

public interface MensagemDestinatarioRepository extends JpaRepository<MensagemDestinatario, Long> {

    Optional<MensagemDestinatario> findByMensagemIdAndDestinatarioMatricula(Long mensagemId, String matricula);

    List<MensagemDestinatario> findByDestinatarioMatriculaAndMensagemIdIn(String matricula, List<Long> mensagemIds);

    boolean existsByMensagemIdAndDestinatarioMatricula(Long mensagemId, String matricula);

    long countByMensagemId(Long mensagemId);

    long countByMensagemIdAndStatus(Long mensagemId, StatusDestinatario status);

    long countByMensagemIdAndDataCienciaIsNotNull(Long mensagemId);

    @Query("""
            select count(md)
            from MensagemDestinatario md
            join MensagemAviso ma on ma.id = md.mensagemId
            where md.destinatarioMatricula = :matricula
              and md.arquivadaDestinatario = false
              and ma.statusPublicacao = mppb.restapi.mobile.feature.mensagem.enums.StatusPublicacao.ENVIADA
              and (ma.dataExpiracao is null or ma.dataExpiracao > CURRENT_TIMESTAMP)
              and md.status <> mppb.restapi.mobile.feature.mensagem.enums.StatusDestinatario.LIDO
            """)
    long contarNaoLidasAtivas(@Param("matricula") String matricula);

    Page<MensagemDestinatario> findByMensagemIdAndStatus(
            Long mensagemId, StatusDestinatario status, Pageable pageable);

    Page<MensagemDestinatario> findByMensagemId(Long mensagemId, Pageable pageable);

    @Query("""
            select md
            from MensagemDestinatario md
            where md.mensagemId = :mensagemId
              and (:status is null or md.status = :status)
              and (:busca is null or lower(md.destinatarioNome) like :busca
                   or lower(md.destinatarioMatricula) like :busca
                   or lower(coalesce(md.destinatarioSetor, '')) like :busca)
            order by md.destinatarioNome asc, md.id asc
            """)
    Page<MensagemDestinatario> filtrarAcompanhamento(
            @Param("mensagemId") Long mensagemId,
            @Param("status") StatusDestinatario status,
            @Param("busca") String busca,
            Pageable pageable);

    @Query("""
            select md
            from MensagemDestinatario md
            join MensagemAviso ma on ma.id = md.mensagemId
            where md.destinatarioMatricula = :matricula
              and ma.statusPublicacao = mppb.restapi.mobile.feature.mensagem.enums.StatusPublicacao.ENVIADA
              and md.arquivadaDestinatario = :arquivada
              and (:somenteNaoLidas = false or md.status <> mppb.restapi.mobile.feature.mensagem.enums.StatusDestinatario.LIDO)
              and (:incluirExpiradas = true or ma.dataExpiracao is null or ma.dataExpiracao > CURRENT_TIMESTAMP)
            order by ma.dataEnvio desc, md.id desc
            """)
    Page<MensagemDestinatario> listarCaixaEntrada(
            @Param("matricula") String matricula,
            @Param("arquivada") boolean arquivada,
            @Param("somenteNaoLidas") boolean somenteNaoLidas,
            @Param("incluirExpiradas") boolean incluirExpiradas,
            Pageable pageable);
}

