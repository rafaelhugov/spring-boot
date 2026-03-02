package mppb.restapi.mobile.feature.participante.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import mppb.restapi.common.core.entity.AbstractEntity;
import mppb.restapi.common.core.util.Esquemas;
import mppb.restapi.mobile.feature.participante.converter.enums.TipoParticipanteEnumConverter;
import mppb.restapi.mobile.feature.participante.enums.TipoParticipanteEnum;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "participante", schema = Esquemas.SCHEMA_CURSOS, uniqueConstraints = {
        @UniqueConstraint(name = "uk_participante_id_servidor", columnNames = "id_servidor")
})
@Builder
public class Participante extends AbstractEntity<Long> {

    @Column(name = "nome")
    private String nome;

    @Column(name = "cpf", length = 11)
    @Size(min = 11, max = 11, message = "CPF deve conter 11 dígitos.")
    private String cpf;

    @Column(name = "email")
    private String email;

    @Column(name = "telefone", length = 20)
    private String telefone;

    @Column(name = "id_servidor", unique = true)
    private Integer idServidor;

    @Convert(converter = TipoParticipanteEnumConverter.class)
    @Column(name = "tipo", nullable = false, length = 1)
    private TipoParticipanteEnum tipo;

}
