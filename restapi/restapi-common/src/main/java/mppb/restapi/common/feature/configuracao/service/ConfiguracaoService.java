package mppb.restapi.common.feature.configuracao.service;

import lombok.RequiredArgsConstructor;
import mppb.restapi.common.feature.configuracao.repository.ConfiguracaoRepository;
import mppb.restapi.common.core.error.NotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço para buscar configurações do banco de dados, com cache.
 * As configurações são cacheadas para evitar leituras repetitivas no banco.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConfiguracaoService {

    private final ConfiguracaoRepository configuracaoRepository;

    /**
     * Busca o valor de uma propriedade de configuração.
     * O resultado é cacheado na cache "configuracoes".
     *
     * @param propriedade A chave da configuração (ex: 'global.instituicao.ip_email')
     * @return O valor (String) da configuração.
     * @throws NotFoundException se a propriedade não for encontrada.
     */
    @Cacheable(value = "configuracoes", key = "#propriedade")
    public String get(String propriedade) {
        return configuracaoRepository.findByPropriedade(propriedade)
                .orElseThrow(() -> new NotFoundException("Propriedade de configuração não encontrada no banco: " + propriedade))
                .getValor();
    }

    /**
     * Busca um valor de configuração como Integer.
     */
    public Integer getAsInteger(String propriedade) {
        try {
            return Integer.parseInt(get(propriedade));
        } catch (NumberFormatException e) {
            throw new RuntimeException("A propriedade de configuração '" + propriedade + "' não é um número inteiro válido.");
        }
    }
}
