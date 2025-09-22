package mppb.microservices.fundoapi.feature.common.entities;

import jakarta.persistence.*;
import lombok.*;
import mppb.microservices.common.core.entity.AbstractEntity;
import mppb.microservices.common.core.util.Constants;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pessoa", schema = Constants.SCHEMA_PUBLIC)
@Builder
public class Pessoa extends AbstractEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence")
    @SequenceGenerator(name = "sequence", sequenceName = Constants.SCHEMA_PUBLIC+".pessoa_id_seq", allocationSize = 1)
    private Long id;

    @Column(name="nome")
    private String nome;

}