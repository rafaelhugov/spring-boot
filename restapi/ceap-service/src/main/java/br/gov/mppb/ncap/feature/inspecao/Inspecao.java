package br.gov.mppb.ncap.feature.inspecao;

import br.gov.mppb.ncap.feature.entidade.Entidade;
import jakarta.persistence.*;
import lombok.*;
import mppb.restapi.common.core.entity.AbstractEntity;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "inspecoes", schema = "ncap")
@AttributeOverride(name = "id", column = @Column(name = "id_inspecao"))
public class Inspecao extends AbstractEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_entidade", nullable = false)
    private Entidade entidade;

    @Column(name = "id_promotorresponsavel", nullable = false)
    private Long idPromotorresponsavel;

    @Column(name = "id_servidor_criador", nullable = false)
    private Long idServidorCriador;

    @Column(name = "id_formtemplate")
    private Long idFormtemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_formtemplate", insertable = false, updatable = false)
    private br.gov.mppb.ncap.feature.formtemplate.FormTemplate template;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private InspecaoStatus status;

    @Column(name = "dataagendamento", nullable = false)
    private LocalDate dataagendamento;

    @Column(name = "datavisita")
    private LocalDate datavisita;

    @Column(name = "semestre", nullable = false, length = 20)
    private String semestre;

    // ------------------------------------------------------------------
    // Campos do PGA (Procedimento / Plano de Gestão Anual)
    // ------------------------------------------------------------------

    @Column(name = "numero_pga", length = 50)
    private String numeroPga;

    @Column(name = "link_pga", length = 500)
    private String linkPga;

    // ------------------------------------------------------------------
    // Tipo de visita e justificativa (Res. CNMP 279/2023)
    // ------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_visita", length = 20)
    private TipoVisita tipoVisita;

    @Column(name = "data_limite_resposta")
    private LocalDate dataLimiteResposta;

    @Enumerated(EnumType.STRING)
    @Column(name = "justificativa_remota", length = 60)
    private JustificativaRemota justificativaRemota;

    @Column(name = "justificativa_detalhe", length = 2000)
    private String justificativaDetalhe;

    // ------------------------------------------------------------------
    // Responsável pela unidade inspecionada
    // ------------------------------------------------------------------

    @Column(name = "responsavel_principal", length = 200)
    private String responsavelPrincipal;

    @Column(name = "email_responsavel", length = 200)
    private String emailResponsavel;

    // ------------------------------------------------------------------
    // Equipe inspetora (IDs de servidores do RH — tabela associativa)
    // ------------------------------------------------------------------

    /**
     * IDs dos servidores do MPPB que compõem a equipe de inspeção.
     * Persistidos em tabela associativa {@code inspecao_membros}.
     * O ID referencia a tabela {@code public.servidor} do banco corporativo.
     */
    @ElementCollection
    @CollectionTable(name = "inspecao_membros", schema = "ncap", joinColumns = @JoinColumn(name = "id_inspecao"))
    @Column(name = "id_servidor", nullable = false)
    @Builder.Default
    private List<Long> membros = new ArrayList<>();

    // ------------------------------------------------------------------
    // Token de acesso para autoavaliação da unidade
    // ------------------------------------------------------------------

    @Column(name = "tokenacesso", unique = true)
    private String tokenacesso;

    @Column(name = "tokenexpiracao")
    private Instant tokenexpiracao;

    // ------------------------------------------------------------------
    // Auditoria
    // ------------------------------------------------------------------

    @CreationTimestamp
    @Column(name = "criadoem", nullable = false, updatable = false)
    private Instant criadoem;

    @UpdateTimestamp
    @Column(name = "atualizadoem", nullable = false)
    private Instant atualizadoem;
}
