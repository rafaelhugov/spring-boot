package mppb.microservices.cursosapi.feature.curso.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import mppb.microservices.common.core.entity.AbstractEntity;
import mppb.microservices.common.core.util.Esquemas;
import mppb.microservices.cursosapi.feature.cadastro.entities.EntidadeTutora;
import mppb.microservices.cursosapi.feature.lookup.entities.Modalidade;
import mppb.microservices.cursosapi.feature.lookup.entities.PublicoAlvo;
import mppb.microservices.cursosapi.feature.lookup.entities.TipoEvento;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "curso", schema = Esquemas.SCHEMA_CURSOS) // Nome da tabela atualizado
@Builder
public class Curso extends AbstractEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "descricao")
    private String descricao;

    @ManyToOne
    @JoinColumn(name = "id_tipo_evento")
    private TipoEvento tipoEvento;

    @ManyToOne
    @JoinColumn(name = "id_entidade_tutora")
    private EntidadeTutora entidadeTutora;

    @CreationTimestamp
    @Column(name = "data_criacao", updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "inscricao_data_inicial")
    private LocalDateTime inscricaoDataInicial;

    @Column(name = "inscricao_data_final")
    private LocalDateTime inscricaoDataFinal;

    @Column(name = "realizacao_data_inicial")
    private LocalDateTime realizacaoDataInicial;

    @Column(name = "realizacao_data_final")
    private LocalDateTime realizacaoDataFinal;

    @Pattern(regexp = "^\\d+:[0-5]\\d:[0-5]\\d$", message = "Formato de carga horária inválido. Use o formato HH:MM:SS.")
    @Column(name = "carga_horaria", length = 8)
    private String cargaHoraria;

    @Column(name = "percentual_ch_aprovacao")
    private Integer percentualChAprovacao;

    @Column(name = "detalha_carga_horaria")
    private String detalhaCargaHoraria;

    @Column(name = "link_programacao")
    private String linkProgramacao;

    @ManyToOne
    @JoinColumn(name = "id_modalidade")
    private Modalidade modalidade;

    @Column(name = "fl_progressao_funcional", length = 1)
    private String flProgressaoFuncional;

    @Column(name = "hash_inscricao", unique = true)
    private UUID hashInscricao;

    @Transient
    private boolean gerarLink;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "cursos_publico", // Nome da tabela atualizado
            schema = "curso",      // Schema atualizado
            joinColumns = @JoinColumn(name = "id_curso"),
            inverseJoinColumns = @JoinColumn(name = "id_publico_alvo")
    )
    private Set<PublicoAlvo> publicosAlvo = new HashSet<>();

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.EAGER)
    @JoinTable(
            name = "cursos_ministrantes",
            schema = "curso",
            joinColumns = @JoinColumn(name = "id_curso"),
            inverseJoinColumns = @JoinColumn(name = "id_ministrante")
    )
    private Set<Ministrante> ministrantes = new HashSet<>();
}
