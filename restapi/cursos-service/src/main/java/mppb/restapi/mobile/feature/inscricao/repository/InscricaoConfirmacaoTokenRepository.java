package mppb.restapi.mobile.feature.inscricao.repository;

import mppb.restapi.common.core.repository.BaseRepository;
import mppb.restapi.mobile.feature.inscricao.entity.InscricaoConfirmacaoToken;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InscricaoConfirmacaoTokenRepository extends BaseRepository<InscricaoConfirmacaoToken, Long> {

    Optional<InscricaoConfirmacaoToken> findByToken(String token);

    Optional<InscricaoConfirmacaoToken> findByInscricaoId(Long inscricaoId);
}
