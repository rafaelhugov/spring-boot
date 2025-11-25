package mppb.microservices.common.infrastructure.client.authapi.dto;

import java.io.Serializable;

public class UsuarioAutorizadoResponseDto implements Serializable {

    private static final long serialVersionUID = -4697286374958484031L;

    private Long id;

    private String nome;

    private String login;

    public UsuarioAutorizadoResponseDto() {
        super();
    }

    public UsuarioAutorizadoResponseDto(Long id, String nome, String login) {
        super();
        this.id = id;
        this.nome = nome;
        this.login = login;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    @Override
    public String toString() {
        return String.format("UsuarioAutorizadoResponseDto [id=%s, nome=%s, login=%s]", id, nome, login);
    }

}
