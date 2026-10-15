package mppb.restapi.cursos.feature.curso.dto;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.validation.groups.ConvertGroup;
import jakarta.validation.groups.Default;
import lombok.*;
import mppb.restapi.common.core.validation.groups.Create;
import mppb.restapi.common.core.validation.groups.Update;
import mppb.restapi.cursos.feature.cadastro.dto.EntidadeTutoraDto;
import mppb.restapi.cursos.feature.lookup.dto.ModalidadeDto;
import mppb.restapi.cursos.feature.lookup.dto.PublicoAlvoDto;
import mppb.restapi.cursos.feature.lookup.dto.TipoEventoDto;
import mppb.restapi.cursos.feature.lookup.dto.MunicipioDto;

import java.time.LocalDateTime;
import java.util.List;

import mppb.restapi.cursos.feature.curso.enums.StatusCursoEnum;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class CursoDto {

    @JsonView(CursoView.Listagem.class)
    @NotNull(groups = {Update.class})
    @Null(groups = Create.class)
    private Long id;

    @JsonView(CursoView.Listagem.class)
    @NotBlank
    @Size(max = 255, message = "O título não pode exceder 255 caracteres.")
    private String titulo;

    private String descricao;

    @NotNull
    private TipoEventoDto tipoEvento;

    @NotNull(message = "A entidade tutora é obrigatória.")
    private EntidadeTutoraDto entidadeTutora;

    @JsonView(CursoView.Listagem.class)
    private LocalDateTime dataCriacao;

    @JsonView(CursoView.Listagem.class)
    private LocalDateTime inscricaoDataInicial;

    @JsonView(CursoView.Listagem.class)
    private LocalDateTime inscricaoDataFinal;

    // TODO: Adicionar validação para garantir que a data final seja após a data inicial.
    // Isso requer uma anotação de validação a nível de classe, que podemos implementar depois.
    @JsonView(CursoView.Listagem.class)
    private LocalDateTime realizacaoDataInicial;

    @JsonView(CursoView.Listagem.class)
    private LocalDateTime realizacaoDataFinal;

    @JsonView(CursoView.Listagem.class)
    @Pattern(regexp = "^([0-9]{1,4}):([0-5][0-9]):([0-5][0-9])$", message = "O formato da carga horária deve ser HHHH:MM:SS.")
    private String cargaHoraria;

    @Min(value = 0, message = "O percentual deve ser no mínimo 0.")
    @Max(value = 100, message = "O percentual deve ser no máximo 100.")
    private Integer percentualChAprovacao;

    private String detalhaCargaHoraria;

    private String linkProgramacao;

    @JsonView(CursoView.Listagem.class)
    @NotNull
    private ModalidadeDto modalidade;

    private String progressaoFuncional;

    @JsonView(CursoView.Listagem.class)
    private StatusCursoEnum status;

    @JsonView(CursoView.Listagem.class)
    private String hashInscricao;

    @JsonView(CursoView.Listagem.class)
    @NotEmpty(message = "Ao menos um público-alvo deve ser selecionado.")
    private List<PublicoAlvoDto> publicosAlvo;

    @Size(max = 4000, groups = {Default.class, Update.class}, message = "Os critérios específicos do público-alvo devem ter no máximo 4000 caracteres.")
    private String criteriosEspecificosPublicoAlvo;

    private List<CursoMinistranteDto> ministrantes;

    private Long certificateTemplateId;

    private Long headerImageId;

    @JsonView(CursoView.Listagem.class)
    private MunicipioDto municipio;
}
