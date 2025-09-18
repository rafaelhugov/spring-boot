package mppb.microservices.fundoapi.feature.cobranca.entity;

import jakarta.persistence.*;
import lombok.*;
import mppb.microservices.common.entity.AbstractEntity;
import mppb.microservices.common.util.Constants;
import mppb.microservices.fundoapi.feature.cobranca.dto.AreaDto;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "area", schema = Constants.SCHEMA_PREFIX)
@Builder
@SqlResultSetMapping(
        name = "AreaDtoMapping",
        classes = @ConstructorResult(
                targetClass = AreaDto.class,
                columns = {
                        @ColumnResult(name = "id", type = Long.class),
                        @ColumnResult(name = "nome", type = String.class)
                }
        )
)
public class Area extends AbstractEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence")
    @SequenceGenerator(name = "sequence", sequenceName = Constants.SCHEMA_PREFIX+".area_id_seq", allocationSize = 1)
    private Long id;

    @Column
    private String nome;

}