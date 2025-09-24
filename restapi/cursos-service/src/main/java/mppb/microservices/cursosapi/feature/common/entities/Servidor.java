package mppb.microservices.cursosapi.feature.common.entities;

import jakarta.persistence.*;
import lombok.*;
import mppb.microservices.common.core.entity.AbstractEntity;
import mppb.microservices.common.core.util.Esquemas;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "servidor", schema = Esquemas.SCHEMA_PUBLIC) // Nome da tabela atualizado
@Builder
public class Servidor extends AbstractEntity<Long> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
