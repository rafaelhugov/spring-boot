package mppb.restapi.mobile.feature.captcha.dto;

public class CaptchaResponseDTO {
    private String token;
    private String imageBase64;

    public CaptchaResponseDTO(String token, String imageBase64) {
        this.token = token;
        this.imageBase64 = imageBase64;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
}
