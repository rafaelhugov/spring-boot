package mppb.microservices.common.client.authapi.dto;


import java.io.Serializable;

public class AutenticaResponseTO implements Serializable {

    private static final long serialVersionUID = -3492818668146750093L;

    private String token;

    public AutenticaResponseTO() {
        super();
    }

    public AutenticaResponseTO(String token) {
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
        return String.format("AutenticaResponseTO [response=%s]", token);
    }

}
