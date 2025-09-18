package mppb.microservices.common.client.authapi.dto;


import java.io.Serializable;

public class AutenticadoResponseTO implements Serializable {

    private static final long serialVersionUID = 3796665478220965301L;

    private Boolean autenticado;

    public AutenticadoResponseTO() {
        super();
    }

    public AutenticadoResponseTO(Boolean autenticado) {
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
        return String.format("AutenticadoResponseTO [autenticado=%s]", autenticado);
    }

}
