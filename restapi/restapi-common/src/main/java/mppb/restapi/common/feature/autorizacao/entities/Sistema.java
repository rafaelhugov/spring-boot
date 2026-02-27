package mppb.restapi.common.feature.autorizacao.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import mppb.restapi.common.core.util.Esquemas;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable // Esta aplicação não deve escrever nesta tabela
@Table(name = "sistema", schema = Esquemas.SCHEMA_AUTORIZACAO)
@Getter
public class Sistema {

    @Id
    @Column(name = "id")
    private Integer id; // int4 é Integer

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "sigla", nullable = false, unique = true)
    private String sigla;

    @Column(name = "url")
    private String url;
}
