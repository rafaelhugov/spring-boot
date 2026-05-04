package mppb.restapi.cursos.feature.lookup.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "localevento", schema = "curso")
@Getter
@Setter
public class LocalEvento {
    @Id
    private Long id;

    @Column(name = "descricao", nullable = false, length = 255)
    private String nome;

    @Column(name = "externo")
    private Boolean externo = false;
}
