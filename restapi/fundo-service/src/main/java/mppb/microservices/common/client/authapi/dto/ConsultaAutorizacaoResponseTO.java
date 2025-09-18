package mppb.microservices.common.client.authapi.dto;


import java.io.Serializable;

public class ConsultaAutorizacaoResponseTO implements Serializable {

    private static final long serialVersionUID = -4773473270585232291L;

    private AcessoDataResponseTO acessoData;

    private UsuarioAutorizadoResponseTO usuario;

    public ConsultaAutorizacaoResponseTO() {
        super();
    }

    public ConsultaAutorizacaoResponseTO(AcessoDataResponseTO acessoData, UsuarioAutorizadoResponseTO usuario) {
        super();
        this.acessoData = acessoData;
        this.usuario = usuario;
    }

    public AcessoDataResponseTO getAcessoData() {
        return acessoData;
    }

    public void setAcessoData(AcessoDataResponseTO acessoData) {
        this.acessoData = acessoData;
    }

    public UsuarioAutorizadoResponseTO getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioAutorizadoResponseTO usuario) {
        this.usuario = usuario;
    }

    @Override
    public String toString() {
        return String.format("ConsultaAutorizacaoResponseTO [acessoData=%s, usuario=%s]", acessoData, usuario);
    }

}
