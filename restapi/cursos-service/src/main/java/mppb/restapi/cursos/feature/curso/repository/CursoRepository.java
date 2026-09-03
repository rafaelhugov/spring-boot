package mppb.restapi.cursos.feature.curso.repository;

import mppb.restapi.common.core.repository.BaseRepository;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.curso.enums.StatusCursoEnum;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface CursoRepository  extends BaseRepository<Curso, Long> {
    Optional<Curso> findByHashInscricao(UUID hash);

    @Modifying
    @Query("""
            UPDATE Curso c
               SET c.realizacaoDataInicial = :inicio,
                   c.realizacaoDataFinal = :fim,
                   c.cargaHoraria = :cargaHoraria,
                   c.status = :status,
                   c.dataModificacao = CURRENT_TIMESTAMP
             WHERE c.id = :id
            """)
    int atualizarDadosRecalculados(
            @Param("id") Long id,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("cargaHoraria") String cargaHoraria,
            @Param("status") StatusCursoEnum status);

    @Modifying
    @Query("UPDATE Curso c SET c.status = mppb.restapi.cursos.feature.curso.enums.StatusCursoEnum.FINALIZADO WHERE c.status <> mppb.restapi.cursos.feature.curso.enums.StatusCursoEnum.FINALIZADO AND c.realizacaoDataFinal < :agora")
    int atualizarCursosConcluidos(@Param("agora") LocalDateTime agora);
}
