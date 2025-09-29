package mppb.restapi.common.core.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mppb.restapi.common.feature.autorizacao.entities.Usuario;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)

public abstract class AuditableEntity<ID> extends AbstractEntity<ID> {

    @CreatedDate
    @Column(name = "dataregistro", nullable = false, updatable = false)
    private LocalDateTime dataRegistro;

    @LastModifiedDate
    @Column(name = "dataatualizacao")
    private LocalDateTime dataAtualizacao;

    @Column(name = "dataexclusao")
    private LocalDateTime dataExclusao;

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuarioregistro", nullable = false, updatable = false)
    private Usuario usuarioRegistro;

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuarioatualizacao")
    private Usuario usuarioAtualizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuarioexclusao")
    private Usuario usuarioExclusao;
}
