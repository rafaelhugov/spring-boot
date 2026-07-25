package mppb.restapi.common.infrastructure.client.authapi.dto;


import java.io.Serializable;

public class AutenticaResponseDto implements Serializable {

    private static final long serialVersionUID = -3492818668146750093L;

    private String token;

    public AutenticaResponseDto() {
        super();
    }

    public AutenticaResponseDto(String token) {
        super();
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "AutenticaResponseDto [response=***]";
    }

}
