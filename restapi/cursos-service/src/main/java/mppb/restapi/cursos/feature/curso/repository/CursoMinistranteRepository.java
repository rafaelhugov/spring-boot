package mppb.restapi.cursos.feature.curso.repository;

import mppb.restapi.common.core.repository.BaseRepository;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.curso.entity.CursoMinistrante;
import mppb.restapi.cursos.feature.ministrante.entity.Ministrante;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CursoMinistranteRepository extends BaseRepository<CursoMinistrante, Long> {

    /**
     * Busca todas as associações de ministrantes para um determinado curso.
     *
     * @param cursoId O ID do curso.
     * @return Uma lista de associações CursoMinistrante.
     */
    List<CursoMinistrante> findByCursoId(Long cursoId);

    /**
     * Verifica se já existe uma associação entre um curso e um ministrante específicos.
     * Usado para evitar a duplicação de ministrantes num mesmo curso.
     *
     * @param curso O objeto do Curso.
     * @param ministrante O objeto do Ministrante.
     * @return true se a associação já existe, false caso contrário.
     */
    boolean existsByCursoAndMinistrante(Curso curso, Ministrante ministrante);

    /**
     * Busca uma associação específica pelo seu ID e pelo ID do curso, garantindo que ela pertence ao curso correto.
     * Usado na operação de remoção segura.
     *
     * @param id O ID da associação (da tabela cursoministrante).
     * @param cursoId O ID do curso ao qual a associação deve pertencer.
     * @return Um Optional contendo a associação, se encontrada.
     */
    Optional<CursoMinistrante> findByIdAndCursoId(Long id, Long cursoId);
}
