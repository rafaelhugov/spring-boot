package mppb.microservices.fundoapi.feature.cobranca.entity;

import jakarta.persistence.*;
import lombok.*;
import mppb.microservices.common.core.util.Constants;
import mppb.microservices.fundoapi.feature.cobranca.converter.enums.TipoSolicitacaoEnumConverter;
import mppb.microservices.fundoapi.feature.cobranca.enums.TipoSolicitacaoEnum;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "natureza", schema = Constants.SCHEMA_PREFIX)
@Builder
public class Natureza {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence")
    @SequenceGenerator(name = "sequence", sequenceName = Constants.SCHEMA_PREFIX+".natureza_id_seq", allocationSize = 1)
    private Long id;

    @Column
    private String nome;

    @Column(name = "tipo",  nullable = true)
    @Convert(converter = TipoSolicitacaoEnumConverter.class)
    private TipoSolicitacaoEnum tipo;
}
