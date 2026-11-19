package mppb.restapi.mobile.feature.mensagem.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mppb.restapi.mobile.feature.mensagem.enums.StatusDestinatario;

@Entity
@Table(name = "mensagemdestinatario", schema = "diariooficial")
@Getter
@Setter
@NoArgsConstructor
public class MensagemDestinatario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mensagem_id", nullable = false)
    private Long mensagemId;

    @Column(name = "destinatariomatricula", nullable = false, length = 20)
    private String destinatarioMatricula;

    @Column(name = "destinatarionome", nullable = false, length = 180)
    private String destinatarioNome;

    @Column(name = "destinatariosetor", length = 180)
    private String destinatarioSetor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StatusDestinatario status = StatusDestinatario.PENDENTE;

    @Column(name = "dataentrega")
    private LocalDateTime dataEntrega;

    @Column(name = "dataleitura")
    private LocalDateTime dataLeitura;

    @Column(name = "dataciencia")
    private LocalDateTime dataCiencia;

    @Column(name = "arquivadadestinatario", nullable = false)
    private boolean arquivadaDestinatario;

    @Column(name = "dataregistro", nullable = false)
    private LocalDateTime dataRegistro;

    @PrePersist
    void prePersist() {
        if (dataRegistro == null) {
            dataRegistro = LocalDateTime.now();
        }
        if (status == null) {
            status = StatusDestinatario.PENDENTE;
        }
    }
}

