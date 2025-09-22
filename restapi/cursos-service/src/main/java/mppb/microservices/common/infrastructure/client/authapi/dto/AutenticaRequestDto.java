package mppb.microservices.common.infrastructure.client.authapi.dto;


import java.io.Serializable;

import jakarta.validation.constraints.NotNull;

public class AutenticaRequestDto implements Serializable {

    private static final long serialVersionUID = -8747145710404783187L;

    @NotNull
    private String login;

    @NotNull
    private String senha;

    public AutenticaRequestDto() {
        super();
    }

    public AutenticaRequestDto(@NotNull String login, @NotNull String senha) {
        super();
        this.login = login;
        this.senha = senha;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    @Override
    public String toString() {
        return String.format("AutenticaRequestDto [login=%s, senha=%s]", login, senha);
    }

}
