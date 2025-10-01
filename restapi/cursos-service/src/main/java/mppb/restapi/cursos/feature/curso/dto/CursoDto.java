package mppb.restapi.cursos.feature.curso.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.validation.groups.ConvertGroup;
import jakarta.validation.groups.Default;
import lombok.*;
import mppb.restapi.common.core.validation.*;
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
    @NotBlank
    @Size(max = 255, message = "O título não pode exceder 255 caracteres.")
    private String titulo;

    private String descricao;

    @NotNull
    private TipoEventoDto tipoEvento;

    @Valid
    @ConvertGroup.List({@ConvertGroup(from = Creation.class, to = Update.class), @ConvertGroup(to = Update.class)})
    private EntidadeTutoraDto entidadeTutora;

    private LocalDateTime dataCriacao;

    private LocalDateTime inscricaoDataInicial;

    private LocalDateTime inscricaoDataFinal;

    // TODO: Adicionar validação para garantir que a data final seja após a data inicial.
    // Isso requer uma anotação de validação a nível de classe, que podemos implementar depois.


    private LocalDateTime realizacaoDataInicial;

    private LocalDateTime realizacaoDataFinal;

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

    private String hashInscricao;

    @JsonView(CursoView.Listagem.class)
    @NotEmpty(message = "Ao menos um público-alvo deve ser selecionado.")
    private List<PublicoAlvoDto> publicosAlvo ;

    // TODO remover ministrantes daqui
    @JsonView(CursoView.Listagem.class)
    private List<Object> ministrantes ;

    /*
    @AssertTrue(message = "A entidade tutora selecionada é inválida (não contém um ID).")
    @JsonIgnore // Ignora este método na serialização/desserialização do JSON
    private boolean isEntidadeTutora() {
        // Se o objeto for nulo, a anotação @NotNull no campo já vai tratar disso.
        // Este método só precisa de validar o caso em que o objeto existe, mas o seu ID é nulo.
        if (entidadeTutora == null) {
            return true; // Deixa o @NotNull fazer o seu trabalho
        }
        return entidadeTutora.getId() != null;
    }
    */

}
