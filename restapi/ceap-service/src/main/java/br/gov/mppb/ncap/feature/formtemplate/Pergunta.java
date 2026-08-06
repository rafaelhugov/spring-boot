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
@Table(name = "perguntas", schema = "ncap")
@AttributeOverride(name = "id", column = @Column(name = "id_pergunta"))
public class Pergunta extends AbstractEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_bloco", nullable = false)
    private Bloco bloco;

    @Column(name = "idcnmp", length = 100)
    private String idcnmp;

    @Column(name = "label", nullable = false, columnDefinition = "TEXT")
    private String label;

    @Column(name = "campotipo", nullable = false, length = 50)
    private String campotipo;

    @Builder.Default
    @Column(name = "obrigatorio", nullable = false)
    private Boolean obrigatorio = false;

    @Builder.Default
    @Column(name = "exportarcnmp", nullable = false)
    private Boolean exportarcnmp = true;

    @Column(name = "ordem", nullable = false)
    private Integer ordem;

    @Builder.Default
    @Column(name = "tabulacao", nullable = false)
    private Integer tabulacao = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_dependenciapai")
    private Pergunta dependenciapai;

    @Column(name = "dependenciavalor")
    private String dependenciavalor;

    @Builder.Default
    @OneToMany(mappedBy = "pergunta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OpcaoResposta> opcoes = new ArrayList<>();
}
