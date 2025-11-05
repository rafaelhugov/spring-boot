package mppb.microservices.fundoapi.feature.cobranca.entity;

import jakarta.persistence.*;
import lombok.*;
import mppb.microservices.common.core.util.Constants;
import mppb.microservices.fundoapi.feature.common.entities.Comarca;
import mppb.microservices.fundoapi.feature.common.entities.Estado;
import mppb.microservices.fundoapi.feature.common.entities.Municipio;
import mppb.microservices.fundoapi.feature.common.entities.Pessoa;
import mppb.microservices.fundoapi.feature.cobranca.converter.enums.TipoSolicitacaoEnumConverter;
import mppb.microservices.fundoapi.feature.cobranca.enums.TipoSolicitacaoEnum;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "solicitacao", schema = Constants.SCHEMA_PREFIX)
@Builder
public class Solicitacao {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence")
    @SequenceGenerator(name = "sequence", sequenceName = Constants.SCHEMA_PREFIX+".solicitacao_id_seq", allocationSize = 1)
    private Long id;

    @Column(name="nome")
    private String nome;

    @Column(name="email")
    private String email;

    @Column(name="telefone")
    private String telefone;

    @Column(name="cpf")
    private String cpf;

    @Column(name="dataadesao")
    private LocalDate dataAdesao;

    @Column(name="cnpj")
    private String cnpj;

    @Column(name="refis")
    private Boolean refis;

    @Column(name="objeto")
    private String objeto;

    @ManyToOne
    @JoinColumn(name = "id_comarca", nullable = true)
    private Comarca comarca;

    @ManyToOne
    @JoinColumn(name = "id_municipio", nullable = false)
    private Municipio municipio;

    @ManyToOne
    @JoinColumn(name = "id_natureza", nullable = false)
    private Natureza natureza;

    @Column(name="numeroprocesso")
    private String numeroProcesso;

    @Column(name="endereco")
    private String endereco;

    @ManyToOne
    @JoinColumn(name = "id_enderecoestado", nullable = false)
    private Estado enderecoEstado;

    @ManyToOne
    @JoinColumn(name = "id_enderecocidade", nullable = false)
    private Municipio enderecoCidade;

    @Column(name="cep")
    private String cep;

    @Column(name="numeroboletos")
    private Long numeroBoletos;

    @ManyToOne
    @JoinColumn(name = "id_orgao", nullable = true)
    private Pessoa orgao;

    @ManyToOne
    @JoinColumn(name = "id_area", nullable = true)
    private Area area;

    @Column(name="datavencimento")
    private LocalDate dataVencimento;

    @Column(name="dataregistro")
    private LocalDateTime dataRegistro;

    @Column(name="valor")
    private Double valor;

    @Column(name = "tipo",  nullable = true)
    @Convert(converter = TipoSolicitacaoEnumConverter.class)
    private TipoSolicitacaoEnum tipo;

    @ManyToOne
    @JoinColumn(name = "tipo", referencedColumnName = "sigla", insertable = false, updatable = false, nullable = true)
    private Fundo fundo;

    @OneToMany(mappedBy = "solicitacao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<Boleto> boletos;

}
