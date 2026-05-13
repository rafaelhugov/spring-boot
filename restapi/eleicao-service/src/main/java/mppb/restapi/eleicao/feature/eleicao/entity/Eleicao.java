package mppb.restapi.eleicao.feature.eleicao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mppb.restapi.common.core.entity.AbstractEntity;
import mppb.restapi.eleicao.feature.candidato.entity.Candidato;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "eleicao", schema = "eleicao")
@Getter
@Setter
@NoArgsConstructor
@SQLRestriction("excluido = false")
public class Eleicao extends AbstractEntity<Long> {

    @Column(nullable = false)
    private String titulo;

    @Column(name = "dataeleicao")
    private LocalDate dataEleicao;

    @Column(name = "datainicial")
    private LocalDateTime dataInicial;

    @Column(name = "datafinal")
    private LocalDateTime dataFinal;

    @Column(name = "tipocandidato")
    private String tipoCandidato;

    @Column(name = "votosporeleitor")
    private Integer votosPorEleitor = 1;

    private Integer ordem = 0;

    @OneToMany(mappedBy = "eleicao")
    private List<Candidato> candidatos;

    @org.hibernate.annotations.Formula("(select count(el.id) from eleicao.eleitor el where el.id_eleicao = id and el.excluido = false)")
    private Integer numeroEleitoresTotal;

    @org.hibernate.annotations.Formula("(select count(v.id) from eleicao.voto v where v.id_eleicao = id and v.excluido = false)")
    private Integer numeroEleitoresVotaram;

    @org.hibernate.annotations.Formula("(select count(vc.id_candidato) from eleicao.voto v join eleicao.votocandidato vc on vc.id_voto = v.id where v.id_eleicao = id and v.excluido = false)")
    private Integer numeroVotosComputados;

    @Column(nullable = false)
    private Boolean excluido = false;
}
