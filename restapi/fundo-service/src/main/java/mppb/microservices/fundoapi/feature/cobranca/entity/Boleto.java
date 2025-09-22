package mppb.microservices.fundoapi.feature.cobranca.entity;

import jakarta.persistence.*;
import lombok.*;
import mppb.microservices.common.core.util.Constants;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "", schema = Constants.SCHEMA_PREFIX)
@Builder
public class Boleto {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence")
    @SequenceGenerator(name = "sequence", sequenceName = Constants.SCHEMA_PREFIX+".boleto_id_seq", allocationSize = 1)
    private Long id;

    @Column(name="identificador")
    private String identificador; // não foi usado UUID pq no projeto JSF ficou como String.

    @Column(name="valor")
    private Double valor;

    @Column(name="valororiginal")
    private Double valorOriginal;

    @ManyToOne
    @JoinColumn(name = "id_solicitacao", nullable = false)
    private Solicitacao solicitacao;

    @Column(name="datavencimento")
    private LocalDate dataVencimento;

    @Column(name="datavencimentooriginal")
    private LocalDate dataVencimentoOriginal;

    @Column(name="datapagamento")
    private LocalDate dataPagamento;

    @Column(name="datainicialgeracao")
    private LocalDate dataInicialGeracao;

    @Column(name="dataatualizacaovalores")
    private LocalDateTime dataAtualizacaoValores;

    @Column(name="ordem")
    private Long ordem;

    @Column(name="impresso")
    private Boolean impresso;

    @Column(name="nossonumero")
    private String nossoNumero;

    @Column(name="pixqrcode")
    private String pixQrcode;

    @Column(name="pixtxid")
    private String pixTxId;

    @Column(name="pixurl")
    private String pixUrl;

    @Column(name="pagamentolinhadigitavel")
    private String pagamentoLinhaDigitavel;

    @Column(name="pagamentocodigobarras")
    private String pagamentoCodigoBarras;

    @Transient
    private boolean faltaPersistir;

}
