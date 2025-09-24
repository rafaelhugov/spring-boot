package mppb.microservices.cursosapi.feature.lookup.entities;


import jakarta.persistence.*;
import lombok.*;
import mppb.microservices.common.core.util.Esquemas;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cursomodalidade", schema = Esquemas.SCHEMA_CURSOS) // Nome da tabela atualizado
@Builder
public class Modalidade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "descricao", nullable = false)
    private String descricao;
}
