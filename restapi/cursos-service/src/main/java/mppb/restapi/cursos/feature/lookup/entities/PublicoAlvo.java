package mppb.restapi.cursos.feature.lookup.entities;

import jakarta.persistence.*;
import lombok.*;
import mppb.restapi.common.core.entity.AbstractEntity;
import mppb.restapi.common.core.util.Esquemas;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "publicoalvo", schema = "curso")
@Builder
public class PublicoAlvo  extends AbstractEntity<Long> {

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "internoexterno")
    private String internoExterno;

    public String getDescricao() {
        return nome;
    }
}
