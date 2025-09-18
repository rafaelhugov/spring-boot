package br.gov.mppb.ceaf_backend.domain;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "tipo_evento")
@Data
public class TipoEvento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "descricao", nullable = false)
    private String descricao;
}
