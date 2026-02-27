package mppb.restapi.mobile.feature.ministrante.repository;

import mppb.restapi.common.core.repository.BaseRepository;
import mppb.restapi.mobile.feature.ministrante.entity.Ministrante;
import mppb.restapi.mobile.feature.ministrante.enums.TipoMinistranteEnum;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MinistranteRepository extends BaseRepository<Ministrante, Long> {

    /**
     * Busca um ministrante pelo seu ID de servidor único.
     * Utilizado para verificar se um ministrante interno já foi "importado" para o sistema de cursos.
     *
     * @param idServidor O ID do servidor vindo da base de RH.
     * @return Um Optional contendo o Ministrante, se encontrado.
     */
    Optional<Ministrante> findByIdServidor(Integer idServidor);

    /**
     * Busca um ministrante pelo seu CPF único.
     * Utilizado para verificar se um ministrante externo já existe antes de criar um novo registo.
     *
     * @param cpf O CPF do ministrante externo.
     * @return Um Optional contendo o Ministrante, se encontrado.
     */
    Optional<Ministrante> findByCpf(String cpf);


    /**
     * Busca por ministrantes EXTERNOS pelo nome, ignorando maiúsculas/minúsculas.
     * Usado na funcionalidade de busca unificada de ministrantes.
     *
     * @param tipo O tipo de ministrante (deve ser EXTERNO).
     * @param nome O termo de busca para o nome.
     * @return Uma lista de ministrantes externos encontrados.
     */
    List<Ministrante> findAllByTipoAndNomeContainingIgnoreCase(TipoMinistranteEnum tipo, String nome);

    /**
     * Verifica se já existe um ministrante associado a um ID de servidor.
     * Usado para evitar a duplicação de ministrantes internos.
     *
     * @param idServidor O ID do servidor a ser verificado.
     * @return true se um ministrante com o ID de servidor já existe, false caso contrário.
     */
    boolean existsByIdServidor(Integer idServidor);

    /**
     * Verifica se já existe um ministrante com um determinado CPF.
     * Usado para evitar a duplicação de ministrantes externos.
     *
     * @param cpf O CPF a ser verificado.
     * @return true se um ministrante com o CPF já existe, false caso contrário.
     */
    boolean existsByCpf(String cpf);

}

