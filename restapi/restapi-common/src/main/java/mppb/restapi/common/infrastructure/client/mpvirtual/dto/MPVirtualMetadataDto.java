package mppb.restapi.common.infrastructure.client.mpvirtual.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MPVirtualMetadataDto {
    private Long idTipoDocumentoPecaInicial;
    private Long codClasse;
    private Long codAssunto;
    private String resumo;
    private String instancia;
    private List<InteressadoDto> interessados;
    private Long idDesignacao;
    private String numeroAutomaticoDocumento;
    private Integer tipoDistribuicao;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InteressadoDto {
        private Integer idTipoInteressado;
        private Integer idPessoa;
        private Boolean sigiloso;
    }
}
