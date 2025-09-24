package mppb.microservices.cursosapi.feature.lookup.entities;

import jakarta.persistence.*;
import lombok.*;
import mppb.microservices.common.core.util.Esquemas;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tipoevento", schema = Esquemas.SCHEMA_CURSOS) // Nome da tabela atualizado
@Builder
public class TipoEvento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "descricao", nullable = false)
    private String descricao;
}
