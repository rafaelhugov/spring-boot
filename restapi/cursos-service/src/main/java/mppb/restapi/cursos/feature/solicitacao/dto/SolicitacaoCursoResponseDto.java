package mppb.restapi.cursos.feature.solicitacao.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class SolicitacaoCursoResponseDto {

    private Long id;

    // Status e Integração
    private String statusDescricao;
    private String numeroPgaMpVirtual;
    private Long idPgaMpVirtual;
    private UUID uuidReferencia;
    private Long idMinuta;

    /**
     * Payload enviado ao MPVirtual (apenas preenchido no momento do protocolo para
     * fins de log/exibição)
     */
    private Object mpVirtualPayload;

    // Dados base
    private String nomeCurso;
    private Integer estimativaParticipantes;
    private String nomeSolicitante;

    // IDs para re-popular selects (Modalidade, Localização e Tipo de Evento)
    private Long idModalidade;
    private String modalidadeDescricao;
    private Long idTipoEvento;
    private String tipoEventoDescricao;

    private Long idEntidadeTutora;
    private String entidadeTutoraNome;

    private List<Long> idsPublicoAlvo;
    private List<String> publicosAlvoDescricao;
    private String criteriosEspecificosPublicoAlvo;

    private Long idLocalEvento;
    private String localEventoDescricao;
    private String descricaoLocalExterno;

    private Boolean temOrgaoExterno;
    private String nomeOrgaoExterno;

    // Apoio Logístico
    private Boolean cardDivulgacao;
    private Boolean comunicacaoImprensa;
    private Boolean oficioConvite;
    private Boolean coffeeBreak;
    private Boolean kitParticipante;
    private Boolean programacaoImpressa;
    private Boolean trasladoMinistrante;
    private Boolean semApoioLogistico;

    // Financeiro
    private Boolean pagamentoHonorario;
    private Boolean passagemAerea;
    private Boolean hospedagem;
    private Boolean semOnusFinanceiro;

    // Documentação
    private String observacoesGerais;
    private String descricaoAjustes;
    private Long idArquivoProgramacaoGed;
    private Long idCursoGerado;

    // Listas
    private List<AgendaResponseDto> agendas;
    private List<MinistranteResponseDto> ministrantes;

    // Timestamps
    private LocalDateTime dataCriacao;
    private LocalDateTime dataModificacao;

    @Data
    public static class AgendaResponseDto {
        private Long id;
        private LocalDate dataInicio;
        private LocalDate dataFim;
        private LocalTime horarioInicio;
        private LocalTime horarioFim;
    }

    @Data
    public static class MinistranteResponseDto {
        private Long id;
        private Long idPapelMinistrante;
        private Long ministranteId;
        private Integer idServidor;
        private String nomeMinistrante;
        private String telefone;
        private String cpf;
        private String email;
        private Long idFoto;
        private Long idCurriculo;
    }
}
