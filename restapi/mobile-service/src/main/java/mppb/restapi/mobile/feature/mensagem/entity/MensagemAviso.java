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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mppb.restapi.mobile.feature.mensagem.enums.ModalidadeEnvio;
import mppb.restapi.mobile.feature.mensagem.enums.PrioridadeMensagem;
import mppb.restapi.mobile.feature.mensagem.enums.StatusPublicacao;

@Entity
@Table(name = "mensagemaviso", schema = "diariooficial")
@Getter
@Setter
@NoArgsConstructor
public class MensagemAviso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 160)
    private String titulo;

    @Column(columnDefinition = "text")
    private String conteudo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PrioridadeMensagem prioridade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ModalidadeEnvio modalidade;

    @Column(name = "exigeciencia", nullable = false)
    private boolean exigeCiencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "statuspublicacao", nullable = false, length = 16)
    private StatusPublicacao statusPublicacao;

    @Column(name = "criteriosjson", columnDefinition = "text")
    private String criteriosJson;

    @Column(name = "remetentematricula", nullable = false, length = 20)
    private String remetenteMatricula;

    @Column(name = "remetentenome", nullable = false, length = 180)
    private String remetenteNome;

    @Column(name = "dataagendamento")
    private LocalDateTime dataAgendamento;

    @Column(name = "dataenvio")
    private LocalDateTime dataEnvio;

    @Column(name = "dataexpiracao")
    private LocalDateTime dataExpiracao;

    @Column(name = "arquivadaremetente", nullable = false)
    private boolean arquivadaRemetente;

    @Column(name = "dataregistro", nullable = false)
    private LocalDateTime dataRegistro;

    @Column(name = "dataatualizacao", nullable = false)
    private LocalDateTime dataAtualizacao;

    @PrePersist
    void prePersist() {
        LocalDateTime agora = LocalDateTime.now();
        if (dataRegistro == null) {
            dataRegistro = agora;
        }
        dataAtualizacao = agora;
    }

    @PreUpdate
    void preUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }
}

