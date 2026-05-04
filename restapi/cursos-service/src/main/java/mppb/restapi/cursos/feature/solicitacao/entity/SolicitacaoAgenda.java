package mppb.restapi.cursos.feature.solicitacao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "solicitacaoagenda", schema = "curso")
@Getter
@Setter
public class SolicitacaoAgenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_solicitacao", nullable = false)
    private SolicitacaoCurso solicitacao;

    @Column(name = "datainicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "datafim", nullable = false)
    private LocalDate dataFim;

    @Column(name = "horarioinicio", nullable = false)
    private LocalTime horarioInicio;

    @Column(name = "horariofim", nullable = false)
    private LocalTime horarioFim;
}
