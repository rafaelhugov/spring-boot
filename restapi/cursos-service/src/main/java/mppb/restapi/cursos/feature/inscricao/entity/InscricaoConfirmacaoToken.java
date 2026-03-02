package mppb.restapi.cursos.feature.inscricao.entity;

import jakarta.persistence.*;
import lombok.*;
import mppb.restapi.common.core.entity.AbstractEntity;
import mppb.restapi.common.core.util.Esquemas;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inscricaoconfirmacaotoken", schema = Esquemas.SCHEMA_CURSOS)
@Builder
public class InscricaoConfirmacaoToken extends AbstractEntity<Long> {

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "datacriacao", nullable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "dataexpiracao", nullable = false)
    private LocalDateTime dataExpiracao;

    @Column(name = "dataconfirmacao")
    private LocalDateTime dataConfirmacao;

    @OneToOne(targetEntity = Inscricao.class, fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "id_inscricao")
    private Inscricao inscricao;

    public InscricaoConfirmacaoToken(Inscricao inscricao, int validadeEmMinutos) {
        this.inscricao = inscricao;
        this.token = UUID.randomUUID().toString();
        this.dataCriacao = LocalDateTime.now();
        this.dataExpiracao = this.dataCriacao.plusMinutes(validadeEmMinutos);
    }

}

