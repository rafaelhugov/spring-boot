package mppb.restapi.cursos.feature.solicitacao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mppb.restapi.cursos.feature.lookup.entities.PapelMinistrante;

@Entity
@Table(name = "solicitacao_ministrante", schema = "curso")
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
    @JoinColumn(name = "id_papel_ministrante")
    private PapelMinistrante papelMinistrante;

    @Column(name = "id_servidor_interno")
    private Long idServidorInterno; // FK para public.servidor, opcional

    @Column(name = "nome_ministrante", nullable = false, length = 255)
    private String nomeMinistrante;

    @Column(name = "contato_info", length = 150)
    private String contatoInfo;

    @Column(name = "id_arquivo_foto_ged")
    private Long idArquivoFotoGed;

    @Column(name = "id_arquivo_curriculo_ged")
    private Long idArquivoCurriculoGed;

    @Column(name = "resumo_curriculo", columnDefinition = "text")
    private String resumoCurriculo;
}
