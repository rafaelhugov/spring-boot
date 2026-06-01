package mppb.restapi.cursos.feature.frequencia.entity;

import jakarta.persistence.*;
import lombok.*;
import mppb.restapi.common.core.entity.AbstractEntity;
import mppb.restapi.common.core.util.Esquemas;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chamada", schema = Esquemas.SCHEMA_CURSOS)
@Builder
public class Chamada extends AbstractEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_aula", nullable = false)
    private Aula aula;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "datageracao")
    private LocalDateTime dataGeracao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private mppb.restapi.cursos.feature.frequencia.enums.TipoChamadaEnum tipo;

    @Column(name = "aberta", nullable = false)
    @Builder.Default
    private boolean aberta = true;

    @PrePersist
    public void gerarTokenEData() {
        if (this.token == null) {
            this.token = UUID.randomUUID().toString();
        }
        if (this.dataGeracao == null) {
            this.dataGeracao = LocalDateTime.now();
        }
    }
}