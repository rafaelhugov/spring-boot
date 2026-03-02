package mppb.restapi.mobile.feature.diariooficial.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import mppb.restapi.common.core.entity.AbstractEntity;
import mppb.restapi.common.core.util.Esquemas;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
