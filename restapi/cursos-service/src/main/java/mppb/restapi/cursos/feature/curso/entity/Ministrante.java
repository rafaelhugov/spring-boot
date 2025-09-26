package mppb.restapi.cursos.feature.curso.entity;

import jakarta.persistence.*;
import lombok.*;
import mppb.restapi.common.core.util.Esquemas;
import mppb.restapi.cursos.feature.lookup.entities.PapelMinistrante;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ministrante", schema = Esquemas.SCHEMA_CURSOS)
@Builder
public class Ministrante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_servidor")
    private Integer idServidor;

    @Column(name = "nome")
    private String nome;

    @Column(name = "nomeorgaoexterno")
    private String nomeOrgaoExterno;

    @ManyToOne
    @JoinColumn(name = "id_papel")
    private PapelMinistrante papel;

    @Transient
    //TODO criar enum
    private String tipo = "interno";

    @Transient
    private String cargo;
}