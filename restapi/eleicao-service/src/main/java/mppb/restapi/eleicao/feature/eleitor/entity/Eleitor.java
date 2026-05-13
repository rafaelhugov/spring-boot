package mppb.restapi.eleicao.feature.eleitor.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mppb.restapi.common.core.entity.AbstractEntity;
import mppb.restapi.eleicao.feature.eleicao.entity.Eleicao;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "eleitor", schema = "eleicao")
@Getter
@Setter
@NoArgsConstructor
@SQLRestriction("excluido = false")
public class Eleitor extends AbstractEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_eleicao", nullable = false)
    private Eleicao eleicao;

    @Column(name = "id_servidor")
    private Long idServidor;

    private Boolean votou = false;

    @Column(nullable = false)
    private Boolean excluido = false;
}
