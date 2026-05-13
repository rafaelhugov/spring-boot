package mppb.restapi.eleicao.feature.voto.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mppb.restapi.common.core.entity.AbstractEntity;
import mppb.restapi.eleicao.feature.candidato.entity.Candidato;
import mppb.restapi.eleicao.feature.eleicao.entity.Eleicao;
import org.hibernate.annotations.SQLRestriction;

import java.util.List;

@Entity
@Table(name = "voto", schema = "eleicao")
@Getter
@Setter
@NoArgsConstructor
@SQLRestriction("excluido = false")
public class Voto extends AbstractEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_eleicao", nullable = false)
    private Eleicao eleicao;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "votocandidato",
        schema = "eleicao",
        joinColumns = @JoinColumn(name = "id_voto"),
        inverseJoinColumns = @JoinColumn(name = "id_candidato")
    )
    private List<Candidato> candidatos;

    @Column(nullable = false, length = 1)
    private String tipo; // V (Voto), B (Branco), N (Nulo)

    private String hash;

    private String ip;

    @Column(nullable = false)
    private Boolean excluido = false;
}
