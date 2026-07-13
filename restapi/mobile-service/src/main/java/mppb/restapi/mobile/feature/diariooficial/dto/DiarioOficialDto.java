package mppb.restapi.mobile.feature.diariooficial.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class DiarioOficialDto {

    private Long id;
    private LocalDate dataPublicacao;
    private Long numeroEdicao;

    @JsonProperty("idArquivo")
    private Long idDocumentoAssinado;
}
