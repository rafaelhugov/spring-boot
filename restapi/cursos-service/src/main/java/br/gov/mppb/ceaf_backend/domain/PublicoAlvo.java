package br.gov.mppb.ceaf_backend.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "publicoalvo")
@Data
@EqualsAndHashCode(of = "id") // Importante para o funcionamento do Set
public class PublicoAlvo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "internoexterno", nullable = false, length = 1)
    private String internoExterno;
}
