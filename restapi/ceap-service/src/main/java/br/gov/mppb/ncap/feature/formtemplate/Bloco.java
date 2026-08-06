package br.gov.mppb.ncap.feature.formtemplate;

import jakarta.persistence.*;
import lombok.*;
import mppb.restapi.common.core.entity.AbstractEntity;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "blocos", schema = "ncap")
@AttributeOverride(name = "id", column = @Column(name = "id_bloco"))
public class Bloco extends AbstractEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_secao", nullable = false)
    private Secao secao;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "ordem", nullable = false)
    private Integer ordem;

    @Builder.Default
    @OneToMany(mappedBy = "bloco", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ordem ASC")
    private List<Pergunta> perguntas = new ArrayList<>();
}
