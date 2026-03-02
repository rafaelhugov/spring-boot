package mppb.restapi.cursos.feature.solicitacao.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class SolicitacaoCursoRequestDto {

    // Informações do Evento
    @NotBlank(message = "O nome do curso é obrigatório")
    private String nomeCurso;

    @NotNull(message = "A estimativa de participantes é obrigatória")
    @Min(value = 1, message = "A estimativa de participantes deve ser pelo menos 1")
    private Integer estimativaParticipantes;

    @NotNull(message = "O público-alvo é obrigatório")
    private Long idPublicoAlvo;

    private Boolean identificacaoOrgaoMembros;
    private String especificacaoOrgaoMembros;

    // Modalidade e Localização
    @NotNull(message = "A modalidade é obrigatória")
    private Long idModalidade;

    private Long idLocalEvento; // Pode ser nulo se for 100% online
    private String descricaoLocalExterno;

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

    @Data
    public static class AgendaRequestDto {
        @NotNull(message = "A data do evento é obrigatória na agenda")
        private LocalDate dataEvento;

        @NotNull(message = "O horário de início é obrigatório na agenda")
        private LocalTime horarioInicio;

        @NotNull(message = "O horário de término é obrigatório na agenda")
        private LocalTime horarioFim;
    }

    @Data
    public static class MinistranteRequestDto {
        @NotNull(message = "O papel do ministrante é obrigatório")
        private Long idPapelMinistrante;

        private Long idServidorInterno; // Se for servidor do MP

        @NotBlank(message = "O nome do ministrante é obrigatório")
        private String nomeMinistrante;

        private String contatoInfo;
        private Long idArquivoFotoGed; // Arquivo previamente enviado pro GED
        private Long idArquivoCurriculoGed; // Arquivo previamente enviado pro GED
        private String resumoCurriculo;
    }
}
