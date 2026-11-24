package mppb.restapi.mobile.feature.mensagem.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mppb.restapi.mobile.feature.mensagem.enums.CategoriaAnexo;

@Entity
@Table(name = "mensagemanexo", schema = "diariooficial")
@Getter
@Setter
@NoArgsConstructor
public class MensagemAnexo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mensagem_id")
    private Long mensagemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CategoriaAnexo categoria;

    @Column(name = "nomeoriginal", nullable = false, length = 255)
    private String nomeOriginal;

    @Column(name = "mimetype", nullable = false, length = 120)
    private String mimeType;

    @Column(name = "tamanhobytes", nullable = false)
    private Long tamanhoBytes;

    @Column(name = "duracaosegundos")
    private Integer duracaoSegundos;

    @Column(name = "caminhostorage", nullable = false, length = 500)
    private String caminhoStorage;

    @Column(name = "caminhothumbnail", length = 500)
    private String caminhoThumbnail;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "conteudobytes")
    private byte[] conteudoBytes;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "thumbnailbytes")
    private byte[] thumbnailBytes;

    @Column(name = "dataregistro", nullable = false)
    private LocalDateTime dataRegistro;

    @PrePersist
    void prePersist() {
        if (dataRegistro == null) {
            dataRegistro = LocalDateTime.now();
        }
    }
}

