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
    private String naturezaAto;

    public DesignacaoAtivaItemResponse(
            Long id,
            String tipoCodigo,
            String tipoDescricao,
            String titulo,
            String subtitulo,
            String orgao,
            String funcao,
            LocalDate dataInicial,
            LocalDate dataFinal,
            String ato,
            LocalDate dataPublicacao) {
        this(id, tipoCodigo, tipoDescricao, titulo, subtitulo, orgao, funcao,
                dataInicial, dataFinal, ato, dataPublicacao, null);
    }
}
