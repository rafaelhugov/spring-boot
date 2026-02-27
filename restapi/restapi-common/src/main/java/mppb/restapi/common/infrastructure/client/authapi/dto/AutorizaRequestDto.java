package mppb.restapi.common.infrastructure.client.authapi.dto;

import jakarta.validation.constraints.NotNull;

public class AutorizaRequestDto extends AutenticaRequestDto {

    private static final long serialVersionUID = 1152810370436818280L;

    @NotNull
    private String siglaSistema;

    public AutorizaRequestDto(@NotNull String login, @NotNull String senha, @NotNull String siglaSistema) {
        super(login, senha);
        this.siglaSistema = siglaSistema;
    }

    public String getSiglaSistema() {
        return siglaSistema;
    }

    public void setSiglaSistema(String siglaSistema) {
        this.siglaSistema = siglaSistema;
    }

    @Override
    public String toString() {
        return String.format("AutorizaRequestDto [siglaSistema=%s]", siglaSistema);
    }

}
