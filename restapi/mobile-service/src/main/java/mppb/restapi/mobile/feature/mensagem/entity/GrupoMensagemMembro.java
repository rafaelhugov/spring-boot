package mppb.restapi.mobile.feature.mensagem.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "grupomensagemmembro", schema = "diariooficial")
@Getter
@Setter
@NoArgsConstructor
public class GrupoMensagemMembro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "grupo_id", nullable = false)
    private Long grupoId;

    @Column(nullable = false, length = 20)
    private String matricula;

    @Column(name = "dataregistro", nullable = false)
    private LocalDateTime dataRegistro;

    @PrePersist
    void prePersist() {
        if (dataRegistro == null) {
            dataRegistro = LocalDateTime.now();
        }
    }
}

