package mppb.restapi.cursos.feature.inscricao.repository;

import mppb.restapi.common.core.repository.BaseRepository;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.inscricao.entity.Inscricao;
import mppb.restapi.cursos.feature.participante.entity.Participante;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InscricaoRepository extends BaseRepository<Inscricao, Long> {

    /**
     * Busca todas as inscrições de um curso de forma paginada, otimizando a consulta
     * para carregar os dados do participante associado na mesma query (JOIN FETCH).
     * Isso evita o problema de performance N+1 e resolve a LazyInitializationException.
     *
     * @param cursoId O ID do curso.
     * @param pageable As informações de paginação.
     * @return Uma página de inscrições com os participantes já carregados.
     */
    @Query(value = """
            SELECT i FROM Inscricao i
            JOIN FETCH i.participante p
            WHERE i.curso.id = :cursoId
            """,
            countQuery = "SELECT count(i) FROM Inscricao i WHERE i.curso.id = :cursoId")
    Page<Inscricao> findAllByCursoId(@Param("cursoId") Long cursoId, Pageable pageable);

    /**
     * Verifica se já existe uma inscrição para uma combinação específica de curso e participante.
     * Usado para evitar inscrições duplicadas.
     *
     * @param curso O objeto Curso.
     * @param participante O objeto Participante.
     * @return true se a inscrição já existe, false caso contrário.
     */
    boolean existsByCursoAndParticipante(Curso curso, Participante participante);

    /**
     * Verifica se uma inscrição com um determinado ID pertence a um curso específico.
     * Usado para garantir a segurança na operação de remoção.
     *
     * @param id O ID da inscrição.
     * @param cursoId O ID do curso.
     * @return true se a inscrição pertence ao curso, false caso contrário.
     */
    boolean existsByIdAndCursoId(Long id, Long cursoId);

    Optional<Inscricao> findByIdAndCursoId(Long inscricaoId, Long cursoId);

    Optional<Inscricao> findByCursoAndParticipante(Curso curso, Participante participante);

}

