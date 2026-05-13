package mppb.restapi.eleicao.feature.candidato.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mppb.restapi.common.core.entity.AbstractEntity;
import mppb.restapi.eleicao.feature.eleicao.entity.Eleicao;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "candidato", schema = "eleicao")
@Getter
@Setter
@NoArgsConstructor
@SQLRestriction("excluido = false")
public class Candidato extends AbstractEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_eleicao", nullable = false)
    private Eleicao eleicao;

    @Column(name = "id_servidor")
    private Long idServidor;

    @Column(name = "id_foto")
    private Long idFoto;

    private String titulo;

    private Integer ordem = 0;

    private Boolean ativo = true;

    @Column(nullable = false)
    private Boolean excluido = false;
}
