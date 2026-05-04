package mppb.restapi.common.infrastructure.client.mpvirtual.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MPVirtualMetadataDto {
    private Long idTipoDocumentoPecaInicial;
    private Long idTipoDocumento; // Usado em Minutas
    private Long codClasse;
    private Long codAssunto;
    private Long idAssunto; // Usado em Minutas
    private String resumo;
    private String instancia;
    private List<InteressadoDto> interessados;
    private Long idDesignacao;
    private String numeroAutomaticoDocumento;
    private Integer tipoDistribuicao;
    private Boolean assinado; // Usado em Minutas

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
