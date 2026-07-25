package mppb.restapi.cursos.feature.common.repository;

import mppb.restapi.common.core.repository.BaseRepository;
import mppb.restapi.cursos.feature.common.entities.Arquivo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ArquivoRepository extends BaseRepository<Arquivo, Long> {

    @Query("""
            select (count(c) > 0)
            from Curso c
            where c.headerImageId = :arquivoId
            """)
    boolean existsCursoHeaderImageReference(@Param("arquivoId") Long arquivoId);

    @Query("""
            select (count(c) > 0)
            from Curso c
            where c.certificateTemplateId = :arquivoId
            """)
    boolean existsCursoCertificateTemplateReference(@Param("arquivoId") Long arquivoId);

    @Query("""
            select (count(c) > 0)
            from Curso c
            where c.headerImageId = :arquivoId
              and c.status = mppb.restapi.cursos.feature.curso.enums.StatusCursoEnum.PUBLICADO
            """)
    boolean existsPublicHeaderImageReference(@Param("arquivoId") Long arquivoId);

    @Query("""
            select (count(cac) > 0)
            from CursoArquivoComprobatorio cac
            where cac.arquivo.id = :arquivoId
            """)
    boolean existsArquivoComprobatorioReference(@Param("arquivoId") Long arquivoId);

    @Query("""
            select (count(m) > 0)
            from Ministrante m
            where m.idFoto = :arquivoId
               or m.idCurriculo = :arquivoId
            """)
    boolean existsMinistranteReference(@Param("arquivoId") Long arquivoId);

    @Query("""
            select (count(sm) > 0)
            from SolicitacaoMinistrante sm
            where sm.idFoto = :arquivoId
               or sm.idCurriculo = :arquivoId
            """)
    boolean existsSolicitacaoMinistranteReference(@Param("arquivoId") Long arquivoId);

    @Query("""
            select (count(sm) > 0)
            from SolicitacaoMinistrante sm
            where (sm.idFoto = :arquivoId
                   or sm.idCurriculo = :arquivoId)
              and sm.solicitacao.idServidorInteressado = :servidorId
            """)
    boolean existsSolicitacaoMinistranteReferenceForServidor(@Param("arquivoId") Long arquivoId,
                                                             @Param("servidorId") Long servidorId);

    @Query("""
            select (count(s) > 0)
            from SolicitacaoCurso s
            where s.idArquivoProgramacaoGed = :arquivoId
            """)
    boolean existsSolicitacaoProgramacaoReference(@Param("arquivoId") Long arquivoId);

    @Query("""
            select (count(s) > 0)
            from SolicitacaoCurso s
            where s.idArquivoProgramacaoGed = :arquivoId
              and s.idServidorInteressado = :servidorId
            """)
    boolean existsSolicitacaoProgramacaoReferenceForServidor(@Param("arquivoId") Long arquivoId,
                                                             @Param("servidorId") Long servidorId);
}
