package mppb.restapi.cursos.feature.cadastro.entities;

import jakarta.persistence.*;
import lombok.*;
import mppb.restapi.common.core.entity.AuditableEntity;
import mppb.restapi.common.core.util.Esquemas;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "entidadetutora", schema = Esquemas.SCHEMA_CURSOS) // Nome da tabela atualizado
@Builder
@SQLRestriction("dataexclusao IS NULL")
public class EntidadeTutora  extends AuditableEntity<Long> {

    @Column(name = "nome")
    private String nome;
}
