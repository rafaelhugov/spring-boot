package mppb.restapi.common.feature.autorizacao.service;

import lombok.RequiredArgsConstructor;
import mppb.restapi.common.core.error.NotFoundException;
import mppb.restapi.common.feature.autorizacao.entities.Sistema;
import mppb.restapi.common.feature.autorizacao.repository.SistemaRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SistemaService {

    private final SistemaRepository sistemaRepository;

    /**
     * Busca a entidade Sistema pela sua sigla.
     * O resultado é cacheado para alta performance.
     */
    @Cacheable(value = "sistemas", key = "#sigla")
    public Sistema findBySigla(String sigla) {
        return sistemaRepository.findBySigla(sigla)
                .orElseThrow(() -> new NotFoundException("Sistema não encontrado com a sigla: " + sigla));
    }

    /**
     * Busca a URL de um sistema pela sua sigla.
     */
    @Cacheable(value = "sistemas_url", key = "#sigla")
    public String findUrlBySigla(String sigla) {
        String url = findBySigla(sigla).getUrl();
        if (url == null || url.isBlank()) {
            throw new NotFoundException("A URL para o sistema " + sigla + " não está configurada.");
        }
        return url;
    }
}
