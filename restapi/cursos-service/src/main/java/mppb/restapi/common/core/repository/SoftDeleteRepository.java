package mppb.restapi.common.core.repository;

import jakarta.transaction.Transactional;
import mppb.restapi.common.core.entity.AuditableEntity;
import mppb.restapi.common.feature.autorizacao.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

@NoRepositoryBean
public interface SoftDeleteRepository<T extends AuditableEntity<ID>, ID> extends BaseRepository<T, ID> {


    @Modifying
    @Transactional
    @Query("UPDATE #{#entityName} e " +
            "SET e.dataExclusao = :dataExclusao, e.usuarioExclusao = :usuarioExclusao " +
            "WHERE e.id = :id")
    void softDelete(
            @Param("id") ID id,
            @Param("usuarioExclusao") Usuario usuarioExclusao,
            @Param("dataExclusao") LocalDateTime dataExclusao
    );
}
