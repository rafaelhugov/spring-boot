package mppb.restapi.cursos.feature.common.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mppb.restapi.common.core.util.Esquemas;
import java.time.LocalDateTime;

@Entity
@Table(name = "arquivo", schema = Esquemas.SCHEMA_GED)
@Getter
@Setter
@NoArgsConstructor
public class Arquivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nomearquivo", nullable = false)
    private String nomeArquivo;

    @Column(name = "mimetype")
    private String mimeType;

    @Column(name = "tamanho")
    private Long tamanho;

    @Column(name = "bytes")
    private byte[] bytes;

    @Column(name = "dataregistro", updatable = false)
    private LocalDateTime dataRegistro;

    @PrePersist
    protected void onCreate() {
        this.dataRegistro = LocalDateTime.now();
    }
}
