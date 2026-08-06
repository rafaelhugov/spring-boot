package br.gov.mppb.ncap.feature.inspecao;

import br.gov.mppb.ncap.feature.formtemplate.Pergunta;
import jakarta.persistence.*;
import lombok.*;
import mppb.restapi.common.core.entity.AbstractEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "respostasinspecao", schema = "ncap")
@AttributeOverride(name = "id", column = @Column(name = "id_respostainspecao"))
public class RespostaInspecao extends AbstractEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_inspecao", nullable = false)
    private Inspecao inspecao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pergunta", nullable = false)
    private Pergunta pergunta;

    @Column(name = "valorresposta", columnDefinition = "TEXT")
    private String valorresposta;

    @Column(name = "observacaoprevisita", columnDefinition = "TEXT")
    private String observacaoprevisita;

    @Column(name = "observacaocorregedoria", columnDefinition = "TEXT")
    private String observacaocorregedoria;
}
