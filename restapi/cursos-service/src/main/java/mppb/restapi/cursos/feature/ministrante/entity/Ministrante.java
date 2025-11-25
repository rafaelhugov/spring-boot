package mppb.restapi.cursos.feature.ministrante.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.*;
import mppb.restapi.common.core.entity.AbstractEntity;
import mppb.restapi.common.core.util.Esquemas;
import mppb.restapi.cursos.feature.ministrante.converter.enums.TipoMinistranteEnumConverter;
import mppb.restapi.cursos.feature.ministrante.enums.TipoMinistranteEnum;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ministrante", schema = Esquemas.SCHEMA_CURSOS)
@Builder
public class Ministrante extends AbstractEntity<Long> {

    @Column(name = "id_servidor", unique = true)
    private Integer idServidor;

    @Column(name = "nome")
    private String nome;

    @Column(name = "cpf", length = 11, unique = true)
    @Size(min = 11, max = 11, message = "CPF deve conter 11 dígitos.")
    private String cpf;

    @Column(name = "nomeorgaoexterno")
    private String nomeOrgaoExterno;

    @Convert(converter = TipoMinistranteEnumConverter.class)
    @Column(name = "tipo", nullable = false, length = 1)
    private TipoMinistranteEnum tipo;

}
