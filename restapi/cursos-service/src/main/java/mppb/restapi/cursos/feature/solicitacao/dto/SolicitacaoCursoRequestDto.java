package mppb.restapi.cursos.feature.solicitacao.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SolicitacaoCursoRequestDto {

    // Informações do Evento
    @NotBlank(message = "O nome do curso é obrigatório")
    private String nomeCurso;

    @NotNull(message = "A estimativa de participantes é obrigatória")
    @Min(value = 1, message = "A estimativa de participantes deve ser pelo menos 1")
    private Integer estimativaParticipantes;

    @NotNull(message = "O público-alvo é obrigatório")
    private List<Long> idsPublicoAlvo;

    private Boolean identificacaoOrgaoMembros;
    private String especificacaoOrgaoMembros;

    // Modalidade e Localização
    // Modalidade, Localização e Tipo de Evento
    @NotNull(message = "A modalidade é obrigatória")
    private Long idModalidade;

    @NotNull(message = "O tipo do evento é obrigatório")
    private Long idTipoEvento;

    private Long idLocalEvento; // Pode ser nulo se for 100% online
    private String descricaoLocalExterno;

    private Long idEntidadeTutora;

    // Apoio Logístico
    private Boolean cardDivulgacao = false;
    private Boolean comunicacaoImprensa = false;
    private Boolean oficioConvite = false;
    private Boolean coffeeBreak = false;
    private Boolean kitParticipante = false;
    private Boolean programacaoImpressa = false;
    private Boolean trasladoMinistrante = false;

    // Financeiro
    private Boolean pagamentoHonorario = false;
    private Boolean passagemAerea = false;
    private Boolean hospedagem = false;

    // Documentação e Notas
    private String observacoesGerais;
    private Long idArquivoProgramacaoGed; // Arquivo previamente enviado pro GED

    // Listas aninhadas
    @Valid
    private List<AgendaRequestDto> agendas;

    @Valid
    private List<MinistranteRequestDto> ministrantes;

    private String status; // RASCUNHO | SUBMETIDO

    @Data
    public static class AgendaRequestDto {
        @NotNull(message = "A data de início do período é obrigatória")
        private LocalDate dataInicio;

        @NotNull(message = "A data de término do período é obrigatória")
        private LocalDate dataFim;

        @NotNull(message = "O horário de início é obrigatório na agenda")
        private LocalTime horarioInicio;

        @NotNull(message = "O horário de término é obrigatório na agenda")
        private LocalTime horarioFim;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MinistranteRequestDto {
        @NotNull(message = "O papel do ministrante é obrigatório")
        private Long idPapelMinistrante;

        private Long ministranteId; // Se já tiver registro em curso.ministrante

        // Front-end pode enviar como idServidor ou idServidorInterno
        @JsonAlias("idServidorInterno")
        private Integer idServidor; // Se for servidor do MP

        @NotBlank(message = "O nome do ministrante é obrigatório")
        private String nomeMinistrante;

        private String telefone;
        private String cpf;
        private String email;
        private Long idFoto;
        private Long idCurriculo;
    }
}
