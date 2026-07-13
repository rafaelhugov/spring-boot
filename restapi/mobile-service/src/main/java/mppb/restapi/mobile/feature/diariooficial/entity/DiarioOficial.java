package mppb.restapi.mobile.feature.diariooficial.entity;

import jakarta.persistence.*;
import lombok.*;
import mppb.restapi.common.core.entity.AbstractEntity;
import mppb.restapi.common.core.util.Esquemas;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "diariooficial", schema = Esquemas.SCHEMA_DIARIOOFICIAL)
@Builder
public class DiarioOficial extends AbstractEntity<Long> {

        @Column(name = "numeroedicao", nullable = false)
        private Long numeroEdicao;

        @Column(name = "datapublicacao", updatable = false)
        private LocalDate dataPublicacao;

        @Column(name = "id_documentoassinado", nullable = true)
        private Long idDocumentoAssinado;

}
