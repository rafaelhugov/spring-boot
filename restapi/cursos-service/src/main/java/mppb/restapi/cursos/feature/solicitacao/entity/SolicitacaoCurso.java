package mppb.restapi.cursos.feature.solicitacao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mppb.restapi.cursos.feature.lookup.entities.LocalEvento;
import mppb.restapi.cursos.feature.lookup.entities.Modalidade;
import mppb.restapi.cursos.feature.lookup.entities.PublicoAlvo;
import mppb.restapi.cursos.feature.lookup.entities.StatusTramitacao;
import mppb.restapi.cursos.feature.lookup.entities.TipoEvento;
import mppb.restapi.cursos.feature.cadastro.entities.EntidadeTutora;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "solicitacaocurso", schema = "curso")
@Getter
@Setter
public class SolicitacaoCurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_criacao", updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_modificacao")
    private LocalDateTime dataModificacao;

    @PrePersist
    protected void onCreate() {
        this.dataCriacao = LocalDateTime.now();
        this.dataModificacao = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.dataModificacao = LocalDateTime.now();
    }

    // Controle de Processo (Integração MPVirtual)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_status")
    private StatusTramitacao status;

    @Column(name = "numero_pga_mpvirtual", length = 25)
    private String numeroPgaMpVirtual;

    @Column(name = "uuid_referencia", nullable = false, updatable = false)
    private UUID uuidReferencia = UUID.randomUUID();

    // Identificação do Interessado
    // Assumption: we keep ID as bigint to link to public.servidor later if needed,
    // or we fetch details from JWT context for auditoria.
    @Column(name = "id_servidor_interessado", nullable = false)
    private Long idServidorInteressado;

    @Column(name = "id_orgao_interessado", nullable = false)
    private Long idOrgaoInteressado;

    // Informações do Evento
    @Column(name = "nome_curso", nullable = false, length = 255)
    private String nomeCurso;

    @Column(name = "estimativa_participantes", nullable = false)
    private Integer estimativaParticipantes;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "solicitacao_publico_alvo", schema = "curso", joinColumns = @JoinColumn(name = "id_solicitacao"), inverseJoinColumns = @JoinColumn(name = "id_publico_alvo"))
    private List<PublicoAlvo> publicosAlvo = new ArrayList<>();

    @Column(name = "identificacao_orgao_membros")
    private Boolean identificacaoOrgaoMembros = false;

    @Column(name = "especificacao_orgao_membros", columnDefinition = "text")
    private String especificacaoOrgaoMembros;

    // Modalidade, Localização e Tipo de Evento
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_modalidade")
    private Modalidade modalidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_evento")
    private TipoEvento tipoEvento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_entidade_tutora")
    private EntidadeTutora entidadeTutora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_local_evento")
    private LocalEvento localEvento;

    @Column(name = "descricao_local_externo", columnDefinition = "text")
    private String descricaoLocalExterno;

    // Apoio Logístico
    @Column(name = "card_divulgacao")
    private Boolean cardDivulgacao = false;

    @Column(name = "comunicacao_imprensa")
    private Boolean comunicacaoImprensa = false;

    @Column(name = "oficio_convite")
    private Boolean oficioConvite = false;

    @Column(name = "coffee_break")
    private Boolean coffeeBreak = false;

    @Column(name = "kit_participante")
    private Boolean kitParticipante = false;

    @Column(name = "programacao_impressa")
    private Boolean programacaoImpressa = false;

    @Column(name = "traslado_ministrante")
    private Boolean trasladoMinistrante = false;

    // Financeiro (Ônus para o MPPB)
    @Column(name = "pagamento_honorario")
    private Boolean pagamentoHonorario = false;

    @Column(name = "passagem_aerea")
    private Boolean passagemAerea = false;

    @Column(name = "hospedagem")
    private Boolean hospedagem = false;

    // Documentação e Notas
    @Column(name = "observacoes_gerais", columnDefinition = "text")
    private String observacoesGerais;

    @Column(name = "id_arquivo_programacao_ged")
    private Long idArquivoProgramacaoGed;

    @OneToMany(mappedBy = "solicitacao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SolicitacaoAgenda> agendas = new ArrayList<>();

    @OneToMany(mappedBy = "solicitacao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SolicitacaoMinistrante> ministrantes = new ArrayList<>();

    public void addAgenda(SolicitacaoAgenda agenda) {
        agendas.add(agenda);
        agenda.setSolicitacao(this);
    }

    public void removeAgenda(SolicitacaoAgenda agenda) {
        agendas.remove(agenda);
        agenda.setSolicitacao(null);
    }

    public void addMinistrante(SolicitacaoMinistrante ministrante) {
        ministrantes.add(ministrante);
        ministrante.setSolicitacao(this);
    }

    public void removeMinistrante(SolicitacaoMinistrante ministrante) {
        ministrantes.remove(ministrante);
        ministrante.setSolicitacao(null);
    }

    @Column(name = "id_curso_gerado")
    private Long idCursoGerado;
}
