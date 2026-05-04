package mppb.restapi.cursos.feature.lookup.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "statustramitacao", schema = "curso")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StatusTramitacao {
    @Id
    private Long id;

    @Column(nullable = false, length = 50)
    private String descricao;
}
