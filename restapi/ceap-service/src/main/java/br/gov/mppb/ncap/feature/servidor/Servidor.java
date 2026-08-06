package br.gov.mppb.ncap.feature.servidor;

import jakarta.persistence.*;
import lombok.*;
import mppb.restapi.common.core.entity.AbstractEntity;

/**
 * Entidade shell que referencia a tabela corporativa {@code public.servidor}.
 * Os dados são obtidos via queries nativas com {@link ServidorProjection}.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "servidor", schema = "public")
public class Servidor extends AbstractEntity<Long> {

    @Column(name = "id_pessoa")
    private Long idPessoa;

    @Column(name = "id_cargo")
    private Long idCargo;

    @Column(name = "matricula", length = 50)
    private String matricula;

    @Column(name = "ativo", length = 1)
    private String ativo;

    @Column(name = "excluido")
    private Boolean excluido;
}
