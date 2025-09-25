package mppb.restapi.cursos.feature.cadastro.entities;

import jakarta.persistence.*;
import lombok.*;
import mppb.restapi.common.core.entity.AbstractEntity;
import mppb.restapi.common.core.util.Esquemas;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "entidadetutora", schema = Esquemas.SCHEMA_CURSOS) // Nome da tabela atualizado
@Builder
public class EntidadeTutora  extends AbstractEntity<Long>  {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome")
    private String nome;
}
