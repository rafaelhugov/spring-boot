package mppb.restapi.mobile.feature.lookup.entities;

import jakarta.persistence.*;
import lombok.*;
import mppb.restapi.common.core.entity.AbstractEntity;
import mppb.restapi.common.core.util.Esquemas;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "papelministrante", schema = Esquemas.SCHEMA_CURSOS) // Nome da tabela atualizado
@Builder
public class PapelMinistrante  extends AbstractEntity<Long> {

    @Column(name = "nome")
    private String nome;
}
