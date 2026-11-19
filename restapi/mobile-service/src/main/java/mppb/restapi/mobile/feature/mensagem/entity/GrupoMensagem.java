package mppb.restapi.mobile.feature.mensagem.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "grupomensagem", schema = "diariooficial")
@Getter
@Setter
@NoArgsConstructor
public class GrupoMensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 300)
    private String descricao;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criadopormatricula", nullable = false, length = 20)
    private String criadoPorMatricula;

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

