package mppb.restapi.cursos.feature.frequencia.entity;

import jakarta.persistence.*;
import lombok.*;
import mppb.restapi.common.core.entity.AbstractEntity;
import mppb.restapi.common.core.util.Esquemas;
import mppb.restapi.cursos.feature.curso.entity.Curso;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "aula", schema = Esquemas.SCHEMA_CURSOS)
@Builder
public class Aula extends AbstractEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_curso", nullable = false)
    private Curso curso;

    @Column(name = "descricao", nullable = false)
    private String descricao;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(name = "horainicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "horafim", nullable = false)
    private LocalTime horaFim;

    //@Column(name = "ativa")
    //private boolean ativa;

    // Relacionamento bidirecional para facilitar o gerenciamento das chamadas da aula
    @OneToMany(mappedBy = "aula", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Chamada> chamadas = new ArrayList<>();
}