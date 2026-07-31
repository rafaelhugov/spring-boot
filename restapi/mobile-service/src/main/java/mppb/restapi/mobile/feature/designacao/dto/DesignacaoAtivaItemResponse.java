package mppb.restapi.mobile.feature.designacao.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DesignacaoAtivaItemResponse {
    private Long id;
    private String tipoCodigo;
    private String tipoDescricao;
    private String titulo;
    private String subtitulo;
    private String orgao;
    private String funcao;
    private LocalDate dataInicial;
    private LocalDate dataFinal;
    private String ato;
    private LocalDate dataPublicacao;
}
