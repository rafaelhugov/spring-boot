package mppb.restapi.common.infrastructure.client.mpvirtual.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MPVirtualProcessoRequestDto {
    private String titulo;
    private String descricao;
    private String codigoAssunto;
    private Long idInteressado;
    private byte[] documentoPdf; // The generated course request PDF
    private String nomeArquivo;
    private LocalDateTime dataSolicitacao;
}
