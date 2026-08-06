package br.gov.mppb.ncap.feature.inspecao.dto;

import br.gov.mppb.ncap.feature.inspecao.InspecaoStatus;
import br.gov.mppb.ncap.feature.inspecao.JustificativaRemota;
import br.gov.mppb.ncap.feature.inspecao.TipoVisita;
import jakarta.validation.constraints.*;
import lombok.*;
import mppb.restapi.common.core.validation.groups.Create;
import mppb.restapi.common.core.validation.groups.Update;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspecaoDto {

    @NotNull(groups = {Update.class})
    @Null(groups = {Create.class})
    private Long id;

    // ------------------------------------------------------------------
    // Entidade
    // ------------------------------------------------------------------

    @NotNull(message = "A entidade é obrigatória.")
    private Long entidadeId;

    private String entidadeNomecnmp;

    private String entidadeTipoentidade;

    private String entidadeMunicipio;

    private Long entidadeIdCnmp;

    private String entidadeComarca;

    private String formtemplateResolucao;

    // ------------------------------------------------------------------
    // Promotor responsável e status
    // ------------------------------------------------------------------

    @NotNull(message = "O promotor responsável é obrigatório.")
    private Long idPromotorresponsavel;

    private String promotorNome;

    private Long idServidorCriador;

    private Long idFormtemplate;

    private InspecaoStatus status;

    // ------------------------------------------------------------------
    // Datas
    // ------------------------------------------------------------------

    @NotNull(message = "A data de agendamento é obrigatória.")
    private LocalDate dataagendamento;

    private LocalDate datavisita;

    @NotBlank(message = "O semestre é obrigatório.")
    @Size(max = 20, message = "O semestre não pode exceder 20 caracteres.")
    private String semestre;

    // ------------------------------------------------------------------
    // PGA
    // ------------------------------------------------------------------

    @Size(max = 50, message = "O número do PGA não pode exceder 50 caracteres.")
    private String numeroPga;

    @Size(max = 500, message = "O link do PGA não pode exceder 500 caracteres.")
    private String linkPga;

    // ------------------------------------------------------------------
    // Tipo de visita e justificativa (Res. CNMP 279/2023)
    // ------------------------------------------------------------------

    private TipoVisita tipoVisita;

    private LocalDate dataLimiteResposta;

    /**
     * Obrigatório apenas quando {@code tipoVisita == VIRTUAL}.
     * Validado programaticamente na Service (RN01/FA01).
     */
    private JustificativaRemota justificativaRemota;

    @Size(max = 2000, message = "O detalhamento da justificativa não pode exceder 2000 caracteres.")
    private String justificativaDetalhe;

    // ------------------------------------------------------------------
    // Responsável pela entidade inspecionada
    // ------------------------------------------------------------------

    @Size(max = 200, message = "O nome do responsável não pode exceder 200 caracteres.")
    private String responsavelPrincipal;

    @Email(message = "O e-mail do responsável é inválido.")
    @Size(max = 200, message = "O e-mail não pode exceder 200 caracteres.")
    private String emailResponsavel;

    // ------------------------------------------------------------------
    // Equipe inspetora (IDs de servidores do RH)
    // ------------------------------------------------------------------

    @Builder.Default
    private List<Long> membros = new ArrayList<>();

    // ------------------------------------------------------------------
    // Token e auditoria (somente leitura — gerados pelo backend)
    // ------------------------------------------------------------------

    private String tokenacesso;

    private Instant tokenexpiracao;

    private Instant criadoem;

    private Instant atualizadoem;
}
