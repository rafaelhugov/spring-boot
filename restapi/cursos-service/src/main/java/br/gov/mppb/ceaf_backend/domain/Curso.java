package br.gov.mppb.ceaf_backend.domain;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "curso") // Nome da tabela atualizado
@Data
public class Curso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "descricao")
    private String descricao;

    @ManyToOne
    @JoinColumn(name = "id_tipoevento")
    private TipoEvento tipoEvento;

    @ManyToOne
    @JoinColumn(name = "id_entidadetutora")
    private EntidadeTutora entidadeTutora;

    @CreationTimestamp
    @Column(name = "datacriacao", updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "inscricaodatainicial")
    private LocalDateTime inscricaoDataInicial;

    @Column(name = "inscricaodatafinal")
    private LocalDateTime inscricaoDataFinal;

    @Column(name = "realizacaodatainicial")
    private LocalDateTime realizacaoDataInicial;

    @Column(name = "realizacaodatafinal")
    private LocalDateTime realizacaoDataFinal;

    @Pattern(regexp = "^\\d+:[0-5]\\d:[0-5]\\d$", message = "Formato de carga horária inválido. Use o formato HH:MM:SS.")
    @Column(name = "cargahoraria", length = 8)
    private String cargaHoraria;

    @Column(name = "percentualchaprovacao")
    private Integer percentualChAprovacao;

    @Column(name = "detalhacargahoraria")
    private String detalhaCargaHoraria;

    @Column(name = "linkprogramacao")
    private String linkProgramacao;

    @ManyToOne
    @JoinColumn(name = "id_modalidade")
    private Modalidade modalidade;

    @Column(name = "progressaofuncional", length = 1)
    private String flProgressaoFuncional;

    @Column(name = "hashinscricao", unique = true)
    private UUID hashInscricao;

    @Transient
    private boolean gerarLink;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "cursopublicoalvo", // Nome da tabela atualizado
            schema = "curso",      // Schema atualizado
            joinColumns = @JoinColumn(name = "id_curso"),
            inverseJoinColumns = @JoinColumn(name = "id_publicoalvo")
    )
    private Set<PublicoAlvo> publicosAlvo = new HashSet<>();

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.EAGER)
    @JoinTable(
            name = "cursoministrante",
            schema = "curso",
            joinColumns = @JoinColumn(name = "id_curso"),
            inverseJoinColumns = @JoinColumn(name = "id_ministrante")
    )
    private Set<Ministrante> ministrantes = new HashSet<>();

    @OneToMany(mappedBy = "curso", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private Set<CursoMinistrante> cursoMinistrantes = new HashSet<>();
}
