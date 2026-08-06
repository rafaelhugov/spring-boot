package br.gov.mppb.ncap.feature.inspecao;

import mppb.restapi.common.core.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InspecaoRepository extends BaseRepository<Inspecao, Long> {

    Page<Inspecao> findByEntidadeNomecnmpContainingIgnoreCase(String search, Pageable pageable);

    Page<Inspecao> findByIdPromotorresponsavel(Long idPromotorresponsavel, Pageable pageable);

    Optional<Inspecao> findByTokenacesso(String tokenacesso);

    /**
     * Verifica se já existe uma inspeção ativa para a entidade no semestre informado (RN04 - UC001).
     * Inspeções canceladas/concluídas podem existir, mas não impedem novo agendamento — apenas
     * as com status diferente de CONCLUIDA bloqueiam.
     *
     * @param idEntidade ID da entidade policial
     * @param semestre  Semestre de referência (ex: "2024.1")
     * @return true se já existe agendamento ativo para aquela entidade/semestre
     */
    @Query("SELECT COUNT(i) > 0 FROM Inspecao i " +
           "WHERE i.entidade.id = :idEntidade " +
           "AND i.semestre = :semestre " +
           "AND i.status <> br.gov.mppb.ncap.feature.inspecao.InspecaoStatus.CONCLUIDA")
    boolean existsInspecaoAtivaParaEntidadeESemestre(@Param("idEntidade") Long idEntidade,
                                                     @Param("semestre") String semestre);
}
