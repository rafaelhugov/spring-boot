package mppb.restapi.cursos.feature.carteirafuncional.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class ServidorResponseTO implements Serializable {

    private static final long serialVersionUID = 5406120479384778903L;

    private Long id;

    @JsonProperty("nome")
    private String pessoaFisicaNome;

    private String matricula;

    private String ativo;

    private String login;

    @JsonProperty("cargo")
    private String cargoNome;
}
