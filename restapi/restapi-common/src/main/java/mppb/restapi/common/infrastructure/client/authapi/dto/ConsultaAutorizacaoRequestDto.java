package mppb.restapi.common.infrastructure.client.authapi.dto;


import java.io.Serializable;

import jakarta.validation.constraints.NotNull;

public class ConsultaAutorizacaoRequestDto implements Serializable {

    private static final long serialVersionUID = -85264779039141468L;

    @NotNull
    private String token;

    private String siglaSistema;

    public ConsultaAutorizacaoRequestDto() {
        super();
    }

    public ConsultaAutorizacaoRequestDto(@NotNull String token, String siglaSistema) {
        super();
        this.token = token;
        this.siglaSistema = siglaSistema;
    }

    public ConsultaAutorizacaoRequestDto(@NotNull String token) {
        super();
        this.token = token;
        this.siglaSistema = "";
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getSiglaSistema() {
        return siglaSistema;
    }

    public void setSiglaSistema(String siglaSistema) {
        this.siglaSistema = siglaSistema;
    }

    @Override
    public String toString() {
        return String.format("ConsultaAutorizacaoRequestDto [response=%s, siglaSistema=%s]", token, siglaSistema);
    }

}
