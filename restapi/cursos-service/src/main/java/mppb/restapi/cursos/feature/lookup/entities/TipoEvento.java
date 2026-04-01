package mppb.restapi.cursos.feature.lookup.entities;

import jakarta.persistence.*;
import lombok.*;
import mppb.restapi.common.core.entity.AbstractEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tipoevento", schema = "curso")
@Builder
public class TipoEvento  extends AbstractEntity<Long> {

    @Column(name = "descricao", nullable = false)
    private String descricao;
}
