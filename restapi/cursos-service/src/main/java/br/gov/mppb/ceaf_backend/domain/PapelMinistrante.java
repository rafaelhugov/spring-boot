package br.gov.mppb.ceaf_backend.domain;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "papelministrante")
@Data
public class PapelMinistrante {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nome")
    private String nome;
}