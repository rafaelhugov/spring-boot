package mppb.restapi.cursos.feature.lookup.entities;

import jakarta.persistence.*;
import lombok.*;
import mppb.restapi.common.core.entity.AbstractEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "estado", schema = "public")
@Builder
public class Estado extends AbstractEntity<Long> {

    @Column(name = "nome")
    private String nome;

    @Column(name = "sigla")
    private String sigla;
}
