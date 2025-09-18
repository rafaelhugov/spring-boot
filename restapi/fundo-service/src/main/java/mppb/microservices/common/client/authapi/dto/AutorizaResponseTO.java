package mppb.microservices.common.client.authapi.dto;

public class AutorizaResponseTO extends AutenticaResponseTO {

    private static final long serialVersionUID = 7942556694690683194L;

    public AutorizaResponseTO() {
        super();
    }

    public AutorizaResponseTO(String token) {
        super(token);
    }

}