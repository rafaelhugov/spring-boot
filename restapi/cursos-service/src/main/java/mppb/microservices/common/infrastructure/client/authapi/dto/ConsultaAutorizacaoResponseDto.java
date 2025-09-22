package mppb.microservices.common.infrastructure.client.authapi.dto;


import java.io.Serializable;

public class ConsultaAutorizacaoResponseDto implements Serializable {

    private static final long serialVersionUID = -4773473270585232291L;

    private AcessoDataResponseDto acessoData;

    private UsuarioAutorizadoResponseDto usuario;

    public ConsultaAutorizacaoResponseDto() {
        super();
    }

    public ConsultaAutorizacaoResponseDto(AcessoDataResponseDto acessoData, UsuarioAutorizadoResponseDto usuario) {
        super();
        this.acessoData = acessoData;
        this.usuario = usuario;
    }

    public AcessoDataResponseDto getAcessoData() {
        return acessoData;
    }

    public void setAcessoData(AcessoDataResponseDto acessoData) {
        this.acessoData = acessoData;
    }

    public UsuarioAutorizadoResponseDto getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioAutorizadoResponseDto usuario) {
        this.usuario = usuario;
    }

    @Override
    public String toString() {
        return String.format("ConsultaAutorizacaoResponseDto [acessoData=%s, usuario=%s]", acessoData, usuario);
    }

}
