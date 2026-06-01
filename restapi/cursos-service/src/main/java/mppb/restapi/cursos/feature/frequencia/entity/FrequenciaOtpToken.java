package mppb.restapi.cursos.feature.frequencia.entity;

import jakarta.persistence.*;
import lombok.*;
import mppb.restapi.common.core.entity.AbstractEntity;
import mppb.restapi.common.core.util.Esquemas;
import mppb.restapi.cursos.feature.inscricao.entity.Inscricao;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "frequenciaotptoken", schema = Esquemas.SCHEMA_CURSOS)
@Builder
public class FrequenciaOtpToken extends AbstractEntity<Long> {

    @Column(name = "codigo", nullable = false, length = 6)
    private String codigo;

    @Column(name = "datacriacao", nullable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "dataexpiracao", nullable = false)
    private LocalDateTime dataExpiracao;

    @ManyToOne(targetEntity = Chamada.class, fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "id_chamada")
    private Chamada chamada;

    @ManyToOne(targetEntity = Inscricao.class, fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "id_inscricao")
    private Inscricao inscricao;

    public FrequenciaOtpToken(Chamada chamada, Inscricao inscricao, String codigo, int validadeEmMinutos) {
        this.chamada = chamada;
        this.inscricao = inscricao;
        this.codigo = codigo;
        this.dataCriacao = LocalDateTime.now();
        this.dataExpiracao = this.dataCriacao.plusMinutes(validadeEmMinutos);
    }
}
