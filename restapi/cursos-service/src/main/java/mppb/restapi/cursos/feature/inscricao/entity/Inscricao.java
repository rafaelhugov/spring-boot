package mppb.restapi.cursos.feature.inscricao.entity;

import jakarta.persistence.*;
import lombok.*;
import mppb.restapi.common.core.entity.AbstractEntity;
import mppb.restapi.common.core.util.Esquemas;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.inscricao.converter.enums.SituacaoInscricaoEnumConverter;
import mppb.restapi.cursos.feature.inscricao.enums.SituacaoInscricaoEnum;
import mppb.restapi.cursos.feature.participante.entity.Participante;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inscricao", schema = Esquemas.SCHEMA_CURSOS, uniqueConstraints = {
    @UniqueConstraint(name = "uk_inscricao_curso_participante", columnNames = {"id_curso", "id_participante"})
})
@Builder
public class Inscricao extends AbstractEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_curso", nullable = false)
    private Curso curso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_participante", nullable = false)
    private Participante participante;

    @CreationTimestamp
    @Column(name = "datainscricao", updatable = false)
    private LocalDateTime dataInscricao;

    @Convert(converter = SituacaoInscricaoEnumConverter.class)
    @Column(name = "situacao", nullable = false, length = 1)
    private SituacaoInscricaoEnum situacao;

    @Column(name = "frequencia")
    private Integer frequencia;

    @Column(name = "pcd", nullable = false)
    private Boolean pcd;

    @Column(name = "necessidadeespecial", length = 500)
    private String necessidadeEspecial;

    @Column(name = "cienciacriteriospublicoalvo", nullable = false, columnDefinition = "boolean default false")
    private Boolean cienciaCriteriosPublicoAlvo;

    @Column(name = "datacienciacriteriospublicoalvo")
    private LocalDateTime dataCienciaCriteriosPublicoAlvo;

    @Column(name = "criteriospublicoalvodeclarados", columnDefinition = "text")
    private String criteriosPublicoAlvoDeclarados;

    @PrePersist
    private void onPrePersist() {
        if (this.situacao == null) {
            this.situacao = SituacaoInscricaoEnum.INSCRITO;
        }
        if (this.frequencia == null) {
            this.frequencia = 0;
        }
        if (this.pcd == null) {
            this.pcd = false;
        }
        if (this.cienciaCriteriosPublicoAlvo == null) {
            this.cienciaCriteriosPublicoAlvo = false;
        }
    }
}
