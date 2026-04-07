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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "solicitacaocurso", schema = "curso")
@Getter
@Setter
public class SolicitacaoCurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "datacriacao", updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "datamodificacao")
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

    @Column(name = "numeropga", length = 25)
    private String numeroPgaMpVirtual;

    @Column(name = "id_pga")
    private Long idPgaMpVirtual;

    @Column(name = "uuid_referencia", nullable = false, updatable = false)
    private UUID uuidReferencia = UUID.randomUUID();

    // Identificação do Interessado
    // Assumption: we keep ID as bigint to link to public.servidor later if needed,
    // or we fetch details from JWT context for auditoria.
    @Column(name = "id_requerente", nullable = false)
    private Long idServidorInteressado;

    @Column(name = "id_orgao", nullable = false)
    private Long idOrgaoInteressado;

    @Column(name = "nomesolicitante")
    private String nomeSolicitante;

    // Informações do Evento
    @Column(name = "nomecurso", nullable = false, length = 255)
    private String nomeCurso;

    @Column(name = "estimativaparticipantes", nullable = false)
    private Integer estimativaParticipantes;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "solicitacaopublicoalvo", schema = "curso", joinColumns = @JoinColumn(name = "id_solicitacao"), inverseJoinColumns = @JoinColumn(name = "id_publicoalvo"))
    private List<PublicoAlvo> publicosAlvo = new ArrayList<>();

    @Column(name = "temorgaoexterno")
    private Boolean temOrgaoExterno = false;

    @Column(name = "nomeorgaoexterno", columnDefinition = "text")
    private String nomeOrgaoExterno;

    // Modalidade, Localização e Tipo de Evento
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_modalidade")
    private Modalidade modalidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipoevento")
    private TipoEvento tipoEvento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_entidadetutora")
    private EntidadeTutora entidadeTutora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_localevento")
    private LocalEvento localEvento;

    @Column(name = "descricaolocalexterno", columnDefinition = "text")
    private String descricaoLocalExterno;

    // Apoio Logístico
    @Column(name = "carddivulgacao")
    private Boolean cardDivulgacao = false;

    @Column(name = "comunicacaoimprensa")
    private Boolean comunicacaoImprensa = false;

    @Column(name = "oficioconvite")
    private Boolean oficioConvite = false;

    @Column(name = "coffeebreak")
    private Boolean coffeeBreak = false;

    @Column(name = "kitparticipante")
    private Boolean kitParticipante = false;

    @Column(name = "programacaoimpressa")
    private Boolean programacaoImpressa = false;

    @Column(name = "trasladoministrante")
    private Boolean trasladoMinistrante = false;

    @Column(name = "sem_apoio_logistico")
    private Boolean semApoioLogistico = false;

    // Financeiro (Ônus para o MPPB)
    @Column(name = "pagamentohonorario")
    private Boolean pagamentoHonorario = false;

    @Column(name = "passagemaerea")
    private Boolean passagemAerea = false;

    @Column(name = "hospedagem")
    private Boolean hospedagem = false;

    @Column(name = "sem_onus_financeiro")
    private Boolean semOnusFinanceiro = false;

    // Documentação e Notas
    @Column(name = "observacoes", columnDefinition = "text")
    private String observacoesGerais;

    @Column(name = "id_arquivo")
    private Long idArquivoProgramacaoGed;

    @OneToMany(mappedBy = "solicitacao", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SolicitacaoAgenda> agendas = new HashSet<>();

    @OneToMany(mappedBy = "solicitacao", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SolicitacaoMinistrante> ministrantes = new HashSet<>();

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

    @Column(name = "id_minuta")
    private Long idMinuta;

    @Column(name = "id_cursogerado")
    private Long idCursoGerado;
}
