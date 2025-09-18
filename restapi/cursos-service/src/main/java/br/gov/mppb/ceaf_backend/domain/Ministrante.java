package br.gov.mppb.ceaf_backend.domain;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "ministrantes")
@Data
public class Ministrante {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_servidor")
    private Integer idServidor;

    @Column(name = "nome")
    private String nome;

    @Column(name = "nome_orgao_externo")
    private String nomeOrgaoExterno;

    @ManyToOne
    @JoinColumn(name = "id_papel")
    private PapelMinistrante papel;

    // Campo temporário para ajudar na comunicação com o frontend
    @Transient
    private String tipo = "interno"; // 'interno' ou 'externo'

    @Transient
    private String cargo;
}