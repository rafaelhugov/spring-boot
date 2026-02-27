package mppb.restapi.common.feature.configuracao.repository;

import mppb.restapi.common.feature.configuracao.entity.Configuracao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfiguracaoRepository extends JpaRepository<Configuracao, Long> {

    /**
     * Busca uma propriedade de configuração pelo seu nome (chave).
     * @param propriedade O nome da chave (ex: 'global.instituicao.ip_email')
     * @return Um Optional contendo a entidade de configuração.
     */
    Optional<Configuracao> findByPropriedade(String propriedade);
}
