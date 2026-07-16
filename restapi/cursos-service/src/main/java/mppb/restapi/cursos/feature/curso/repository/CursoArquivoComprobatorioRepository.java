package mppb.restapi.cursos.feature.curso.repository;

import mppb.restapi.common.core.repository.BaseRepository;
import mppb.restapi.cursos.feature.curso.entity.CursoArquivoComprobatorio;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CursoArquivoComprobatorioRepository extends BaseRepository<CursoArquivoComprobatorio, Long> {

    @Query("SELECT c FROM CursoArquivoComprobatorio c JOIN FETCH c.arquivo WHERE c.curso.id = :cursoId ORDER BY c.dataRegistro DESC")
    List<CursoArquivoComprobatorio> findAllByCursoIdWithArquivo(@Param("cursoId") Long cursoId);

    Optional<CursoArquivoComprobatorio> findByIdAndCursoId(Long id, Long cursoId);
}
