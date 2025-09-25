package mppb.restapi.common.infrastructure.client.authapi.dto;


import java.io.Serializable;

public class AutenticadoResponseDto implements Serializable {

    private static final long serialVersionUID = 3796665478220965301L;

    private Boolean autenticado;

    public AutenticadoResponseDto() {
        super();
    }

    public AutenticadoResponseDto(Boolean autenticado) {
        super();
        this.autenticado = autenticado;
    }

    public Boolean getAutenticado() {
        return autenticado;
    }

    public void setAutenticado(Boolean autenticado) {
        this.autenticado = autenticado;
    }

    @Override
    public String toString() {
        return String.format("AutenticadoResponseDto [autenticado=%s]", autenticado);
    }

}
