package mppb.restapi.cursos.feature.solicitacao.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class SolicitacaoCursoResponseDto {

    private Long id;

    // Status e Integração
    private String statusDescricao; // Da tabela status_tramitacao
    private String numeroPgaMpVirtual;
    private UUID uuidReferencia;

    // Dados base
    private String nomeCurso;
    private Integer estimativaParticipantes;

    private String modalidadeDescricao;
    private String publicoAlvoDescricao;

    private LocalDateTime dataCriacao;
    private LocalDateTime dataModificacao;
}
