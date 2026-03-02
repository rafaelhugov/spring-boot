package mppb.restapi.mobile.feature.curso.entity;

import jakarta.persistence.*;
import lombok.*;
import mppb.restapi.common.core.entity.AbstractEntity;
import mppb.restapi.common.core.util.Esquemas;
import mppb.restapi.mobile.feature.lookup.entities.PapelMinistrante;
import mppb.restapi.mobile.feature.ministrante.entity.Ministrante;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cursoministrante", schema = Esquemas.SCHEMA_CURSOS)
@Builder
public class CursoMinistrante extends AbstractEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_curso", nullable = false)
    private Curso curso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ministrante", nullable = false)
    private Ministrante ministrante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_papel", nullable = false)
    private PapelMinistrante papel;

}
