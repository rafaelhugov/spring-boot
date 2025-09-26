package br.gov.mppb.ceaf_backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "ministrante")
@Data
public class Ministrante {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_servidor")
    private Long idServidor;

    @Column(name = "nome")
    private String nome;

    @Column(name = "nomeorgaoexterno")
    private String nomeOrgaoExterno;

    @ManyToOne
    @JoinColumn(name = "id_papel")
    private PapelMinistrante papel;

    @OneToMany(mappedBy = "ministrante", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore // Ignora este campo na serialização para evitar loops
    private Set<CursoMinistrante> cursoMinistrantes = new HashSet<>();

    // Campo temporário para ajudar na comunicação com o frontend
    @Transient
    private String tipo = "interno"; // 'interno' ou 'externo'

    @Transient
    private String cargo;
}