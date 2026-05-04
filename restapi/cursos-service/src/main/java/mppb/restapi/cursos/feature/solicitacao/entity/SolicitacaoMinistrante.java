package mppb.restapi.cursos.feature.solicitacao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mppb.restapi.cursos.feature.lookup.entities.PapelMinistrante;
import mppb.restapi.cursos.feature.ministrante.entity.Ministrante;

@Entity
@Table(name = "solicitacaoministrante", schema = "curso")
@Getter
@Setter
public class SolicitacaoMinistrante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_solicitacao", nullable = false)
    private SolicitacaoCurso solicitacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_papelministrante")
    private PapelMinistrante papelMinistrante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ministrante")
    private Ministrante ministrante;

    @Column(name = "id_servidor")
    private Integer idServidor;

    @Column(name = "nomeministrante", nullable = false, length = 255)
    private String nomeMinistrante;

    @Column(name = "telefone", length = 150)
    private String telefone;

    @Column(name = "cpf", length = 11)
    private String cpf;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "contatoinfo", length = 150)
    private String contatoInfo;

    @Column(name = "resumocurriculo", columnDefinition = "text")
    private String resumoCurriculo;

    @Column(name = "id_foto", nullable = false)
    private Long idFoto;

    @Column(name = "id_curriculo", nullable = false)
    private Long idCurriculo;

    @PrePersist
    @PreUpdate
    public void prePersistOrUpdate() {
        if (this.nomeMinistrante != null) {
            this.nomeMinistrante = this.nomeMinistrante.toUpperCase();
        }
    }

}
