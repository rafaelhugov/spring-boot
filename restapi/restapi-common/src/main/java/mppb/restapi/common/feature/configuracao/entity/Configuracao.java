package mppb.restapi.common.feature.configuracao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import mppb.restapi.common.core.util.Esquemas;
import org.hibernate.annotations.Immutable;

/**
 * Mapeia a tabela de configuração do banco de dados (administrador.configuracao).
 * Marcada como @Immutable, pois esta aplicação não deve escrevê-la.
 */
@Entity
@Immutable
@Table(name = "configuracao", schema = Esquemas.SCHEMA_ADMINISTRADOR)
@Getter
public class Configuracao {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "propriedade", unique = true, nullable = false)
    private String propriedade;

    @Column(name = "valor")
    private String valor;
}

