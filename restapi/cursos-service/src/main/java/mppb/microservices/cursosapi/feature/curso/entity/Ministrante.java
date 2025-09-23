package mppb.microservices.cursosapi.feature.curso.entity;

import jakarta.persistence.*;
import lombok.*;
import mppb.microservices.common.core.util.Esquemas;
import mppb.microservices.cursosapi.feature.lookup.entities.PapelMinistrante;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ministrante", schema = Esquemas.SCHEMA_CURSOS) // Nome da tabela atualizado
@Builder
public class Ministrante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_servidor")
    private Integer idServidor;

    @Column(name = "nome")
    private String nome;

    @Column(name = "nome_orgao_externo")
    private String nomeOrgaoExterno;

    @ManyToOne
    @JoinColumn(name = "id_papel")
    private PapelMinistrante papel;

    // Campo temporário para ajudar na comunicação com o frontend
    @Transient
    private String tipo = "interno"; // 'interno' ou 'externo'

    @Transient
    private String cargo;
}