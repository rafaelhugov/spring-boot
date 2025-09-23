package mppb.microservices.cursosapi.feature.lookup.entities;

import jakarta.persistence.*;
import lombok.*;
import mppb.microservices.common.core.util.Esquemas;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "publico_alvo", schema = Esquemas.SCHEMA_CURSOS) // Nome da tabela atualizado
@Builder
public class PublicoAlvo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "interno_externo", nullable = false, length = 1)
    private String internoExterno;
}
