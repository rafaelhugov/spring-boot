package mppb.microservices.fundoapi.feature.cobranca.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import mppb.microservices.common.core.util.Constants;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fundo", schema = Constants.SCHEMA_PREFIX)
@Builder
public class Fundo {

    @Id
    private Long id;

    @Column(name = "sigla", unique = true)
    private String sigla;

    @Column(name = "nome")
    private String nome;

    @Column(name = "razaosocial")
    private String razaoSocial;

    @Column(name = "convenio")
    private Long convenio;

    @Column(name = "agencia")
    private Long agencia;

    @Column(name = "conta")
    private Long conta;

    @Column(name = "carteira")
    private Long carteira;

    @Column(name = "variacaocarteira")
    private Long variacaoCarteira;

    @Column(name = "cnpj")
    private String cnpj;

    @Column(name = "diasparavencimento")
    private Long diasParaVencimento;

}