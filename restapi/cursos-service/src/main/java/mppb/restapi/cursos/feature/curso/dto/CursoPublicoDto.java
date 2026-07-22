package mppb.restapi.cursos.feature.curso.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para expor publicamente os detalhes de um curso para a página de inscrição.
 * Contém apenas os campos necessários e seguros para visualização externa.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class CursoPublicoDto {

    private String hashInscricao;
    private String titulo;
    private String descricao;
    private LocalDateTime inscricaoDataInicial;
    private LocalDateTime inscricaoDataFinal;
    private LocalDateTime realizacaoDataInicial;
    private LocalDateTime realizacaoDataFinal;
    private String cargaHoraria;
    private String linkProgramacao;
    private String modalidade;
    private String municipio;
    private List<String> publicosAlvo;

    /**
     * Campo que informa ao frontend se o curso está aberto a inscrições de internos.
     */
    private boolean aceitaInscricaoInterna;

    /**
     * Campo que informa ao frontend se o curso está aberto a inscrições de externos.
     */
    private boolean aceitaInscricaoExterna;
}
