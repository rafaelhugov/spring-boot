package mppb.restapi.cursos.feature.participante.repository;

import mppb.restapi.common.core.repository.BaseRepository;
import mppb.restapi.cursos.feature.participante.entity.Participante;
import mppb.restapi.cursos.feature.participante.enums.TipoParticipanteEnum;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipanteRepository extends BaseRepository<Participante, Long> {

    /**
     * Busca um participante pelo seu CPF.
     * Usado para identificar participantes externos.
     *
     * @param cpf O CPF do participante.
     * @return Um Optional contendo o participante, se encontrado.
     */
    Optional<Participante> findByCpf(String cpf);

    /**
     * Busca um participante pelo seu ID de servidor.
     * Usado para identificar participantes internos.
     *
     * @param idServidor O ID do servidor.
     * @return Um Optional contendo o participante, se encontrado.
     */
    Optional<Participante> findByIdServidor(Integer idServidor);

    /**
     * Verifica se já existe um participante com o CPF informado.
     *
     * @param cpf O CPF a ser verificado.
     * @return true se existir, false caso contrário.
     */
    boolean existsByCpf(String cpf);

    /**
     *  Busca de participantes externos
     *
     */
    List<Participante> findAllByTipoAndNomeContainingIgnoreCase(TipoParticipanteEnum tipo, String nome);
}
