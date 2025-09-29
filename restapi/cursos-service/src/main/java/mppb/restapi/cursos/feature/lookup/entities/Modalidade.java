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
@Table(name = "cursomodalidade", schema = Esquemas.SCHEMA_CURSOS) // Nome da tabela atualizado
@Builder
public class Modalidade extends AbstractEntity<Long> {

    @Column(name = "descricao", nullable = false)
    private String descricao;
}
