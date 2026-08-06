package br.gov.mppb.ncap.feature.entidade;

import jakarta.persistence.*;
import lombok.*;
import mppb.restapi.common.core.entity.AbstractEntity;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Formula;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "entidades", schema = "ncap")
@AttributeOverride(name = "id", column = @Column(name = "id_entidade"))
public class Entidade extends AbstractEntity<Long> {

    @Column(name = "id_cnmp", unique = true)
    private Long idCnmp;

    @Column(name = "nomecnmp", nullable = false)
    private String nomecnmp;

    @Column(name = "descricaomppb")
    private String descricaomppb;

    @Column(name = "tipoentidade", nullable = false, length = 150)
    private String tipoentidade;

    @Column(name = "id_comarca", nullable = false)
    private Long idComarca;

    /**
     * Nome da comarca obtido via subquery do Hibernate (@Formula).
     * Campo somente leitura — não persiste na tabela ncap.entidades.
     */
    @Formula("(SELECT c.nome FROM tj.comarca c WHERE c.id = id_comarca AND c.excluido = false LIMIT 1)")
    private String nomeComarca;

    @Column(name = "municipio", nullable = false)
    private String municipio;

    @Builder.Default
    @Column(name = "ativa", nullable = false)
    private Boolean ativa = true;

    @CreationTimestamp
    @Column(name = "criadoem", nullable = false, updatable = false)
    private Instant criadoem;
}
