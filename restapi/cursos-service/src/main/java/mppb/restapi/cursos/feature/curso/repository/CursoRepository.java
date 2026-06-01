package mppb.restapi.cursos.feature.curso.repository;

import mppb.restapi.common.core.repository.BaseRepository;
import mppb.restapi.cursos.feature.curso.entity.Curso;

import java.util.Optional;
import java.util.UUID;

public interface CursoRepository  extends BaseRepository<Curso, Long> {
    Optional<Curso> findByHashInscricao(UUID hash);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Curso c SET c.status = mppb.restapi.cursos.feature.curso.enums.StatusCursoEnum.FINALIZADO WHERE c.status <> mppb.restapi.cursos.feature.curso.enums.StatusCursoEnum.FINALIZADO AND c.realizacaoDataFinal < :agora")
    int atualizarCursosConcluidos(@org.springframework.data.repository.query.Param("agora") java.time.LocalDateTime agora);
}
