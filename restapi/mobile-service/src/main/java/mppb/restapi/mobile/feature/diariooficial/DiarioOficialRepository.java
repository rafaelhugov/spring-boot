package mppb.restapi.mobile.feature.diariooficial;

import mppb.restapi.common.core.repository.BaseRepository;
import mppb.restapi.mobile.feature.diariooficial.entity.DiarioOficial;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DiarioOficialRepository  extends BaseRepository<DiarioOficial, Long> {

    /**
     * Recupera os 60 diários oficiais mais recentes.
     * Nota: Usei nativeQuery = true para manter a fidelidade ao SQL original
     * que acessa a View específica do banco de dados.
     */
    @Query(value = "SELECT id, dataPublicacao, numeroEdicao, id_documentoassinado " +
            "FROM diariooficial.diario_oficial_externo_vw " +
            "ORDER BY datapublicacao DESC LIMIT 60",
            nativeQuery = true)
    List<DiarioOficial> recuperarDiariosOficiaisRecentes();
}
