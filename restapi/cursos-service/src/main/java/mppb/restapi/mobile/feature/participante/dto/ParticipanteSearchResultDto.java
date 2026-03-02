package mppb.restapi.mobile.feature.participante.dto;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipanteSearchResultDto {

    @JsonView(ParticipanteView.Listagem.class)
    private Long participanteId;

    @JsonView(ParticipanteView.Listagem.class)
    private Integer idServidor;

    @JsonView(ParticipanteView.Listagem.class)
    private String nome;

    @JsonView(ParticipanteView.Listagem.class)
    private String tipo;

    @JsonView(ParticipanteView.Listagem.class)
    private String cpf;

    @JsonView(ParticipanteView.Listagem.class)
    private String email;

    @JsonView(ParticipanteView.Listagem.class)
    private String telefone;

    @JsonView(ParticipanteView.Listagem.class)
    private String cargo;

    @JsonView(ParticipanteView.Listagem.class)
    private String servidorMatricula;

    @JsonView(ParticipanteView.Listagem.class)
    private String situacao;
}

