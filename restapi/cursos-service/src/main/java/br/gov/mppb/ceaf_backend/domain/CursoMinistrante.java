package br.gov.mppb.ceaf_backend.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
@Table(name = "cursoministrante")
public class CursoMinistrante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_curso")
    @JsonBackReference
    private Curso curso;

    @ManyToOne(fetch = FetchType.EAGER) // Eager para facilitar o acesso ao nome
    @JoinColumn(name = "id_ministrante")
    private Ministrante ministrante;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_papel")
    private PapelMinistrante papel;

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Curso getCurso() { return curso; }
    public void setCurso(Curso curso) { this.curso = curso; }
    public Ministrante getMinistrante() { return ministrante; }
    public void setMinistrante(Ministrante ministrante) { this.ministrante = ministrante; }
    public PapelMinistrante getPapel() { return papel; }
    public void setPapel(PapelMinistrante papel) { this.papel = papel; }
}

