package mppb.microservices.cursosapi.feature.lookup.entities;

import jakarta.persistence.*;
import lombok.*;
import mppb.microservices.common.core.util.Esquemas;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "papelministrante", schema = Esquemas.SCHEMA_CURSOS) // Nome da tabela atualizado
@Builder
public class PapelMinistrante {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nome")
    private String nome;
}
