package mppb.microservices.common.infrastructure.client.authapi.dto;

public class AutorizaResponseDto extends AutenticaResponseDto {

    private static final long serialVersionUID = 7942556694690683194L;

    public AutorizaResponseDto() {
        super();
    }

    public AutorizaResponseDto(String token) {
        super(token);
    }

}