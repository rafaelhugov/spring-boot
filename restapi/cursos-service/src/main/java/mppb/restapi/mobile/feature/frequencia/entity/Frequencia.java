package mppb.restapi.mobile.feature.frequencia.entity;

import jakarta.persistence.*;
import lombok.*;
import mppb.restapi.common.core.entity.AbstractEntity;
import mppb.restapi.common.core.util.Esquemas;
import mppb.restapi.mobile.feature.inscricao.entity.Inscricao;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "frequencia", schema = Esquemas.SCHEMA_CURSOS, uniqueConstraints = {
        @UniqueConstraint(name = "uk_frequencia_chamada_inscricao", columnNames = {"id_chamada", "id_inscricao"})
})
@Builder
public class Frequencia extends AbstractEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_chamada", nullable = false)
    private Chamada chamada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_inscricao", nullable = false)
    private Inscricao inscricao;

    @Column(name = "datahoraregistro", nullable = false)
    private LocalDateTime dataHoraRegistro;

    @PrePersist
    public void registrarDataHora() {
        if (this.dataHoraRegistro == null) {
            this.dataHoraRegistro = LocalDateTime.now();
        }
    }
}