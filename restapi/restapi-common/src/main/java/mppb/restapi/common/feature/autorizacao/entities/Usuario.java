package mppb.restapi.common.feature.autorizacao.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import mppb.restapi.common.core.entity.AbstractEntity;
import mppb.restapi.common.core.util.Esquemas;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "usuario", schema = Esquemas.SCHEMA_AUTORIZACAO) // Nome da tabela atualizado
@Builder
public class Usuario extends AbstractEntity<Long> {

    @Column(name="nome")
    private String nome;

    @Column(name="login")
    private String login;

    @Column(name="matricula")
    private String matricula;

    @Column(name="ativo")
    private Boolean ativo;

    @Column(name="dataregistro")
    private LocalDateTime dataRegistro;

    @Column(name="dataatualizacao")
    private LocalDateTime dataAtualizacao;

    @Column(name="excluido")
    private Boolean excluido;

}

