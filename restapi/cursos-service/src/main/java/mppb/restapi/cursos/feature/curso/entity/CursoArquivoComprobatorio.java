package mppb.restapi.cursos.feature.curso.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mppb.restapi.common.core.entity.AbstractEntity;
import mppb.restapi.common.core.util.Esquemas;
import mppb.restapi.cursos.feature.common.entities.Arquivo;

import java.time.LocalDateTime;

@Entity
@Table(name = "cursoarquivocomprobatorio", schema = Esquemas.SCHEMA_CURSOS)
@Getter
@Setter
@NoArgsConstructor
public class CursoArquivoComprobatorio extends AbstractEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_curso", nullable = false)
    private Curso curso;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_arquivo", nullable = false)
    private Arquivo arquivo;

    @Column(name = "dataregistro", nullable = false, updatable = false)
    private LocalDateTime dataRegistro;

    @PrePersist
    protected void onCreate() {
        this.dataRegistro = LocalDateTime.now();
    }
}
