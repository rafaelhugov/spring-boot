package mppb.restapi.cursos.feature.curso.dto;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import mppb.restapi.cursos.feature.cadastro.dto.EntidadeTutoraDto;
import mppb.restapi.cursos.feature.lookup.dto.ModalidadeDto;
import mppb.restapi.cursos.feature.lookup.dto.PublicoAlvoDto;
import mppb.restapi.cursos.feature.lookup.dto.TipoEventoDto;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class CursoDto {

    @JsonView(CursoView.Listagem.class)
    private Long id;

    @JsonView(CursoView.Listagem.class)
    private String titulo;

    private String descricao;

    private TipoEventoDto tipoEvento;

    private EntidadeTutoraDto entidadeTutora;

    private LocalDateTime dataCriacao;

    private LocalDateTime inscricaoDataInicial;

    private LocalDateTime inscricaoDataFinal;

    private LocalDateTime realizacaoDataInicial;

    private LocalDateTime realizacaoDataFinal;

    private String cargaHoraria;

    private Integer percentualChAprovacao;

    private String detalhaCargaHoraria;

    private String linkProgramacao;

    @JsonView(CursoView.Listagem.class)
    private ModalidadeDto modalidade;

    private String progressaoFuncional;

    private String hashInscricao;

    private Boolean gerarLink;

    @JsonView(CursoView.Listagem.class)
    private List<PublicoAlvoDto> publicosAlvo ;

}
