package mppb.restapi.mobile.feature.common.entities;

import jakarta.persistence.*;
import lombok.*;
import mppb.restapi.common.core.entity.AbstractEntity;
import mppb.restapi.common.core.util.Esquemas;

@Getter
@Setter
@AllArgsConstructor
@Entity
@Table(name = "servidor", schema = Esquemas.SCHEMA_PUBLIC) // Nome da tabela atualizado
@Builder
public class Servidor extends AbstractEntity<Long> {
}
